import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'

import AddTransferContactModal from '@/pages/ward/transfer/-components/AddTransferContactModal.vue'

const wrappers: VueWrapper[] = []
const recipient = {
  id: 2,
  name: '박지연',
  bank: '신한은행',
  accountNumber: '110-234-567890',
}

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
})

function mountModal() {
  const wrapper = mount(AddTransferContactModal, {
    attachTo: document.body,
    props: {
      open: true,
      recipient,
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

describe('AddTransferContactModal', () => {
  it('수취인 계좌 정보를 표시한다', async () => {
    mountModal()
    await nextTick()

    expect(document.body.textContent).toContain('안심계좌 추가')
    expect(document.body.textContent).toContain('박지연')
    expect(document.body.textContent).toContain('신한은행 110-234-567890')
  })

  it('입력한 별칭으로 안심계좌 추가를 요청한다', async () => {
    const wrapper = mountModal()
    await nextTick()

    const input = document.body.querySelector('input') as HTMLInputElement
    input.value = '지연 이모'
    input.dispatchEvent(new Event('input', { bubbles: true }))
    await nextTick()

    const addButton = [...document.body.querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === '추가하기',
    )
    addButton?.click()
    await nextTick()

    expect(wrapper.emitted('confirm')?.at(-1)).toEqual(['지연 이모'])
  })
})
