import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'

describe('GuardSeniorAvatarList', () => {
  it('이상 거래가 있는 시니어의 아바타에 오류 색상 테두리를 표시한다', () => {
    const wrapper = mount(GuardSeniorAvatarList, {
      props: {
        seniors: [
          { id: '12', name: '김시니어', hasPending: true },
          { id: '13', name: '이시니어' },
        ],
        activeSeniorId: '13',
      },
    })

    const pendingSenior = wrapper.get(
      'button[aria-label="김시니어 이상 거래 있음"]',
    )
    const activeSenior = wrapper.get('button[aria-label="이시니어"]')

    expect(pendingSenior.get('span').classes()).toContain('border-error')
    expect(activeSenior.get('span').classes()).toContain('border-primary-500')
  })
})
