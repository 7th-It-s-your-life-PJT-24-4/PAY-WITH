import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import Progress from '@/components/Progress.vue'

describe('Progress', () => {
  it('exposes the progress value and renders its indicator width', () => {
    const wrapper = mount(Progress, {
      props: {
        label: '이상거래 의심도 게이지',
        indicatorColor: '#ff6161',
        value: 50,
      },
    })

    const progressbar = wrapper.get('[role="progressbar"]')
    const indicator = progressbar.get('[data-state]')

    expect(progressbar.attributes('aria-label')).toBe('이상거래 의심도 게이지')
    expect(progressbar.attributes('aria-valuenow')).toBe('50')
    expect(progressbar.attributes('aria-valuemax')).toBe('100')
    expect(indicator.attributes('style')).toContain('width: 50%')
    expect(indicator.attributes('style')).toContain(
      'background-color: rgb(255, 97, 97)',
    )
  })

  it('clamps an out-of-range value to the configured maximum', () => {
    const wrapper = mount(Progress, {
      props: {
        label: '진행률',
        max: 80,
        value: 100,
      },
    })

    expect(
      wrapper.get('[role="progressbar"]').attributes('aria-valuenow'),
    ).toBe('80')
  })
})
