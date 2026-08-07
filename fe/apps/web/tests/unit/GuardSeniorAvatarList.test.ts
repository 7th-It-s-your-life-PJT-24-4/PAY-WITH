import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'

describe('GuardSeniorAvatarList', () => {
  it('다른 시니어의 이상 거래만 오류 색상으로 표시하고 현재 시니어는 선택 색상을 유지한다', () => {
    const wrapper = mount(GuardSeniorAvatarList, {
      props: {
        seniors: [
          { id: '12', name: '김시니어', hasPending: true },
          { id: '13', name: '이시니어', hasPending: true },
        ],
        activeSeniorId: '13',
      },
    })

    const pendingSenior = wrapper.get(
      'button[aria-label="김시니어 이상 거래 있음"]',
    )
    const activeSenior = wrapper.get(
      'button[aria-label="이시니어 이상 거래 있음"]',
    )

    expect(pendingSenior.get('span').classes()).toContain('border-error')
    expect(activeSenior.get('span').classes()).toContain('border-primary-500')
    expect(activeSenior.get('span').classes()).not.toContain('border-error')
  })
})
