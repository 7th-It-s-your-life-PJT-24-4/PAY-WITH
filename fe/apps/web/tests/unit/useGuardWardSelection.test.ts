import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { defineComponent, h, ref } from 'vue'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { beforeEach, describe, expect, it } from 'vitest'

import { useGuardWardSelection } from '@/pages/guard/-composables/useGuardWardSelection'
import { useGuardStore } from '@/stores/guard.store'

type WardSelection = ReturnType<typeof useGuardWardSelection>

describe('useGuardWardSelection', () => {
  let router: Router
  let selection: WardSelection
  let guardStore: ReturnType<typeof useGuardStore>

  beforeEach(async () => {
    router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/guard', component: { template: '<div />' } }],
    })
    await router.push('/guard?wardId=12&type=TRANSFER')

    const pinia = createPinia()
    const TestComponent = defineComponent({
      setup() {
        selection = useGuardWardSelection()
        return () => h('div')
      },
    })
    mount(TestComponent, { global: { plugins: [pinia, router] } })
    guardStore = useGuardStore(pinia)
  })

  it('URL의 wardId를 보호자 선택 상태로 복원한다', () => {
    expect(selection.activeWardId.value).toBe(12)
    expect(guardStore.activeWardId).toBe(12)
  })

  it('시니어 선택 시 기존 query를 보존하며 URL과 store를 함께 갱신한다', async () => {
    await expect(selection.selectWard('13')).resolves.toBe(true)

    expect(router.currentRoute.value.query).toEqual({
      wardId: '13',
      type: 'TRANSFER',
    })
    expect(guardStore.activeWardId).toBe(13)
  })

  it('서버가 선택한 시니어와 연결 해제 상태를 URL에 동기화한다', async () => {
    const selectedWardId = ref<number | null>(null)
    selection.syncSelectedWard(selectedWardId)
    selectedWardId.value = 14
    await flushPromises()

    expect(router.currentRoute.value.query.wardId).toBe('14')
    expect(guardStore.activeWardId).toBe(14)

    await selection.clearWardSelection()

    expect(router.currentRoute.value.query).toEqual({ type: 'TRANSFER' })
    expect(guardStore.activeWardId).toBeNull()
  })

  it('유효하지 않은 시니어 ID는 선택 상태를 변경하지 않는다', async () => {
    await expect(selection.selectWard('ward')).resolves.toBe(false)

    expect(router.currentRoute.value.query.wardId).toBe('12')
    expect(guardStore.activeWardId).toBe(12)
  })
})
