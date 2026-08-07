import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import TransferGuardianCallModal from '@/pages/ward/transfer/-components/TransferGuardianCallModal.vue'

vi.mock('@/lib/query/ward/guardian', () => ({
  wardGuardianQueryOptions: () => ({
    queryKey: ['ward-guardian'],
    queryFn: () =>
      Promise.resolve({
        name: '이보호',
        phone: '01098765432',
      }),
  }),
}))

describe('TransferGuardianCallModal', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  let wrapper: VueWrapper | null = null

  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(globalThis, 'open').mockImplementation(() => null)
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
    vi.restoreAllMocks()
  })

  it('API에서 조회된 보호자의 이름과 포맷팅된 전화번호를 모달에 표시한다', async () => {
    queryClient.setQueryData(['ward-guardian'], {
      name: '이보호',
      phone: '01098765432',
    })

    wrapper = mount(TransferGuardianCallModal, {
      props: { open: true },
      attachTo: document.body,
      global: {
        plugins: [[VueQueryPlugin, { queryClient }]],
      },
    })
    await flushPromises()

    expect(document.body.textContent).toContain('이보호님의 연결된 번호')
    expect(document.body.textContent).toContain('010-9876-5432')
  })

  it('전화 걸기 버튼 클릭 시 tel: 링크로 이동한다', async () => {
    queryClient.setQueryData(['ward-guardian'], {
      name: '이보호',
      phone: '01098765432',
    })

    wrapper = mount(TransferGuardianCallModal, {
      props: { open: true },
      attachTo: document.body,
      global: {
        plugins: [[VueQueryPlugin, { queryClient }]],
      },
    })
    await flushPromises()

    const buttons = Array.from(document.querySelectorAll('button'))
    const callButton = buttons.find(
      (b) => b.textContent?.trim() === '전화 걸기',
    )

    expect(callButton).not.toBeUndefined()
    callButton?.click()

    expect(globalThis.open).toHaveBeenCalledWith('tel:01098765432', '_self')
  })
})
