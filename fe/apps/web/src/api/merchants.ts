import { apiClient } from '@/api/client'
import {
  merchantListResponseSchema,
  type Merchant,
} from '@/schemas/merchant.schema'

/** 가맹점 스캐너용 무토큰 개방 API — 결제 가능한(좌표가 있는) 가맹점만 내려온다 */
export async function getMerchants(): Promise<Merchant[]> {
  const response = await apiClient.get('/merchants', merchantListResponseSchema)
  return response.data.merchants
}
