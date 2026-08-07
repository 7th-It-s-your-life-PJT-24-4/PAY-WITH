import { onBeforeUnmount, onMounted, type Ref } from 'vue'

export function useEnsureFocusedInputVisible(
  inputElement: Ref<HTMLElement | null>,
) {
  let animationFrame: number | null = null

  function ensureInputVisible() {
    if (animationFrame !== null) cancelAnimationFrame(animationFrame)

    animationFrame = requestAnimationFrame(() => {
      inputElement.value?.scrollIntoView({
        behavior: 'auto',
        block: 'center',
        inline: 'nearest',
      })
      animationFrame = null
    })
  }

  function handleViewportResize() {
    if (document.activeElement === inputElement.value) ensureInputVisible()
  }

  onMounted(() => {
    window.visualViewport?.addEventListener('resize', handleViewportResize)
  })

  onBeforeUnmount(() => {
    window.visualViewport?.removeEventListener('resize', handleViewportResize)
    if (animationFrame !== null) cancelAnimationFrame(animationFrame)
  })

  return { ensureInputVisible }
}
