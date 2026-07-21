import { defineMain } from '@storybook/vue3-vite/node'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const uiSource = fileURLToPath(
  new URL('../../../packages/ui/src/index.ts', import.meta.url),
)

export default defineMain({
  framework: '@storybook/vue3-vite',
  stories: ['../src/**/*.stories.@(ts|tsx|js|jsx)'],
  viteFinal: async (config) => {
    config.plugins = [...(config.plugins ?? []), vue(), tailwindcss()]
    config.resolve ??= {}
    const existingAliases = Array.isArray(config.resolve.alias)
      ? config.resolve.alias
      : Object.entries(config.resolve.alias ?? {}).map(
          ([find, replacement]) => ({ find, replacement }),
        )

    config.resolve.alias = [
      { find: /^@pay-with\/ui$/, replacement: uiSource },
      ...existingAliases,
    ]

    return config
  },
})
