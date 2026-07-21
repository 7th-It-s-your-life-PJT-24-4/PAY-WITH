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

  it('renders decorative icons before and after the label', () => {
    const wrapper = mount(UiButton, {
      props: {
        label: 'Continue',
      },
      slots: {
        leading: '<svg data-icon="leading" />',
        trailing: '<svg data-icon="trailing" />',
      },
    })

    const buttonChildren = wrapper.get('button').element.children

    expect(
      buttonChildren[0]?.querySelector('[data-icon="leading"]'),
    ).not.toBeNull()
    expect(buttonChildren[1]?.textContent).toBe('Continue')
    expect(
      buttonChildren[2]?.querySelector('[data-icon="trailing"]'),
    ).not.toBeNull()
    expect(buttonChildren[0]?.getAttribute('aria-hidden')).toBe('true')
    expect(buttonChildren[2]?.getAttribute('aria-hidden')).toBe('true')
  })
})
