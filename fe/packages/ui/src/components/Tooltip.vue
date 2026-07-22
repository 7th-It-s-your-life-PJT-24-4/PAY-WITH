<script setup lang="ts">
import {
  TooltipArrow,
  TooltipContent,
  TooltipProvider,
  TooltipRoot,
  TooltipTrigger,
} from 'reka-ui'

defineProps<{
  content: string
}>()
</script>

<template>
  <TooltipProvider>
    <TooltipRoot>
      <TooltipTrigger as-child>
        <slot />
      </TooltipTrigger>
      <TooltipContent
        class="z-50 rounded-md bg-slate-950 px-2.5 py-1.5 text-xs font-medium text-white shadow-lg"
        :side-offset="8"
        side="top"
      >
        {{ content }}
        <TooltipArrow class="fill-slate-950" :height="5" :width="10" />
      </TooltipContent>
    </TooltipRoot>
  </TooltipProvider>
</template>
