---
name: fe-scaffold
description: "Vue 3 frontend scaffolding for this repository. Use when the user asks to create or modify Vue 3 <script setup> TypeScript UI components, page components, route entries, Pinia stores, or standalone Zod schemas in the existing fe/apps/web app. Also use when a page needs light local state or store wiring but does not require API endpoint integration. Do not use for API client + TanStack Query hook sets from endpoints; use fe-api-layer. Do not use when the request is only to add Vitest or Playwright tests; use fe-test. Do not use for PR creation or review comment handling."
---

# fe-scaffold

Use this skill to add or modify production FE files in `fe/apps/web` using the conventions in root `AGENTS.md`.

## Workflow

1. Read root `AGENTS.md` first and follow its FE folder, naming, branch, commit, and validation conventions.
2. Inspect the current app before editing:

```bash
cd fe
find apps/web/src -maxdepth 3 -type f | sort
sed -n '1,220p' apps/web/src/router/index.ts
```

3. Decide the file targets:
   - UI component: `apps/web/src/components/PascalName.vue`
   - Page: `apps/web/src/pages/PascalNamePage.vue`
   - Store: `apps/web/src/stores/name.store.ts`
   - Zod schema: `apps/web/src/schemas/name.schema.ts`
   - Route update: `apps/web/src/router/index.ts`
4. Create only the files needed by the request.
5. Use `@/...` imports for app source imports.
6. Keep API calls out of this skill. If the page needs backend data, create or update the API layer through `fe-api-layer` first, then consume the hook from the page.
7. Run validation from `fe`:

```bash
pnpm lint
pnpm build
```

## Vue component pattern

Use `<script setup lang="ts">`. Keep props and emits typed. Use Tailwind utilities directly unless an existing component pattern suggests otherwise. Current UI uses compact bordered sections and slate tones; preserve that style unless the request asks for a different visual direction.

```vue
<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()

const emit = defineEmits<{
  select: [value: string]
}>()
</script>

<template>
  <section class="rounded-lg border border-slate-200 bg-white p-4">
    <h2 class="text-lg font-semibold text-slate-950">{{ title }}</h2>
    <p v-if="description" class="mt-1 text-sm text-slate-600">
      {{ description }}
    </p>
    <button
      class="mt-4 rounded bg-slate-950 px-3 py-2 text-sm font-medium text-white"
      type="button"
      @click="emit('select', title)"
    >
      Select
    </button>
  </section>
</template>
```

## Page pattern

Pages compose components and stores. Keep network calls out of `fe-scaffold`; if the page needs API data, create API hooks through `fe-api-layer`.

```vue
<script setup lang="ts">
import ExampleCard from '@/components/ExampleCard.vue'
import { useExampleStore } from '@/stores/example.store'

const exampleStore = useExampleStore()
</script>

<template>
  <main class="mx-auto max-w-5xl px-6 py-8">
    <ExampleCard
      title="Example"
      :description="`Count: ${exampleStore.count}`"
      @select="exampleStore.increment"
    />
  </main>
</template>
```

## Pinia store pattern

```ts
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useExampleStore = defineStore('example', () => {
  const count = ref(0)
  const doubleCount = computed(() => count.value * 2)

  function increment() {
    count.value += 1
  }

  return { count, doubleCount, increment }
})
```

## Zod schema pattern

Create schema files only for local validation or shared FE domain validation. For API response schemas tied to endpoints, use `fe-api-layer`.

```ts
import { z } from 'zod'

export const exampleSchema = z.object({
  id: z.number(),
  name: z.string().min(1),
})

export type Example = z.infer<typeof exampleSchema>
```

## Router update pattern

```ts
import ExamplePage from '@/pages/ExamplePage.vue'

{
  path: '/examples',
  name: 'examples',
  component: ExamplePage,
}
```
