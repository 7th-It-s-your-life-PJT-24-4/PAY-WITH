<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { BrowserQRCodeReader, type IScannerControls } from '@zxing/browser'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { computed, onBeforeUnmount, onMounted, ref, useTemplateRef } from 'vue'

import { getApiErrorCode, getApiErrorMessage } from '@/api/error'
import { getMerchants } from '@/api/merchants'
import { executePayment } from '@/api/payments'
import type { ExecutedPayment } from '@/schemas/payment.schema'

/** 피보호자 QR 은 토큰 문자열을 그대로 굽는다 — 이 접두사로 다른 QR 을 걸러낸다 */
const QR_TOKEN_PREFIX = 'pay_qr_'
const FDS_BLOCKED_CODE = 'PAYMENT_006'
const INVALID_TOKEN_CODE = 'PAY_001'

type CameraState = 'idle' | 'requesting' | 'scanning' | 'denied' | 'scanned'
type PaymentOutcome = {
  kind: 'success' | 'blocked' | 'expired' | 'failed'
  message: string
  code: string | null
  payment: ExecutedPayment | null
}

const videoRef = useTemplateRef<HTMLVideoElement>('video')
const cameraState = ref<CameraState>('idle')
const cameraMessage = ref('')
const qrToken = ref('')
const merchantId = ref('')
const amount = ref('')
const outcome = ref<PaymentOutcome | null>(null)

let scannerControls: IScannerControls | null = null
/** 언마운트 뒤에 도착한 카메라 초기화 결과를 버리기 위한 플래그 */
let isActive = true

const merchantsQuery = useQuery({
  queryKey: ['scanner', 'merchants'],
  queryFn: getMerchants,
  retry: false,
})
const paymentMutation = useMutation({ mutationFn: executePayment })

/** 목록 조회가 실패하거나 비어 있으면 가맹점 번호 직접 입력으로 떨어진다 */
const useManualMerchantInput = computed(
  () => merchantsQuery.isError.value || merchantsQuery.data.value?.length === 0,
)
const parsedMerchantId = computed(() => Number(merchantId.value))
const parsedAmount = computed(() => Number(amount.value))
const canExecutePayment = computed(
  () =>
    qrToken.value.startsWith(QR_TOKEN_PREFIX) &&
    Number.isInteger(parsedMerchantId.value) &&
    parsedMerchantId.value > 0 &&
    Number.isInteger(parsedAmount.value) &&
    parsedAmount.value > 0 &&
    !paymentMutation.isPending.value,
)

/** 디코드 콜백이 비동기로 바꾸는 값이라, 인라인 비교 대신 함수로 읽는다 */
function hasScanned() {
  return cameraState.value === 'scanned'
}

function stopCamera() {
  scannerControls?.stop()
  scannerControls = null

  // controls.stop() 이 트랙까지 정리하지만, 초기화 중 실패한 스트림은 남을 수 있어 직접 확인한다
  const stream = videoRef.value?.srcObject
  if (stream instanceof MediaStream) {
    stream.getTracks().forEach((track) => track.stop())
    videoRef.value!.srcObject = null
  }
}

async function startCamera() {
  if (cameraState.value === 'requesting' || cameraState.value === 'scanning')
    return

  stopCamera()
  cameraState.value = 'requesting'
  cameraMessage.value = ''

  try {
    const reader = new BrowserQRCodeReader()
    const controls = await reader.decodeFromConstraints(
      { video: { facingMode: { ideal: 'environment' } } },
      videoRef.value!,
      (result) => {
        // 프레임마다 호출된다 — 인식 실패(error)는 정상 흐름이라 무시한다
        if (!result || hasScanned()) return
        handleScan(result.getText())
      },
    )
    scannerControls = controls

    // 이 await 이 끝나기 전에 (a) 디코드 콜백이 이미 인식했거나 (b) 페이지를 벗어났을 수 있다.
    // 두 경우 모두 handleScan/onBeforeUnmount 의 stopCamera 가 아직 없던 controls 를 놓치므로,
    // 여기서 정리한다. 'scanned' 상태도 덮어쓰지 않는다.
    if (!isActive || hasScanned()) {
      stopCamera()
      return
    }
    cameraState.value = 'scanning'
  } catch {
    cameraState.value = 'denied'
    cameraMessage.value =
      '카메라를 열지 못했습니다. 브라우저 카메라 권한을 확인해 주세요.'
  }
}

/** 인식 성공 즉시 카메라를 멈춰 같은 QR 이 반복 처리되지 않게 한다 */
function handleScan(text: string) {
  cameraState.value = 'scanned'
  stopCamera()

  if (!text.startsWith(QR_TOKEN_PREFIX)) {
    qrToken.value = ''
    cameraMessage.value = 'PAY-WITH 결제 QR 이 아닙니다. 다시 스캔해 주세요.'
    return
  }

  qrToken.value = text
  cameraMessage.value = ''
  outcome.value = null
}

async function submitPayment() {
  if (!canExecutePayment.value) return
  outcome.value = null

  try {
    const payment = await paymentMutation.mutateAsync({
      qrToken: qrToken.value,
      merchantId: parsedMerchantId.value,
      amount: parsedAmount.value,
    })
    outcome.value = {
      kind: 'success',
      message: '결제가 완료되었습니다.',
      code: null,
      payment,
    }
  } catch (error) {
    const code = getApiErrorCode(error)
    const message = await getApiErrorMessage(
      error,
      '결제에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    )
    outcome.value = {
      kind:
        code === FDS_BLOCKED_CODE
          ? 'blocked'
          : code === INVALID_TOKEN_CODE
            ? 'expired'
            : 'failed',
      message,
      code,
      payment: null,
    }
  } finally {
    // 성공·실패 어느 쪽이든 토큰은 1회용이라 폐기한다
    qrToken.value = ''
  }
}

function rescan() {
  outcome.value = null
  qrToken.value = ''
  void startCamera()
}

const statusText = computed(() => {
  if (paymentMutation.isPending.value) return '결제 요청 중…'
  if (cameraState.value === 'requesting') return '카메라 권한 요청 중…'
  if (cameraState.value === 'scanning') return 'QR 스캔 중…'
  if (cameraState.value === 'denied') return '카메라 권한 거부됨'
  if (qrToken.value) return 'QR 인식 완료'
  if (cameraState.value === 'scanned')
    return '카메라 정지됨 — 다시 스캔을 눌러주세요'
  return ''
})

const outcomeToneClass = computed(() => {
  if (!outcome.value) return ''
  return outcome.value.kind === 'success'
    ? 'border-success text-success'
    : 'border-error text-error'
})

onMounted(startCamera)
onBeforeUnmount(() => {
  isActive = false
  stopCamera()
})
</script>

<template>
  <!-- max-w-md 는 디자인 토큰 --spacing-md(16px) 와 충돌해 쓸 수 없다 -->
  <main class="mx-auto flex max-w-[420px] flex-col gap-lg p-mobile-gutter">
    <header>
      <h1 class="type-h2">가맹점 결제 스캐너</h1>
      <p class="type-body-medium mt-xxs text-body-secondary">
        PAY-WITH 결제 QR 을 스캔한 뒤 금액을 입력해 주세요.
      </p>
    </header>

    <section
      class="relative aspect-square overflow-hidden rounded-large bg-gray-100"
    >
      <video
        ref="video"
        class="size-full object-cover"
        muted
        playsinline
      ></video>
      <p
        v-if="statusText"
        class="type-caption absolute inset-x-0 bottom-0 bg-overlay/60 p-xs text-center text-white"
      >
        {{ statusText }}
      </p>
    </section>

    <p v-if="cameraMessage" class="type-body-medium text-error">
      {{ cameraMessage }}
    </p>

    <div class="flex flex-col gap-xs">
      <label class="type-h4" for="qr-token">QR Token</label>
      <input
        id="qr-token"
        class="rounded-medium border border-border bg-gray-900 p-sm text-body-secondary"
        :value="qrToken"
        placeholder="QR 을 스캔하면 자동으로 입력됩니다"
        readonly
      />
    </div>

    <div class="flex flex-col gap-xs">
      <label class="type-h4" for="merchant-id">가맹점</label>
      <select
        v-if="!useManualMerchantInput"
        id="merchant-id"
        v-model="merchantId"
        class="rounded-medium border border-border bg-surface-card p-sm"
      >
        <option value="">가맹점을 선택해 주세요</option>
        <option
          v-for="merchant in merchantsQuery.data.value ?? []"
          :key="merchant.merchantId"
          :value="String(merchant.merchantId)"
        >
          {{ merchant.name
          }}{{ merchant.region ? ` (${merchant.region})` : '' }}
        </option>
      </select>
      <template v-else>
        <input
          id="merchant-id"
          v-model="merchantId"
          class="rounded-medium border border-border bg-surface-card p-sm"
          type="number"
          inputmode="numeric"
          min="1"
          placeholder="가맹점 번호"
        />
        <p class="type-caption text-body-muted">
          가맹점 목록을 불러오지 못해 번호를 직접 입력합니다.
        </p>
      </template>
    </div>

    <div class="flex flex-col gap-xs">
      <label class="type-h4" for="amount">결제 금액</label>
      <input
        id="amount"
        v-model="amount"
        class="rounded-medium border border-border bg-surface-card p-sm"
        type="number"
        inputmode="numeric"
        min="1"
        placeholder="원"
      />
    </div>

    <div class="flex flex-col gap-xs">
      <Button
        class="w-full"
        :label="paymentMutation.isPending.value ? '결제 요청 중…' : '결제 실행'"
        :disabled="!canExecutePayment"
        @click="submitPayment"
      />
      <Button
        class="w-full"
        label="다시 스캔"
        variant="outline-primary"
        :disabled="paymentMutation.isPending.value"
        @click="rescan"
      />
    </div>

    <section
      v-if="outcome"
      class="flex flex-col gap-xxs rounded-large border bg-surface-card p-md"
      :class="outcomeToneClass"
    >
      <p class="type-h3">
        {{
          {
            success: '결제 성공',
            blocked: 'FDS 차단',
            expired: 'QR 만료 또는 무효',
            failed: '결제 실패',
          }[outcome.kind]
        }}
      </p>
      <p class="type-body-medium text-body">{{ outcome.message }}</p>
      <p v-if="outcome.code" class="type-caption text-body-muted">
        오류 코드: {{ outcome.code }}
      </p>
      <dl v-if="outcome.payment" class="type-body-medium mt-xs text-body">
        <div class="flex justify-between">
          <dt>거래 번호</dt>
          <dd>{{ outcome.payment.transactionId }}</dd>
        </div>
        <div class="flex justify-between">
          <dt>상태</dt>
          <dd>{{ outcome.payment.status }}</dd>
        </div>
        <div class="flex justify-between">
          <dt>가맹점</dt>
          <dd>{{ outcome.payment.merchantName }}</dd>
        </div>
        <div class="flex justify-between">
          <dt>결제 금액</dt>
          <dd>{{ outcome.payment.amount.toLocaleString() }}원</dd>
        </div>
      </dl>
    </section>
  </main>
</template>
