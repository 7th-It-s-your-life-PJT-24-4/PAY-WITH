import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import Button from '@/components/Button.vue'

describe('Button', () => {
  it('renders the provided label and forwards clicks', async () => {
    const wrapper = mount(Button, {
      props: {
        label: 'Continue',
      },
    })

    expect(wrapper.get('button').text()).toBe('Continue')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('sets the native disabled state', () => {
    const wrapper = mount(Button, {
      props: {
        disabled: true,
        label: 'Continue',
      },
    })

    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })

  it('renders decorative icons before and after the label', () => {
    const wrapper = mount(Button, {
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

  it('renders guard CTA sizing and variant classes', () => {
    const wrapper = mount(Button, {
      props: {
        label: '충전하기',
        variant: 'guard-cta',
        size: 'guard-cta',
      },
    })

    const button = wrapper.get('button')

    expect(button.classes()).toContain('bg-primary-500')
    expect(button.classes()).toContain('border-0')
    expect(button.classes()).toContain('h-14')
    expect(button.classes()).toContain('rounded-[8px]')
  })

  it('renders guard CTA disabled color without opacity', () => {
    const wrapper = mount(Button, {
      props: {
        disabled: true,
        label: '충전하기',
        variant: 'guard-cta',
        size: 'guard-cta',
      },
    })

    const classes = wrapper.get('button').classes()

    expect(classes).toContain('disabled:bg-gray-700')
    expect(classes).toContain('hover:enabled:bg-primary-400')
    expect(classes).not.toContain('disabled:opacity-[var(--opacity-disabled)]')
  })
})
