import { useQuery } from '@tanstack/vue-query'

import { getUsers } from '@/api/users'

export function useUsersQuery() {
  return useQuery({
    queryKey: ['users'],
    queryFn: getUsers,
  })
}
