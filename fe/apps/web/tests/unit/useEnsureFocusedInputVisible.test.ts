import { mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'

describe('useEnsureFocusedInputVisible', () => {
  let originalVisualViewport: VisualViewport | undefined

  beforeEach(() => {
    originalVisualViewport = window.visualViewport
  })

  afterEach(() => {
    Object.defineProperty(window, 'visualViewport', {
      configurable: true,
      value: originalVisualViewport,
    })
    vi.restoreAllMocks()
  })

  it('visualViewport resize 이벤트 시 포커스된 인풋을 scrollIntoView 한다', async () => {
    const scrollIntoViewMock = vi.fn()
    let resizeListener: (() => void) | null = null

    const mockViewport = {
      addEventListener: vi.fn((event: string, cb: () => void) => {
        if (event === 'resize') resizeListener = cb
      }),
      removeEventListener: vi.fn(),
    } as unknown as VisualViewport

    Object.defineProperty(window, 'visualViewport', {
      configurable: true,
      value: mockViewport,
    })

    const TestComponent = defineComponent({
      setup() {
        const inputRef = ref<HTMLElement | null>(null)
        const { ensureInputVisible } = useEnsureFocusedInputVisible(inputRef)
        return { inputRef, ensureInputVisible }
      },
      template: '<input ref="inputRef" />',
    })

    const wrapper = mount(TestComponent, { attachTo: document.body })
    const input = wrapper.find('input').element as HTMLInputElement
    input.scrollIntoView = scrollIntoViewMock

    input.focus()
    expect(document.activeElement).toBe(input)

    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      cb(0)
      return 1
    })

    resizeListener?.()

    expect(scrollIntoViewMock).toHaveBeenCalledWith({
      behavior: 'auto',
      block: 'center',
      inline: 'nearest',
    })

    wrapper.unmount()
    expect(mockViewport.removeEventListener).toHaveBeenCalledWith(
      'resize',
      expect.any(Function),
    )
  })
})
