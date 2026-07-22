import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { Tabs } from '@pay-with/ui'

const items = [
  { value: 'all', label: '전체' },
  { value: 'deposit', label: '입금' },
  { value: 'withdrawal', label: '출금' },
  { value: 'pending', label: '승인 대기' },
]

const meta = {
  title: 'Components/Tabs',
  component: Tabs,
  args: {
    ariaLabel: '거래 유형 필터',
    items,
    modelValue: 'all',
  },
} satisfies Meta<typeof Tabs>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Selected: Story = {
  args: {
    modelValue: 'withdrawal',
  },
}
