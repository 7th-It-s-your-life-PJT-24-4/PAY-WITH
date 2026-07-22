<script setup lang="ts">
import { AppHeader } from '@pay-with/ui'

import { useUsersQuery } from '@/composables/useUsersQuery'
import { useCounterStore } from '@/stores/counter.store'

const counter = useCounterStore()
const usersQuery = useUsersQuery()
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <AppHeader title="pay-with frontend" />

    <main
      class="mx-auto grid max-w-5xl gap-6 px-6 py-8 md:grid-cols-[320px_1fr]"
    >
      <section class="rounded-lg border border-slate-200 bg-white p-5">
        <p class="text-sm font-medium text-slate-500">Pinia store</p>
        <h1 class="mt-2 text-3xl font-semibold text-slate-950">
          Count: {{ counter.count }}
        </h1>
        <p class="mt-1 text-slate-600">Double: {{ counter.doubleCount }}</p>

        <div class="mt-5 flex gap-2">
          <button
            class="rounded bg-slate-950 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
            type="button"
            @click="counter.increment"
          >
            Increment
          </button>
          <button
            class="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
            type="button"
            @click="counter.reset"
          >
            Reset
          </button>
        </div>
      </section>

      <section class="rounded-lg border border-slate-200 bg-white p-5">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-sm font-medium text-slate-500">TanStack Query</p>
            <h2 class="mt-2 text-2xl font-semibold text-slate-950">
              Users from API
            </h2>
          </div>
          <button
            class="rounded border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
            type="button"
            @click="usersQuery.refetch()"
          >
            Refetch
          </button>
        </div>

        <p v-if="usersQuery.isPending.value" class="mt-5 text-slate-600">
          Loading users...
        </p>
        <p v-else-if="usersQuery.isError.value" class="mt-5 text-red-600">
          Failed to load users.
        </p>
        <ul v-else class="mt-5 divide-y divide-slate-100">
          <li
            v-for="user in usersQuery.data.value?.slice(0, 5)"
            :key="user.id"
            class="py-3"
          >
            <p class="font-medium text-slate-950">{{ user.name }}</p>
            <p class="text-sm text-slate-500">{{ user.email }}</p>
          </li>
        </ul>
      </section>
    </main>
  </div>
</template>
