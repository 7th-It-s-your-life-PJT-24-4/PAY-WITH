import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { AppHeader } from '@pay-with/ui'

const meta = {
  title: 'Components/AppHeader',
  component: AppHeader,
  args: {
    title: 'PayWith',
    showBack: true,
    showProfile: true,
  },
  decorators: [
    () => ({
      template: '<div class="w-[390px]"><story /></div>',
    }),
  ],
} satisfies Meta<typeof AppHeader>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const TitleOnly: Story = {
  args: {
    showBack: false,
    showProfile: false,
  },
}

export const Primary: Story = {
  args: {
    variant: 'primary',
  },
}
