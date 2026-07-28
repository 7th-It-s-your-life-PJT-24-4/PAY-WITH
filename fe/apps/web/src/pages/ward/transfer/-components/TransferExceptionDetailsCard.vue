<script setup lang="ts">
import { Badge } from '@pay-with/ui'

interface TransferExceptionDetailRow {
  label: string
  value: string
  emphasis?: 'primary' | 'error'
  numeric?: boolean
  large?: boolean
}

withDefaults(
  defineProps<{
    title: string
    rows: TransferExceptionDetailRow[]
    badgeLabel?: string
    badgeStatus?: 'success' | 'warning' | 'error' | 'safe'
  }>(),
  {
    badgeLabel: undefined,
    badgeStatus: 'warning',
  },
)
</script>

<template>
  <section
    class="overflow-hidden rounded-large border border-border bg-surface-card shadow-card"
  >
    <header
      class="flex min-h-14 items-center justify-between border-b border-border bg-disabled/35 px-lg py-md"
    >
      <h3 class="type-body-medium text-primary-300">{{ title }}</h3>
      <Badge v-if="badgeLabel" :label="badgeLabel" :status="badgeStatus" />
    </header>

    <dl class="px-xl py-md">
      <div
        v-for="(row, index) in rows"
        :key="row.label"
        class="flex min-h-12 items-center justify-between gap-md py-sm"
        :class="index < rows.length - 1 ? 'border-b border-border' : ''"
      >
        <dt class="type-body-medium shrink-0 text-body-muted">
          {{ row.label }}
        </dt>
        <dd
          class="min-w-0 text-right"
          :class="[
            row.emphasis || row.large ? 'type-h3' : 'type-body-medium',
            row.numeric ? 'font-number' : '',
            row.emphasis === 'primary'
              ? 'text-primary-500'
              : row.emphasis === 'error'
                ? 'text-error'
                : 'text-body',
          ]"
        >
          {{ row.value }}
        </dd>
      </div>
    </dl>
  </section>
</template>
