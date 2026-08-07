import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'

import ChargeAccountNotOwnerModal from '@/pages/ward/charge/-components/ChargeAccountNotOwnerModal.vue'

const wrappers: VueWrapper[] = []

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
})

describe('ChargeAccountNotOwnerModal', () => {
  it('renders senior modal with not-owner warning text and confirmation button', async () => {
    const wrapper = mount(ChargeAccountNotOwnerModal, {
      attachTo: document.body,
      props: {
        open: true,
      },
    })
    wrappers.push(wrapper)
    await nextTick()

    const dialog = document.body.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog?.textContent).toContain('본인 명의의 계좌가 아니에요')
    expect(dialog?.textContent).toContain(
      '충전에 사용하는 계좌는 시니어(본인) 명의의 계좌만 등록할 수 있습니다.',
    )
    expect(dialog?.textContent).toContain('다시 확인하기')
  })

  it('emits confirm and update:open events when button is clicked', async () => {
    const wrapper = mount(ChargeAccountNotOwnerModal, {
      attachTo: document.body,
      props: {
        open: true,
      },
    })
    wrappers.push(wrapper)
    await nextTick()

    const confirmButton = Array.from(
      document.body.querySelectorAll('button'),
    ).find((btn) => btn.textContent?.includes('다시 확인하기'))
    expect(confirmButton).toBeDefined()

    confirmButton?.click()
    await nextTick()

    expect(wrapper.emitted('confirm')).toHaveLength(1)
    expect(wrapper.emitted('update:open')).toEqual([[false]])
  })
})
