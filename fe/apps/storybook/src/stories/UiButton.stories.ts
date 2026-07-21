import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiButton } from '@pay-with/ui'

const meta = {
  title: 'Components/UiButton',
  component: UiButton,
  args: {
    label: 'Continue',
  },
  argTypes: {
    variant: {
      control: 'inline-radio',
      options: [
        'primary',
        'secondary',
        'outline-primary',
        'danger',
        'outline-danger',
        'text',
      ],
    },
    size: {
      control: 'inline-radio',
      options: ['small', 'default', 'large'],
    },
  },
} satisfies Meta<typeof UiButton>

export default meta
type Story = StoryObj<typeof meta>

export const Primary: Story = {}

export const Secondary: Story = {
  args: {
    variant: 'secondary',
  },
}

export const OutlinePrimary: Story = {
  args: {
    variant: 'outline-primary',
  },
}

export const Danger: Story = {
  args: {
    variant: 'danger',
    label: '거래 취소',
  },
}

export const LargePill: Story = {
  args: {
    label: '다음',
    size: 'large',
    pill: true,
  },
}

export const Text: Story = {
  args: {
    label: '자세히 보기',
    variant: 'text',
  },
}

export const Disabled: Story = {
  args: {
    disabled: true,
  },
}
