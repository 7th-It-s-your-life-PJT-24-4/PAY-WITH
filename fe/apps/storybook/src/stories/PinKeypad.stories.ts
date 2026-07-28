import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { PinKeypad } from '@pay-with/ui'

const meta = {
  title: 'Components/PinKeypad',
  component: PinKeypad,
  args: {
    length: 6,
    randomize: true,
    randomizeOnInput: false,
    pseudoClick: true,
    activeDuration: 180,
    disabled: false,
    cancelLabel: '',
  },
  decorators: [
    () => ({
      template:
        '<div class="mx-auto w-full max-w-[390px] rounded-large bg-surface-card p-xl"><story /></div>',
    }),
  ],
} satisfies Meta<typeof PinKeypad>

export default meta
type Story = StoryObj<typeof meta>

export const TransferPin: Story = {}

export const AccountPin: Story = {
  args: { length: 4 },
}

export const Error: Story = {
  args: { error: '비밀번호가 올바르지 않습니다.' },
}

export const Disabled: Story = {
  args: { disabled: true },
}
