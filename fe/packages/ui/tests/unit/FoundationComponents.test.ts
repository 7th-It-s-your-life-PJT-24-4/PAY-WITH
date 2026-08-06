import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AppHeader from '@/components/AppHeader.vue'
import Badge from '@/components/Badge.vue'
import BottomNavigation from '@/components/BottomNavigation.vue'
import Input from '@/components/Input.vue'
import NumericKeypad from '@/components/NumericKeypad.vue'
import Tabs from '@/components/Tabs.vue'

describe('Input', () => {
  it('connects its label and emits input updates', async () => {
    const wrapper = mount(Input, {
      props: {
        label: '이름',
        modelValue: '',
      },
    })

    const input = wrapper.get('input')
    expect(wrapper.get('label').attributes('for')).toBe(input.attributes('id'))

    await input.setValue('김시니어')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['김시니어'])
  })

  it('exposes an error to assistive technology', () => {
    const wrapper = mount(Input, {
      props: {
        error: '필수 입력값입니다.',
        label: '이름',
      },
    })

    expect(wrapper.get('input').attributes('aria-invalid')).toBe('true')
    expect(wrapper.text()).toContain('필수 입력값입니다.')
  })
})

describe('Badge', () => {
  it('renders the semantic status label', () => {
    const wrapper = mount(Badge, {
      props: { label: 'Safe', status: 'safe' },
    })

    expect(wrapper.text()).toContain('Safe')
    expect(wrapper.find('svg').exists()).toBe(true)
  })
})

describe('Tabs', () => {
  it('emits the selected tab value', async () => {
    const wrapper = mount(Tabs, {
      props: {
        ariaLabel: '거래 필터',
        items: [
          { value: 'all', label: '전체' },
          { value: 'deposit', label: '입금' },
        ],
      },
    })

    await wrapper.findAll('[role="tab"]')[1]?.trigger('mousedown')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['deposit'])
    expect(
      wrapper.findAll('[role="tab"]')[1]?.attributes('aria-selected'),
    ).toBe('true')
  })
})

describe('AppHeader', () => {
  it('emits navigation actions', async () => {
    const wrapper = mount(AppHeader, {
      props: { title: 'PayWith', showBack: true, showProfile: true },
    })

    await wrapper.get('[aria-label="뒤로 가기"]').trigger('click')
    await wrapper.get('[aria-label="내 정보"]').trigger('click')

    expect(wrapper.emitted('back')).toHaveLength(1)
    expect(wrapper.emitted('profile')).toHaveLength(1)
  })

  it('supports primary header colors', () => {
    const wrapper = mount(AppHeader, {
      props: {
        title: 'PayWith',
        showBack: true,
        showProfile: true,
        variant: 'primary',
      },
    })

    expect(wrapper.get('header').classes()).toContain('bg-primary-500')
    expect(wrapper.get('h1').classes()).toContain('text-white')
    expect(wrapper.get('[aria-label="뒤로 가기"]').classes()).toContain(
      'text-white',
    )
    expect(wrapper.get('[aria-label="내 정보"]').classes()).toContain(
      'text-white',
    )
  })
})

describe('BottomNavigation', () => {
  it('marks the active item and emits navigation', async () => {
    const wrapper = mount(BottomNavigation, {
      props: {
        active: 'home',
        items: [
          { value: 'home', label: '홈' },
          { value: 'history', label: '내역' },
        ],
      },
    })

    expect(wrapper.get('[aria-current="page"]').text()).toContain('홈')
    await wrapper.findAll('button')[1]?.trigger('click')

    expect(wrapper.emitted('navigate')?.[0]).toEqual(['history'])
  })

  it('keeps the ward center action foreground independent from active state', async () => {
    const wrapper = mount(BottomNavigation, {
      props: {
        active: 'home',
        variant: 'ward',
        centerActionValue: 'home',
        items: [
          { value: 'transfer', label: '송금' },
          { value: 'home', label: '홈' },
          { value: 'payment', label: '결제' },
        ],
      },
    })
    const centerAction = wrapper.get('[data-center-action="true"]')

    expect(centerAction.classes()).toContain('text-white')

    await wrapper.setProps({ active: 'transfer' })

    expect(centerAction.classes()).toContain('text-white')
    expect(wrapper.get('[aria-current="page"]').text()).toContain('송금')
  })
})

describe('NumericKeypad', () => {
  it('emits number, cancel, and backspace actions', async () => {
    const wrapper = mount(NumericKeypad)

    await wrapper.get('[aria-label="1"]').trigger('click')
    await wrapper.get('button:nth-of-type(10)').trigger('click')
    await wrapper.get('[aria-label="한 글자 지우기"]').trigger('click')

    expect(wrapper.emitted('input')?.[0]).toEqual(['1'])
    expect(wrapper.emitted('cancel')).toHaveLength(1)
    expect(wrapper.emitted('backspace')).toHaveLength(1)
  })
})
