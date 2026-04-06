<template>
  <div>
    <v-card-title style="display: inline;">全部分类</v-card-title>
    <v-card-subtitle style="display: inline;">可订阅的视频分类</v-card-subtitle>
    <v-divider />
    <v-chip-group class="ma-2">
      <v-chip
        v-for="(item, index) in classifyData"
        :key="index"
        :size="'large'"
        append-icon="mdi-plus-circle"
        :disabled="disabled"
        @click="closeEvent(item)"
      >
        <template #prepend>
          <v-avatar :image="item.image" :icon="item.icon || 'mdi-file-document-alert-outline'" start></v-avatar>
        </template>
        {{ item.name }}
      </v-chip>
    </v-chip-group>
  </div>
</template>

<script setup>
defineProps({
  classifyData: {
    type: Array,
    default: () => []
  },
  closeEvent: {
    type: Function,
    default: () => {}
  },
  disabled: {
    type: Boolean,
    default: false
  }
});
</script>