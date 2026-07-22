import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { Button, Tooltip } from '@pay-with/ui'

const meta = {
  title: 'Components/Tooltip',
  component: Tooltip,
  args: {
    content: 'Continue to the next step',
  },
  render: (args) => ({
    components: { Button, Tooltip },
    setup: () => ({ args }),
    template: `
      <div class="flex min-h-32 items-center justify-center">
        <Tooltip v-bind="args">
          <Button label="Continue" />
        </Tooltip>
      </div>
    `,
  }),
} satisfies Meta<typeof Tooltip>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
