import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiButton, UiInput, UiModal } from '@pay-with/ui'
import { ref } from 'vue'

const meta = {
  title: 'Components/UiModal',
  component: UiModal,
  args: {
    open: true,
    size: 'large',
    title: '지갑 잠김',
    description:
      '지갑이 잠겨있어 진행할 수 없습니다.\n보호자에게 연락하여 확인하세요.',
  },
  parameters: {
    layout: 'fullscreen',
  },
} satisfies Meta<typeof UiModal>

export default meta
type Story = StoryObj<typeof meta>

export const Information: Story = {
  render: (args) => ({
    components: { UiButton, UiModal },
    setup() {
      const open = ref(args.open)
      return { args, open }
    },
    template: `
      <div class="flex min-h-screen items-center justify-center bg-background">
        <UiButton label="모달 열기" @click="open = true" />
        <UiModal v-bind="args" v-model:open="open" icon-tone="error">
          <template #icon>
            <span class="type-h1" aria-hidden="true">!</span>
          </template>
          <template #actions="{ buttonSize, close }">
            <UiButton class="w-full" label="확인" :size="buttonSize" pill @click="close" />
          </template>
        </UiModal>
      </div>
    `,
  }),
}

export const Confirmation: Story = {
  args: {
    size: 'default',
    title: '지갑 잠금을 해제할까요?',
    description:
      '위험 거래가 감지되어 자동으로 잠긴 지갑입니다.\n안전한지 확인 후 해제를 권장합니다.',
    iconTone: 'success',
  },
  render: (args) => ({
    components: { UiButton, UiModal },
    setup() {
      const open = ref(args.open)
      return { args, open }
    },
    template: `
      <div class="flex min-h-screen items-center justify-center bg-background">
        <UiButton label="모달 열기" @click="open = true" />
        <UiModal v-bind="args" v-model:open="open">
          <template #icon>
            <span class="type-h2" aria-hidden="true">✓</span>
          </template>
          <template #actions="{ buttonSize, close }">
            <div class="grid grid-cols-2 gap-sm">
              <UiButton class="w-full" label="아니오" :size="buttonSize" variant="outline-primary" pill @click="close" />
              <UiButton class="w-full" label="예" :size="buttonSize" pill @click="close" />
            </div>
          </template>
        </UiModal>
      </div>
    `,
  }),
}

export const Form: Story = {
  args: {
    size: 'large',
    title: '연락처 추가',
    description: '아래 계좌를 연락처에 추가할까요?',
  },
  render: (args) => ({
    components: { UiButton, UiInput, UiModal },
    setup() {
      const open = ref(args.open)
      const alias = ref('')
      return { alias, args, open }
    },
    template: `
      <div class="flex min-h-screen items-center justify-center bg-background">
        <UiButton label="모달 열기" @click="open = true" />
        <UiModal v-bind="args" v-model:open="open">
          <template #icon>
            <span class="type-h1" aria-hidden="true">+</span>
          </template>
          <div class="border-b border-border pb-xl">
            <p class="type-h2 text-body">홍길동</p>
            <p class="type-h3 mt-xs text-body-muted">우리은행 1002-123-456789</p>
          </div>
          <UiInput
            v-model="alias"
            label="연락처 별칭"
            placeholder="연락처 별칭 입력(선택)"
            large
          />
          <template #actions="{ buttonSize, close }">
            <div class="flex flex-col gap-md">
              <UiButton class="w-full" label="추가하기" :size="buttonSize" pill @click="close" />
              <UiButton class="w-full" label="취소" :size="buttonSize" variant="outline-primary" pill @click="close" />
            </div>
          </template>
        </UiModal>
      </div>
    `,
  }),
}
