import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import Input from '@/components/Input.vue'

describe('Input', () => {
  it('한글 조합 중에는 필터링하지 않고 완성 후 값을 전달한다', async () => {
    const wrapper = mount(Input, {
      props: {
        inputFilter: /[^가-힣]/g,
        label: '성함',
      },
    })
    const input = wrapper.get('input')

    ;(input.element as HTMLInputElement).value = 'ㅎ'
    await input.trigger('input', { isComposing: true })

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    expect((input.element as HTMLInputElement).value).toBe('ㅎ')

    ;(input.element as HTMLInputElement).value = '홍'
    await input.trigger('compositionend')

    expect(wrapper.emitted('update:modelValue')).toEqual([['홍']])
  })
})
