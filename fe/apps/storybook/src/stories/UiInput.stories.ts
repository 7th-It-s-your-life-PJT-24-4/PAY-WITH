import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiInput } from '@pay-with/ui'

const meta = {
  title: 'Components/UiInput',
  component: UiInput,
  args: {
    label: '이름',
    modelValue: '',
    placeholder: '내용을 입력하세요',
  },
  decorators: [
    () => ({
      template: '<div class="w-[320px]"><story /></div>',
    }),
  ],
} satisfies Meta<typeof UiInput>

export default meta
type Story = StoryObj<typeof meta>

export const Text: Story = {}

export const Password: Story = {
  args: {
    label: '비밀번호',
    type: 'password',
    modelValue: '123456',
  },
}

export const Numeric: Story = {
  args: {
    label: '인증번호',
    modelValue: '123456',
    inputmode: 'numeric',
    numeric: true,
  },
}

export const NumericLarge: Story = {
  args: {
    label: '송금 금액',
    modelValue: '100000',
    inputmode: 'numeric',
    numeric: true,
    large: true,
  },
}

export const Error: Story = {
  args: {
    error: '입력값을 다시 확인해 주세요.',
  },
}
