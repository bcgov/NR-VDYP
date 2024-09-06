<template>
  <div>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface DialogDrag {
  el: HTMLElement | null
  mouseStartX: number | null
  mouseStartY: number | null
  elStartX: number | null
  elStartY: number | null
  oldTransition: string | null
}

const dialogDrag = ref<DialogDrag>({
  el: null,
  mouseStartX: null,
  mouseStartY: null,
  elStartX: null,
  elStartY: null,
  oldTransition: null,
})

const mousedownHandler = (e: MouseEvent) => {
  const closestDialog = (e.target as HTMLElement).closest('.v-overlay--active .v-overlay__content')
  if (e.button === 0 && closestDialog !== null && (e.target as HTMLElement).classList.contains('popup-header')) {
    dialogDrag.value.el = closestDialog as HTMLElement
    dialogDrag.value.mouseStartX = e.clientX
    dialogDrag.value.mouseStartY = e.clientY
    dialogDrag.value.elStartX = dialogDrag.value.el.getBoundingClientRect().left
    dialogDrag.value.elStartY = dialogDrag.value.el.getBoundingClientRect().top
    dialogDrag.value.el.style.position = 'fixed'
    dialogDrag.value.el.style.margin = '0'
    dialogDrag.value.oldTransition = dialogDrag.value.el.style.transition
    dialogDrag.value.el.style.transition = 'none'
  }
}

const mousemoveHandler = (e: MouseEvent) => {
  if (!dialogDrag.value.el) return
  dialogDrag.value.el.style.left =
    Math.min(Math.max(dialogDrag.value.elStartX! + e.clientX - dialogDrag.value.mouseStartX!, 0), window.innerWidth - dialogDrag.value.el.getBoundingClientRect().width) + 'px'
  dialogDrag.value.el.style.top =
    Math.min(Math.max(dialogDrag.value.elStartY! + e.clientY - dialogDrag.value.mouseStartY!, 0), window.innerHeight - dialogDrag.value.el.getBoundingClientRect().height) + 'px'
}

const mouseupHandler = () => {
  if (!dialogDrag.value.el) return
  dialogDrag.value.el.style.transition = dialogDrag.value.oldTransition!
  dialogDrag.value.el = null
}

onMounted(() => {
  document.addEventListener('mousedown', mousedownHandler)
  document.addEventListener('mousemove', mousemoveHandler)
  document.addEventListener('mouseup', mouseupHandler)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', mousedownHandler)
  document.removeEventListener('mousemove', mousemoveHandler)
  document.removeEventListener('mouseup', mouseupHandler)
})
</script>

<style>
.v-overlay--active .popup-header {
  cursor: grab;
}
.v-overlay--active .popup-header:active {
  cursor: move;
}
</style>
