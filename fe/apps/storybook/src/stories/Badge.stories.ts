import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { Badge } from '@pay-with/ui'

const meta = {
  title: 'Components/Badge',
  component: Badge,
  args: {
    label: 'Success',
    status: 'success',
  },
  argTypes: {
    status: {
      control: 'inline-radio',
      options: ['success', 'warning', 'error', 'safe'],
    },
  },
} satisfies Meta<typeof Badge>

export default meta
type Story = StoryObj<typeof meta>

export const Success: Story = {}

export const Warning: Story = {
  args: { label: 'Warning', status: 'warning' },
}

export const Error: Story = {
  args: { label: 'Error', status: 'error' },
}

export const Safe: Story = {
  args: { label: 'Safe', status: 'safe' },
}
