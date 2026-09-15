<script setup>
import { computed } from 'vue'
import { parseHighlight } from '../utils/highlight.js'

const props = defineProps({ text: { type: String, default: '' } })
const parts = computed(() => parseHighlight(props.text))
</script>

<template><span><template v-for="(part, index) in parts" :key="index"><mark v-if="part.highlight">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></span></template>
