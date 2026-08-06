import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { mockGuardian } from '@/mocks/guardian.mock'
import WardMyGuardianPage from '@/pages/ward/my/guardian/page.vue'
import WardMyPage from '@/pages/ward/my/page.vue'
import WardMyProfilePage from '@/pages/ward/my/profile/page.vue'

const routerPush = vi.fn()
const routerReplace = vi.fn()
const clearQueryClient = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
    replace: routerReplace,
  }),
}))

vi.mock('@tanstack/vue-query', () => ({
  useQueryClient: () => ({
    clear: clearQueryClient,
  }),
}))

vi.mock('@/api/auth-session', () => ({
  clearAuthenticationSession: vi.fn(),
}))

vi.mock('@/api/token-storage', () => ({
  getUserIdFromAccessToken: () => 7,
  tokenStorage: {
    getAccessToken: () => 'access-token',
  },
}))

vi.mock('@/composables/useUserQuery', () => ({
  useUserQuery: () => ({
    data: {
      value: {
        id: 7,
        name: '김수이',
        phone: '01012345678',
        role: 'WARD',
        avatarId: 2,
        createdAt: '2026-08-06T00:00:00',
        updatedAt: '2026-08-06T00:00:00',
      },
    },
    isPending: { value: false },
    isError: { value: false },
  }),
}))

const wrappers: VueWrapper[] = []

afterEach(() => {
  wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

function mountPage(component: Parameters<typeof mount>[0]) {
  const wrapper = mount(component, { attachTo: document.body })
  wrappers.push(wrapper)
  return wrapper
}

function getButtonByName(wrapper: VueWrapper, name: string) {
  const button = wrapper
    .findAll('button')
    .find((candidate) => candidate.text().trim() === name)

  if (!button) throw new Error(`${name} 버튼을 찾을 수 없습니다.`)

  return button
}

describe('피보호자 마이페이지', () => {
  it('보호자 관리 메뉴에서 보호자 관리 화면으로 이동한다', async () => {
    const wrapper = mountPage(WardMyPage)

    await getButtonByName(wrapper, '보호자 관리').trigger('click')

    expect(routerPush).toHaveBeenCalledWith({ name: 'ward-my-guardian' })
  })

  it('내 정보에 로그인 사용자의 이름과 전화번호를 표시한다', () => {
    const wrapper = mountPage(WardMyProfilePage)

    expect(wrapper.text()).toContain('김수이')
    expect(wrapper.text()).toContain('010-1234-5678')
  })

  it('보호자 관리 화면에 현재 보호자 정보와 해제 불가 안내를 표시한다', async () => {
    const wrapper = mountPage(WardMyGuardianPage)

    expect(wrapper.text()).toContain(`${mockGuardian.name}님`)
    expect(wrapper.text()).toContain(mockGuardian.phoneNumber)

    await getButtonByName(wrapper, '연결 해제').trigger('click')
    await nextTick()

    expect(document.body.textContent).toContain('해제 불가')
    expect(document.body.textContent).toContain('보호자만 해제할 수 있습니다.')
  })
})
