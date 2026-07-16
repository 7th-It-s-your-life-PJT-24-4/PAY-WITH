import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import UiButton from '@/components/UiButton.vue'

describe('UiButton', () => {
  it('renders the provided label and forwards clicks', async () => {
    const wrapper = mount(UiButton, {
      props: {
        label: 'Continue',
      },
    })

    expect(wrapper.get('button').text()).toBe('Continue')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('sets the native disabled state', () => {
    const wrapper = mount(UiButton, {
      props: {
        disabled: true,
        label: 'Continue',
      },
    })

    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })
})
