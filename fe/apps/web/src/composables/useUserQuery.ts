import { useQuery } from '@tanstack/vue-query'

import { getUser } from '@/api/users'

export function useUserQuery(id: number) {
  return useQuery({
    queryKey: ['users', id],
    queryFn: () => getUser(id),
    enabled: id > 0,
  })
}
