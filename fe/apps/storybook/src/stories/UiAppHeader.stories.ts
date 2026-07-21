import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiAppHeader } from '@pay-with/ui'

const meta = {
  title: 'Components/UiAppHeader',
  component: UiAppHeader,
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
} satisfies Meta<typeof UiAppHeader>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const TitleOnly: Story = {
  args: {
    showBack: false,
    showProfile: false,
  },
}
