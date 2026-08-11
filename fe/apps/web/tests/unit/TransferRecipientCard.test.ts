import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TransferRecipientCard from '@/pages/ward/transfer/-components/TransferRecipientCard.vue'
import type { TransferRecipient } from '@/stores/transfer.store'

const mockSafeRecipient: TransferRecipient = {
  id: 1,
  name: '민수 형',
  holderName: '김민수',
  bankCode: '004',
  bank: 'KB국민은행',
  accountNumber: '43210201234567',
  isContact: true,
}

const mockRecentRecipient: TransferRecipient = {
  id: 2,
  name: '박지연',
  holderName: '박지연',
  bankCode: '088',
  bank: '신한은행',
  accountNumber: '110234567890',
  isContact: false,
}

describe('TransferRecipientCard', () => {
  it('안심계좌 수취인 정보와 안심 뱃지를 정상 렌더링한다', () => {
    const wrapper = mount(TransferRecipientCard, {
      props: {
        recipient: mockSafeRecipient,
        showAddContact: false,
      },
    })

    expect(wrapper.text()).toContain('민수 형')
    expect(wrapper.text()).toContain('KB국민은행 43210201234567')
    expect(wrapper.text()).toContain('안심')
    expect(wrapper.find('button[aria-label*="안심계좌 추가"]').exists()).toBe(
      false,
    )
  })

  it('미등록 최근 수취인일 때 안심계좌 추가 버튼을 렌더링하고 클릭 시 addContact 이벤트를 발생시킨다', async () => {
    const wrapper = mount(TransferRecipientCard, {
      props: {
        recipient: mockRecentRecipient,
        showAddContact: true,
      },
    })

    expect(wrapper.text()).toContain('박지연')
    expect(wrapper.text()).toContain('신한은행 110234567890')
    const addBtn = wrapper.find('button[aria-label="박지연 안심계좌 추가"]')
    expect(addBtn.exists()).toBe(true)

    await addBtn.trigger('click')
    expect(wrapper.emitted('addContact')).toHaveLength(1)
    expect(wrapper.emitted('addContact')?.[0]).toEqual([mockRecentRecipient])
    expect(wrapper.emitted('select')).toBeUndefined()
  })

  it('카드 본체를 클릭하면 select 이벤트를 발생시킨다', async () => {
    const wrapper = mount(TransferRecipientCard, {
      props: {
        recipient: mockSafeRecipient,
        showAddContact: false,
      },
    })

    const selectBtn = wrapper.find('button[type="button"]')
    await selectBtn.trigger('click')
    expect(wrapper.emitted('select')).toHaveLength(1)
    expect(wrapper.emitted('select')?.[0]).toEqual([mockSafeRecipient])
  })
})
