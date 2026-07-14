---
name: fe-test
description: "Frontend testing skill for this repository. Use when the user asks to add or update Vitest unit tests for existing Vue components, composables, stores, schemas, or API utilities, or Playwright E2E tests for existing user flows in fe/apps/web. Use when the main deliverable is tests. Do not use to create production components, pages, stores, schemas, or API hooks; use fe-scaffold or fe-api-layer. Do not use for PR creation or review comment handling."
---

# fe-test

Use this skill to test existing FE behavior in `fe/apps/web`.

## Workflow

1. Read root `AGENTS.md` and follow its FE conventions.
2. Inspect the target production code, package scripts, and existing tests:

```bash
cd fe
sed -n '1,220p' apps/web/package.json
find apps/web/tests -maxdepth 3 -type f | sort
find apps/web/src -maxdepth 3 -type f | sort
```

3. Choose test type:
   - Vitest: component rendering, composables, Pinia stores, schemas, API wrapper behavior.
   - Playwright: browser user flow, route navigation, form submission, visible UI state.
4. Add tests without changing production code unless a real bug blocks the test. If production changes are needed, stop and explain the bug before editing.
5. Run:

```bash
pnpm test
```

6. If Playwright tests changed, also run:

```bash
pnpm test:e2e
```

## Vitest locations

- Unit tests live in `apps/web/tests/unit`.
- Name tests by target: `user.schema.test.ts`, `useUsersQuery.test.ts`, `ExampleCard.test.ts`.
- Use `@/...` imports for app source imports.
- The Vite/Vitest config currently includes `tests/unit/**/*.test.ts` and uses `jsdom`.

## Component test pattern

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ExampleCard from '@/components/ExampleCard.vue'

describe('ExampleCard', () => {
  it('renders the title and emits select', async () => {
    const wrapper = mount(ExampleCard, {
      props: {
        title: 'Example',
        description: 'Description',
      },
    })

    expect(wrapper.text()).toContain('Example')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual(['Example'])
  })
})
```

## Schema/composable test pattern

```ts
import { describe, expect, it } from 'vitest'

import { userSchema } from '@/schemas/user.schema'

describe('userSchema', () => {
  it('validates a user response', () => {
    expect(
      userSchema.parse({
        id: 1,
        email: 'user@example.com',
        name: 'User',
      }),
    ).toMatchObject({ id: 1 })
  })
})
```

## Playwright locations

- E2E tests live in `apps/web/tests/e2e`.
- Name tests by flow: `login.spec.ts`, `users.spec.ts`, `home.spec.ts`.
- Mock network calls with `page.route` unless the user explicitly asks for live backend integration.
- The Playwright config starts `pnpm dev --host 127.0.0.1` and uses `http://127.0.0.1:5173`.

## Playwright flow pattern

```ts
import { expect, test } from '@playwright/test'

test('creates a user from the UI', async ({ page }) => {
  await page.route('**/users', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        contentType: 'application/json',
        json: { id: 1, email: 'new@example.com', name: 'New User' },
      })
      return
    }

    await route.fulfill({
      contentType: 'application/json',
      json: [],
    })
  })

  await page.goto('/users')
  await page.getByLabel('Email').fill('new@example.com')
  await page.getByLabel('Name').fill('New User')
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page.getByText('New User')).toBeVisible()
})
```

## Testing rules

- Prefer user-observable assertions over implementation details.
- Do not assert Tailwind class strings unless the request is specifically visual styling.
- For Query hooks, mock API functions or network behavior; do not hit production endpoints.
- Keep tests deterministic and independent.
