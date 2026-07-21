import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiBottomNavigation } from '@pay-with/ui'

const guardianItems = [
  { value: 'home', label: '홈' },
  { value: 'charge', label: '충전' },
  { value: 'history', label: '내역' },
  { value: 'report', label: '리포트' },
]

const wardItems = [
  { value: 'transfer', label: '송금' },
  { value: 'payment', label: '결제' },
  { value: 'history', label: '내역' },
]

const meta = {
  title: 'Components/UiBottomNavigation',
  component: UiBottomNavigation,
  args: {
    active: 'home',
    items: guardianItems,
    variant: 'guardian',
  },
  decorators: [
    () => ({
      template: '<div class="w-[390px] pt-12"><story /></div>',
    }),
  ],
  render: (args) => ({
    components: { UiBottomNavigation },
    setup: () => ({ args }),
    template: `
      <UiBottomNavigation v-bind="args">
        <template #icon>●</template>
      </UiBottomNavigation>
    `,
  }),
} satisfies Meta<typeof UiBottomNavigation>

export default meta
type Story = StoryObj<typeof meta>

export const Guardian: Story = {}

export const Ward: Story = {
  args: {
    active: 'payment',
    items: wardItems,
    variant: 'ward',
    centerActionValue: 'payment',
  },
}
