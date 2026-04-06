<template>
  <v-card class="upload-page" elevation="0">
    <v-card class="edit-panel" variant="outlined">
      <v-card-title class="d-flex align-center ga-3">
        <v-icon color="info">mdi-movie-open-plus</v-icon>
        发布视频
        <v-spacer></v-spacer>
        <v-chip class="font-weight-bold" color="success" label>
          <v-icon start icon="mdi-timer-sand"></v-icon>
          审核队列: {{ queueState }}
        </v-chip>
      </v-card-title>
      <v-card-text>
        <VideoEdit :current-video="currentVideo" :clear="clearUp" :save="pushVideo" :set-loading="setBlocking" />
      </v-card-text>
    </v-card>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
      <template v-slot:actions>
        <v-btn color="blue" variant="text" @click="snackbar.show = false">知道了</v-btn>
      </template>
    </v-snackbar>

    <v-overlay :model-value="blocking" persistent class="align-center justify-center">
      <div class="d-flex flex-column align-center ga-3">
        <v-progress-circular indeterminate color="info" :size="64" :width="6"></v-progress-circular>
        <div class="text-body-1">正在上传，请稍候...</div>
      </div>
    </v-overlay>
  </v-card>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { apiGetAuditQueueState } from '../../../apis/video';
import VideoEdit from './edit.vue';

const snackbar = ref({
  show: false,
  text: ''
});

const queueState = ref('空闲');
const blocking = ref(false);

const emptyVideo = () => ({
  caption: '',
  url: '',
  videoPreviewUrl: '',
  cover: '',
  coverPreviewUrl: '',
  labelNames: [],
  typeId: null,
  open: 1,
  duration: ''
});

const currentVideo = ref(emptyVideo());

onMounted(() => {
  getQueueState();
});

const getQueueState = () => {
  apiGetAuditQueueState().then(({ data }) => {
    if (!data.state) {
      return;
    }
    queueState.value = data.message;
  });
};

const clearUp = () => {
  currentVideo.value = emptyVideo();
};

const pushVideo = data => {
  snackbar.value = {
    text: data.message,
    show: true,
    color: data.state ? 'success' : 'error'
  };
  if (data.state) {
    clearUp();
    getQueueState();
  }
};

const setBlocking = value => {
  blocking.value = !!value;
};
</script>

<style scoped>
.upload-page {
  background: radial-gradient(1000px 300px at 10% -5%, rgba(27, 78, 150, 0.28), transparent 45%), #111318;
  border-radius: 16px;
  padding: 12px;
}

.edit-panel {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.12) !important;
}
</style>
