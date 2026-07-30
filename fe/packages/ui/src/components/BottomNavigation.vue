<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'

interface NavigationItem {
  value: string
  label: string
  icon?: Component
}

const props = withDefaults(
  defineProps<{
    items: NavigationItem[]
    active: string
    variant?: 'guardian' | 'ward'
    centerActionValue?: string
    ariaLabel?: string
  }>(),
  {
    variant: 'guardian',
    centerActionValue: undefined,
    ariaLabel: '주요 메뉴',
  },
)

const emit = defineEmits<{
  navigate: [value: string]
}>()

function isCenterAction(value: string) {
  return (
    props.variant === 'ward' &&
    (props.centerActionValue === value ||
      (!props.centerActionValue && value === 'home'))
  )
}

const centerItem = computed(() => {
  if (props.centerActionValue) {
    return props.items.find((item) => item.value === props.centerActionValue)
  }
  return (
    props.items.find((item) => item.value === 'home') ??
    props.items[1] ??
    props.items[0]
  )
})

const leftItem = computed(() => {
  const centerVal = centerItem.value?.value
  return props.items.find((item) => item.value !== centerVal) ?? props.items[0]
})

const rightItem = computed(() => {
  const centerVal = centerItem.value?.value
  const leftVal = leftItem.value?.value
  return (
    props.items.find(
      (item) => item.value !== centerVal && item.value !== leftVal,
    ) ?? props.items[2]
  )
})

function getItemClasses(value: string) {
  if (isCenterAction(value)) {
    return [
      'type-h1 -mt-[45px] m-auto size-[120px] rounded-full',
      'bg-gradient-to-b from-primary-700 to-primary-500 text-on-action',
      'shadow-[0_8px_24px_rgb(8_13_18/18%)]',
      'hover:from-primary-600 hover:to-primary-400',
      'active:from-primary-500 active:to-primary-400',
    ]
  }

  return [
    props.variant === 'ward' ? 'type-h1' : 'type-h4',
    props.active === value ? 'text-primary-300' : 'text-body',
  ]
}
</script>

<template>
  <!-- Ward Variant Navigation -->
  <nav
    v-if="variant === 'ward'"
    class="relative mx-auto flex w-full max-w-[390px] items-end justify-center overflow-x-clip pb-[env(safe-area-inset-bottom)] filter drop-shadow-[0_-8px_20px_rgba(8,13,18,0.12)]"
    :aria-label="ariaLabel"
  >
    <!-- 중앙 홈 원형 버튼 -->
    <button
      v-if="centerItem"
      type="button"
      class="absolute left-1/2 top-[-32px] z-30 flex size-[120px] -translate-x-1/2 flex-col items-center justify-center gap-1 rounded-full border-none bg-gradient-to-b from-[#99E0ED] to-[#00B1D2] text-white text-shadow-lg shadow-[0_6px_20px_rgba(0,177,210,0.35)] transition-all duration-200 hover:scale-105 active:scale-95 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500 motion-reduce:transition-none motion-reduce:hover:scale-100 motion-reduce:active:scale-100"
      :class="[
        active === centerItem.value
          ? 'brightness-110 font-bold'
          : 'hover:brightness-105',
      ]"
      data-center-action="true"
      :aria-current="active === centerItem.value ? 'page' : undefined"
      @click="emit('navigate', centerItem.value)"
    >
      <slot
        name="icon"
        :item="centerItem"
        :active="active === centerItem.value"
      >
        <component
          :is="centerItem.icon"
          v-if="centerItem.icon"
          class="size-12 text-white drop-shadow-lg"
          aria-hidden="true"
        />
      </slot>
      <span class="text-2xl font-semibold leading-none text-white">{{
        centerItem.label
      }}</span>
    </button>

    <!-- 좌우 버튼 트랙 컨테이너 (높이 80px) -->
    <div
      class="relative z-10 flex h-[80px] w-full items-stretch justify-between"
    >
      <!-- 왼쪽 (송금) 버튼 -->
      <button
        v-if="leftItem"
        type="button"
        class="group relative flex flex-1 items-center justify-center border-none outline-none transition-all duration-200 hover:translate-y-[-4px] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500/40 focus-visible:ring-inset motion-reduce:transition-none motion-reduce:hover:translate-y-0"
        :class="[
          active === leftItem.value
            ? 'brightness-105 font-bold'
            : 'opacity-95 hover:opacity-100',
        ]"
        :aria-current="active === leftItem.value ? 'page' : undefined"
        @click="emit('navigate', leftItem.value)"
      >
        <svg
          class="absolute inset-0 -ml-[16px] h-[calc(100%+20px)] w-[calc(100%+16px)] pointer-events-none"
          viewBox="0 0 200 100"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <defs>
            <linearGradient
              id="mint-gradient"
              x1="0%"
              y1="0%"
              x2="100%"
              y2="100%"
            >
              <stop offset="0%" stop-color="#ffffff" />
              <stop offset="100%" stop-color="#ffffff" />
            </linearGradient>
          </defs>
          <path
            d="M200 70C192.203 69.359 184.616 67.1473 177.696 63.4978C170.776 59.8483 164.666 54.8366 159.733 48.7642C154.8 42.6917 150.277 33.0894 144.777 25.5894C139.277 18.0894 130.277 10.0894 122.277 6.58936C114.277 3.08936 99.2769 0 95.2769 0C91.2769 0 20 0 20 0C14.6957 0 9.6086 2.10714 5.85786 5.85786C2.10713 9.60859 0 14.6957 0 20V100H200V70Z"
            fill="url(#mint-gradient)"
          />
        </svg>
        <div
          class="relative z-10 flex flex-row items-center justify-center gap-2 pr-16"
        >
          <slot
            name="icon"
            :item="leftItem"
            :active="active === leftItem.value"
          >
            <component
              :is="leftItem.icon"
              v-if="leftItem.icon"
              class="size-8 text-black"
              aria-hidden="true"
            />
          </slot>
          <span class="text-2xl font-bold leading-none text-black">{{
            leftItem.label
          }}</span>
        </div>
      </button>

      <!-- 오른쪽 (결제) 버튼 -->
      <button
        v-if="rightItem"
        type="button"
        class="group relative flex flex-1 items-center justify-center border-none outline-none transition-all duration-200 hover:translate-y-[-4px] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500/40 focus-visible:ring-inset motion-reduce:transition-none motion-reduce:hover:translate-y-0"
        :class="[
          active === rightItem.value
            ? 'brightness-105 font-bold'
            : 'opacity-95 hover:opacity-100',
        ]"
        :aria-current="active === rightItem.value ? 'page' : undefined"
        @click="emit('navigate', rightItem.value)"
      >
        <svg
          class="absolute inset-0 -mr-[16px] h-[calc(100%+20px)] w-[calc(100%+16px)] pointer-events-none"
          viewBox="0 0 200 100"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <defs>
            <linearGradient
              id="purple-gradient"
              x1="0%"
              y1="0%"
              x2="100%"
              y2="100%"
            >
              <stop offset="0%" stop-color="#ffffff" />
              <stop offset="100%" stop-color="#ffffff" />
            </linearGradient>
          </defs>
          <path
            d="M0 70C7.79709 69.359 15.3839 67.1473 22.304 63.4978C29.224 59.8483 35.3342 54.8366 40.2669 48.7642C45.1995 42.6917 49.7231 33.0894 55.2231 25.5894C60.7231 18.0894 69.7231 10.0894 77.7231 6.58936C85.7231 3.08936 100.723 0 104.723 0C108.723 0 180 0 180 0C185.304 0 190.391 2.10714 194.142 5.85786C197.893 9.60859 200 14.6957 200 20V100H0V70Z"
            fill="url(#purple-gradient)"
          />
        </svg>
        <div
          class="relative z-10 flex flex-row items-center justify-center gap-2 pl-16"
        >
          <slot
            name="icon"
            :item="rightItem"
            :active="active === rightItem.value"
          >
            <component
              :is="rightItem.icon"
              v-if="rightItem.icon"
              class="size-8 text-black"
              aria-hidden="true"
            />
          </slot>
          <span class="text-2xl font-bold leading-none text-black">{{
            rightItem.label
          }}</span>
        </div>
      </button>
    </div>
  </nav>

  <!-- Guardian / Standard Variant Navigation -->
  <nav
    v-else
    class="grid h-bottom-nav w-full border-t border-border bg-surface-card shadow-[0_-2px_5px_rgb(0_0_0/4%)]"
    :style="{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }"
    :aria-label="ariaLabel"
  >
    <button
      v-for="item in items"
      :key="item.value"
      class="relative flex min-h-touch-target flex-col items-center justify-center gap-xxs outline-none transition-colors focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus"
      :class="getItemClasses(item.value)"
      type="button"
      :data-center-action="isCenterAction(item.value) || undefined"
      :aria-current="active === item.value ? 'page' : undefined"
      @click="emit('navigate', item.value)"
    >
      <slot name="icon" :item="item" :active="active === item.value">
        <component
          :is="item.icon"
          v-if="item.icon"
          :class="isCenterAction(item.value) ? 'size-12' : 'size-6'"
          aria-hidden="true"
        />
      </slot>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>
