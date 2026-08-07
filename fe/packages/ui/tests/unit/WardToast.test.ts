import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'

import WardToast from '@/components/WardToast.vue'

const wrappers: VueWrapper[] = []

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
})

describe('WardToast', () => {
  it('renders senior toast with large text and message', async () => {
    const wrapper = mount(WardToast, {
      attachTo: document.body,
      props: {
        open: true,
        message: '은행과 계좌번호를 모두 입력해 주세요',
      },
    })
    wrappers.push(wrapper)
    await nextTick()

    const status = document.body.querySelector('[role="status"]')
    expect(status).not.toBeNull()
    expect(status?.textContent).toContain(
      '은행과 계좌번호를 모두 입력해 주세요',
    )
  })
})
