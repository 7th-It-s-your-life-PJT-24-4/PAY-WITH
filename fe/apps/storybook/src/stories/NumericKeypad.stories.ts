import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { NumericKeypad } from '@pay-with/ui'

const meta = {
  title: 'Components/NumericKeypad',
  component: NumericKeypad,
  args: {
    cancelLabel: '취소',
  },
} satisfies Meta<typeof NumericKeypad>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Disabled: Story = {
  args: { disabled: true },
}
