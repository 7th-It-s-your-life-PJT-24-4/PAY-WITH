import type { LocationQuery, LocationQueryRaw } from 'vue-router'

export function parsePositiveRouteId(value: unknown): number | null {
  const normalized = Array.isArray(value) ? value[0] : value
  if (normalized === null || normalized === undefined || normalized === '')
    return null

  const id = Number(normalized)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

export function withGuardWardId(
  query: LocationQuery,
  wardId: number | null,
): LocationQueryRaw {
  return {
    ...query,
    wardId: wardId ?? undefined,
  }
}
