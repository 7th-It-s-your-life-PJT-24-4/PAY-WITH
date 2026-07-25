import type { Meta, StoryObj } from '@storybook/vue3-vite'
import { Button } from '@pay-with/ui'

const meta = {
  title: 'Components/Button',
  component: Button,
  args: {
    label: 'Continue',
  },
  argTypes: {
    variant: {
      control: 'inline-radio',
      options: [
        'primary',
        'secondary',
        'outline-primary',
        'danger',
        'outline-danger',
        'text',
      ],
    },
    size: {
      control: 'inline-radio',
      options: ['small', 'default', 'large'],
    },
  },
} satisfies Meta<typeof Button>

export default meta
type Story = StoryObj<typeof meta>

export const Primary: Story = {}

export const Secondary: Story = {
  args: {
    variant: 'secondary',
  },
}

export const OutlinePrimary: Story = {
  args: {
    variant: 'outline-primary',
  },
}

export const Danger: Story = {
  args: {
    variant: 'danger',
    label: '거래 취소',
  },
}

export const LargePill: Story = {
  args: {
    label: '다음',
    size: 'large',
    pill: true,
  },
}

export const LeadingIcon: Story = {
  args: {
    label: '송금하기',
    size: 'large',
    pill: true,
  },
  render: (args) => ({
    components: { Button },
    setup: () => ({ args }),
    template: `
      <Button v-bind="args">
        <template #leading>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14M13 6l6 6-6 6" />
          </svg>
        </template>
      </Button>
    `,
  }),
}

export const TrailingIcon: Story = {
  args: {
    label: '다음',
    size: 'large',
    pill: true,
  },
  render: (args) => ({
    components: { Button },
    setup: () => ({ args }),
    template: `
      <Button v-bind="args">
        <template #trailing>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14M13 6l6 6-6 6" />
          </svg>
        </template>
      </Button>
    `,
  }),
}

export const Text: Story = {
  args: {
    label: '자세히 보기',
    variant: 'text',
  },
}

export const Disabled: Story = {
  args: {
    disabled: true,
  },
}
