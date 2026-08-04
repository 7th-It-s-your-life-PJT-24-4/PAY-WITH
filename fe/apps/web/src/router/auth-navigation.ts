import type { User } from '@/schemas/user.schema'

export function getRoleHomePath(role: User['role']) {
  return role === 'GUARD' ? '/guard' : '/ward'
}
