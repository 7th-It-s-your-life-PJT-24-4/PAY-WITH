import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'

import BottomSheet from '@/components/BottomSheet.vue'

const wrappers: VueWrapper[] = []

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
})

function mountBottomSheet() {
  const wrapper = mount(BottomSheet, {
    attachTo: document.body,
    props: {
      open: true,
      title: '계좌 선택',
      description: '충전에 사용할 계좌를 선택합니다.',
    },
    slots: {
      default: '<button type="button">국민 3700</button>',
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

describe('BottomSheet', () => {
  it('renders an accessible dialog with slotted content', async () => {
    mountBottomSheet()
    await nextTick()

    const dialog = document.body.querySelector('[role="dialog"]')
    const titleId = dialog?.getAttribute('aria-labelledby')
    const descriptionId = dialog?.getAttribute('aria-describedby')

    expect(dialog).not.toBeNull()
    expect(document.getElementById(titleId ?? '')?.textContent).toContain(
      '계좌 선택',
    )
    expect(document.getElementById(descriptionId ?? '')?.textContent).toContain(
      '충전에 사용할 계좌를 선택합니다.',
    )
    expect(document.body.textContent).toContain('국민 3700')
  })

  it('emits an open update when the close button is clicked', async () => {
    const wrapper = mountBottomSheet()
    await nextTick()

    ;(
      document.body.querySelector('[aria-label="닫기"]') as HTMLButtonElement
    ).click()
    await nextTick()

    expect(wrapper.emitted('update:open')?.at(-1)).toEqual([false])
  })
})
