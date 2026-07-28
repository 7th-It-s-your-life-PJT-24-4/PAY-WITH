import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

import PinKeypad from '@/components/PinKeypad.vue'

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('PinKeypad', () => {
  it('입력된 비밀번호 대신 입력 자릿수만 표시한다', async () => {
    const wrapper = mount(PinKeypad, {
      props: { length: 4, randomize: false, pseudoClick: false },
    })

    await wrapper.get('[aria-label="1"]').trigger('click')
    await wrapper.get('[aria-label="2"]').trigger('click')

    expect(wrapper.get('[role="status"]').attributes('aria-label')).toBe(
      '비밀번호 2자리 입력됨',
    )
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.emitted('change')).toEqual([[1], [2]])
  })

  it('지정한 길이에서 complete 이벤트를 한 번 발생시킨다', async () => {
    const wrapper = mount(PinKeypad, {
      props: { length: 4, randomize: false, pseudoClick: false },
    })

    for (const key of ['1', '2', '3', '4', '5']) {
      await wrapper.get(`[aria-label="${key}"]`).trigger('click')
    }

    expect(wrapper.emitted('complete')).toEqual([['1234']])
    expect(wrapper.get('[role="status"]').attributes('aria-label')).toBe(
      '비밀번호 4자리 입력됨',
    )
  })

  it('삭제와 취소로 입력 자릿수를 갱신한다', async () => {
    const wrapper = mount(PinKeypad, {
      props: { randomize: false, pseudoClick: false },
    })

    await wrapper.get('[aria-label="1"]').trigger('click')
    await wrapper.get('[aria-label="한 글자 지우기"]').trigger('click')
    await wrapper.get('[aria-label="2"]').trigger('click')
    await wrapper.get('[aria-label="비밀번호 입력 취소"]').trigger('click')

    expect(wrapper.emitted('change')).toEqual([[1], [0], [1], [0]])
    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })

  it('pseudo 클릭은 다른 버튼의 시각 상태만 활성화한다', async () => {
    vi.useFakeTimers()
    vi.spyOn(Math, 'random').mockReturnValue(0)
    const wrapper = mount(PinKeypad, {
      props: {
        randomize: false,
        pseudoClick: true,
        activeDuration: 180,
      },
    })

    await wrapper.get('[aria-label="1"]').trigger('click')

    expect(wrapper.get('[aria-label="1"]').attributes('data-feedback')).toBe(
      'active',
    )
    expect(wrapper.find('[data-feedback="pseudo"]').exists()).toBe(true)
    expect(wrapper.emitted('change')).toEqual([[1]])

    vi.advanceTimersByTime(180)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-feedback]').exists()).toBe(false)
  })

  it('disabled 상태에서는 입력하지 않는다', async () => {
    const wrapper = mount(PinKeypad, {
      props: { disabled: true, randomize: false },
    })

    await wrapper.get('[aria-label="1"]').trigger('click')

    expect(wrapper.emitted('change')).toBeUndefined()
    expect(wrapper.get('[role="status"]').attributes('aria-label')).toBe(
      '비밀번호 0자리 입력됨',
    )
  })
})
