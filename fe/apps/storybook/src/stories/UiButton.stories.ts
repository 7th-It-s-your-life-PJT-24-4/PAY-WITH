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
      options: ['primary', 'secondary'],
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

export const Disabled: Story = {
  args: {
    disabled: true,
  },
}
