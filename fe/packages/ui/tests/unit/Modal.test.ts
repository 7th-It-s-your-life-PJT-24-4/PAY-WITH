import { mount, type VueWrapper } from '@vue/test-utils'
import { h, nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'

import Modal from '@/components/Modal.vue'

const wrappers: VueWrapper[] = []

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
})

function mountModal(props: Partial<InstanceType<typeof Modal>['$props']> = {}) {
  const wrapper = mount(Modal, {
    attachTo: document.body,
    props: {
      open: true,
      title: '지갑 잠김',
      description: '보호자에게 연락하여 확인하세요.',
      ...props,
    },
    slots: {
      actions: '<button type="button">확인</button>',
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

describe('Modal', () => {
  it('renders an accessible dialog title and description', async () => {
    mountModal()
    await nextTick()

    const dialog = document.body.querySelector('[role="dialog"]')
    const titleId = dialog?.getAttribute('aria-labelledby')
    const descriptionId = dialog?.getAttribute('aria-describedby')

    expect(dialog).not.toBeNull()
    expect(document.getElementById(titleId ?? '')?.textContent).toContain(
      '지갑 잠김',
    )
    expect(document.getElementById(descriptionId ?? '')?.textContent).toContain(
      '보호자에게 연락하여 확인하세요.',
    )
  })

  it('emits a close update when Escape is pressed', async () => {
    const wrapper = mountModal()
    await nextTick()

    document.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    )
    await nextTick()

    expect(wrapper.emitted('update:open')?.at(-1)).toEqual([false])
  })

  it('keeps the dialog open on Escape when configured', async () => {
    const wrapper = mountModal({ closeOnEscape: false })
    await nextTick()
    const emittedBeforeEscape = wrapper.emitted('update:open')?.length ?? 0

    document.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    )
    await nextTick()

    expect(wrapper.emitted('update:open')?.length ?? 0).toBe(
      emittedBeforeEscape,
    )
  })

  it('provides a close action to the actions slot', async () => {
    const wrapper = mount(Modal, {
      attachTo: document.body,
      props: { open: true, title: '확인' },
      slots: {
        actions: ({ close }: { close: () => void }) =>
          h('button', { type: 'button', onClick: close }, '닫기'),
      },
    })
    wrappers.push(wrapper)
    await nextTick()

    const button = document.body.querySelector('button') as HTMLButtonElement
    button.click()
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:open')?.at(-1)).toEqual([false])
  })

  it('provides the large button size to senior modal actions', async () => {
    const wrapper = mount(Modal, {
      attachTo: document.body,
      props: { open: true, title: '연락처 추가', size: 'large' },
      slots: {
        actions: ({ buttonSize }: { buttonSize: string }) =>
          h('button', { 'data-size': buttonSize }, '추가하기'),
      },
    })
    wrappers.push(wrapper)
    await nextTick()

    expect(document.body.querySelector('button')?.dataset.size).toBe('large')
  })
})
