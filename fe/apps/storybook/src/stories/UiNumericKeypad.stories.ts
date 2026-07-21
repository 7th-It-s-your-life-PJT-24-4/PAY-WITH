import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiNumericKeypad } from '@pay-with/ui'

const meta = {
  title: 'Components/UiNumericKeypad',
  component: UiNumericKeypad,
  args: {
    cancelLabel: '취소',
  },
} satisfies Meta<typeof UiNumericKeypad>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Disabled: Story = {
  args: { disabled: true },
}
