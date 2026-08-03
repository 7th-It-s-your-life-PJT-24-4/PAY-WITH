import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ConfirmModal from '@/components/ConfirmModal.vue'

describe('ConfirmModal', () => {
  it('renders a danger confirmation action and emits confirm', async () => {
    const wrapper = mount(ConfirmModal, {
      attachTo: document.body,
      props: {
        open: true,
        title: '거래를 거절할까요?',
        confirmLabel: '거절',
        confirmVariant: 'danger',
      },
    })

    await wrapper.vm.$nextTick()

    const confirmButton = Array.from(
      document.body.querySelectorAll('button'),
    ).find((button) => button.textContent === '거절')

    expect(confirmButton?.className).toContain('!bg-error')

    confirmButton?.click()
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('confirm')).toHaveLength(1)

    wrapper.unmount()
  })
})
