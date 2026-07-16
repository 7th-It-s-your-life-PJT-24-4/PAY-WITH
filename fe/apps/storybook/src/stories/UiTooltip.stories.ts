import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { UiButton, UiTooltip } from '@pay-with/ui'

const meta = {
  title: 'Components/UiTooltip',
  component: UiTooltip,
  args: {
    content: 'Continue to the next step',
  },
  render: (args) => ({
    components: { UiButton, UiTooltip },
    setup: () => ({ args }),
    template: `
      <div class="flex min-h-32 items-center justify-center">
        <UiTooltip v-bind="args">
          <UiButton label="Continue" />
        </UiTooltip>
      </div>
    `,
  }),
} satisfies Meta<typeof UiTooltip>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
