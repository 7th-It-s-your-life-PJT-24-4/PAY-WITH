---
name: fe-api-layer
description: "API integration skill for this Vue 3 repository. Use when the user provides or asks for frontend integration with REST endpoints and expects a fetch/axios wrapper, Zod response schema, endpoint functions, and TanStack Query useQuery/useMutation hooks. Use for API base URL environment handling and shared API error handling. Do not use for pure UI components, static pages, route-only work, or Pinia-only state; use fe-scaffold. Do not use when the request is only to test existing code; use fe-test. Do not use for PR creation or review comment handling."
---

# fe-api-layer

Use this skill to create API integration sets in `fe/apps/web`.

## Workflow

1. Read root `AGENTS.md` and follow its FE and API contract conventions.
2. Inspect the backend contract when the endpoint belongs to this repository:

```bash
find be/src/main/java/com/paywith/{controller,dto,common} -type f | sort
sed -n '1,180p' be/src/main/java/com/paywith/common/ApiResponse.java
```

3. Inspect existing API, schema, and composable files:

```bash
cd fe
find apps/web/src/{api,schemas,composables} -maxdepth 2 -type f | sort
sed -n '1,220p' apps/web/src/api/client.ts
```

4. Confirm the endpoint method, path, request body, response shape, auth requirement, and whether it is query or mutation. If any of these are missing and cannot be inferred safely, ask the user.
5. Create or update:
   - Zod schema: `apps/web/src/schemas/<resource>.schema.ts`
   - API endpoint functions: `apps/web/src/api/<resources>.ts`
   - Reusable Query options or hooks only when they add shared behavior; place hooks in `apps/web/src/composables`
   - API base URL env keys: `apps/web/.env.example`, and local `.env` only when needed
6. Run validation:

```bash
pnpm lint
pnpm build
pnpm test
```

## TanStack Query abstraction

Do not create a custom hook that only forwards an endpoint to `useQuery` or `useMutation`. Call TanStack Query directly in the consuming component when no shared options or behavior exist.

```ts
const chargeMutation = useMutation({ mutationFn: createWardCharge })
```

Create a custom hook only when it owns meaningful reusable behavior such as cache updates, invalidation, optimistic updates, polling, dependent-query state, or shared callbacks. A hook such as `useRegisterChargeAccountMutation` is justified when its `onSuccess` updates the account cache.

Define a `queryOptions` factory under `src/lib/query` when the same query configuration or key is reused by components, prefetching, route loaders, or cache operations. Keep `queryKey` and `queryFn` together, pass the factory result to Query APIs, and use `options(params).queryKey` for exact cache reads, updates, or invalidation instead of rebuilding key arrays. Keep a hierarchical key factory as well when broader invalidation such as all or list queries is needed.

```ts
export const wardPaymentKeys = {
  all: ['ward-payments'] as const,
  detail: (paymentId: number) => [...wardPaymentKeys.all, paymentId] as const,
}

export function wardPaymentStatusOptions(paymentId: number) {
  return queryOptions({
    queryKey: wardPaymentKeys.detail(paymentId),
    queryFn: () => getWardPaymentStatus(paymentId),
  })
}

const paymentQuery = useQuery(wardPaymentStatusOptions(paymentId))
queryClient.setQueryData(
  wardPaymentStatusOptions(paymentId).queryKey,
  nextPayment,
)
```

Do not wrap an option factory in another custom hook unless that hook adds behavior.

When a custom hook is justified, name its file and exported function consistently:

- Query: `use<Resource>Query.ts` → `use<Resource>Query`
- Mutation: `use<Action><Resource>Mutation.ts` → `use<Action><Resource>Mutation`
- Query options: `src/lib/query/<resource>.ts` → `<resource>Keys`, `<resource>Options`

Use domain actions such as `Create`, `Register`, `Update`, or `Delete`; do not encode HTTP verbs in hook names.

## API client requirements

Use the existing `apiClient` if present. The backend currently returns `ApiResponse<T>` with `success`, `data`, and `message`, so either the shared client unwraps `data` consistently or each endpoint parses a wrapper schema consistently. Do not mix both styles in one resource module.

If the client is missing, create `apps/web/src/api/client.ts` with:

```ts
import type { ZodSchema } from 'zod'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
}

async function request<TResponse>(
  path: string,
  schema: ZodSchema<TResponse>,
  options: RequestOptions = {},
) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    throw new Error(`API 요청 실패: ${response.status}`)
  }

  const data: unknown = await response.json()
  return schema.parse(data)
}

export const apiClient = {
  get: <TResponse>(path: string, schema: ZodSchema<TResponse>) =>
    request(path, schema),
  post: <TResponse>(
    path: string,
    schema: ZodSchema<TResponse>,
    body: unknown,
  ) => request(path, schema, { method: 'POST', body }),
  put: <TResponse>(
    path: string,
    schema: ZodSchema<TResponse>,
    body: unknown,
  ) => request(path, schema, { method: 'PUT', body }),
  delete: <TResponse>(path: string, schema: ZodSchema<TResponse>) =>
    request(path, schema, { method: 'DELETE' }),
}
```

If implementing backend-wrapped responses, add a small helper schema in the resource schema file or shared schema utilities:

```ts
import { z, type ZodType } from 'zod'

export function apiResponseSchema<T>(dataSchema: ZodType<T>) {
  return z.object({
    success: z.boolean(),
    data: dataSchema,
    message: z.string().nullable(),
  })
}
```

## Environment variables

Keep real secrets out of Git. Ensure `apps/web/.env.example` includes:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

Use `import.meta.env.VITE_API_BASE_URL`; never hardcode base URLs in endpoint modules.

## Endpoint set example

For `GET /api/users` and `POST /api/users` with `VITE_API_BASE_URL=http://localhost:8080/api`, create:

```ts
// apps/web/src/schemas/user.schema.ts
import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const userSchema = z.object({
  id: z.number(),
  email: z.string().email(),
  name: z.string().min(1),
  createdAt: z.string().optional(),
  updatedAt: z.string().optional(),
})

export const usersSchema = z.array(userSchema)
export const userResponseSchema = apiResponseSchema(userSchema)
export const usersResponseSchema = apiResponseSchema(usersSchema)

export const createUserRequestSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  name: z.string().min(1),
})

export type User = z.infer<typeof userSchema>
export type CreateUserRequest = z.infer<typeof createUserRequestSchema>
```

```ts
// apps/web/src/api/users.ts
import { apiClient } from '@/api/client'
import {
  userResponseSchema,
  usersResponseSchema,
  type CreateUserRequest,
  type User,
} from '@/schemas/user.schema'

export function getUsers(): Promise<User[]> {
  return apiClient
    .get('/users', usersResponseSchema)
    .then((response) => response.data)
}

export function createUser(body: CreateUserRequest): Promise<User> {
  return apiClient
    .post('/users', userResponseSchema, body)
    .then((response) => response.data)
}
```

```ts
import { useQuery } from '@tanstack/vue-query'

import { getUsers } from '@/api/users'

const usersQuery = useQuery({
  queryKey: ['users'],
  queryFn: getUsers,
})
```

```ts
// apps/web/src/composables/useCreateUserMutation.ts
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { createUser } from '@/api/users'

export function useCreateUserMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}
```

## Error handling

Keep common HTTP and Zod parse failures in the API wrapper. Components should render query states from TanStack Query: `isPending`, `isError`, `data`, `error`, and `refetch`.
