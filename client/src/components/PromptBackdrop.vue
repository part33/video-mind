<template>
  <div class="prompt-wall" aria-hidden="true">
    <div
      v-for="(column, columnIndex) in columns"
      :key="`column-${columnIndex}`"
      class="prompt-wall__column"
      :class="`prompt-wall__column--${columnIndex + 1}`"
    >
      <p
        v-for="(line, lineIndex) in column"
        :key="`line-${columnIndex}-${lineIndex}`"
        class="prompt-wall__line"
      >
        {{ line }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  lines: {
    type: Array,
    default: () => [],
  },
})

const columns = computed(() => {
  const source = props.lines.filter(Boolean)
  const length = source.length || 1

  return Array.from({ length: 4 }, (_, columnIndex) =>
    Array.from({ length: 14 }, (_, lineIndex) => source[(columnIndex * 4 + lineIndex) % length])
  )
})
</script>

<style scoped>
.prompt-wall {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.prompt-wall__column {
  position: absolute;
  top: -12%;
  width: min(26vw, 360px);
  display: grid;
  gap: 18px;
  opacity: 0.16;
  color: rgba(237, 240, 244, 0.24);
  font-family: var(--font-display);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  line-height: 1.1;
  will-change: transform, opacity;
}

.prompt-wall__column--1 {
  left: -2%;
  transform: rotate(-14deg);
}

.prompt-wall__column--2 {
  left: 22%;
  top: 4%;
  transform: rotate(-10deg);
}

.prompt-wall__column--3 {
  right: 18%;
  top: -8%;
  transform: rotate(-8deg);
}

.prompt-wall__column--4 {
  right: -2%;
  top: 8%;
  transform: rotate(-12deg);
}

.prompt-wall__line {
  margin: 0;
  font-size: clamp(0.7rem, 1vw, 0.92rem);
  white-space: nowrap;
  text-shadow: 0 0 22px rgba(255, 255, 255, 0.05);
}

@media (max-width: 1080px) {
  .prompt-wall__column {
    width: 42vw;
    gap: 14px;
    opacity: 0.12;
  }

  .prompt-wall__column--3,
  .prompt-wall__column--4 {
    display: none;
  }
}

@media (max-width: 760px) {
  .prompt-wall__column {
    width: 80vw;
    opacity: 0.09;
  }

  .prompt-wall__column--2 {
    display: none;
  }
}
</style>
