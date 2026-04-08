<template>
  <div>
    <v-row>
      <v-col cols="12" md="8">
        <v-card class="preview-card mb-4" variant="outlined" @click="openVideoUpload">
          <v-card-title class="d-flex align-center ga-2">
            <v-icon color="info">mdi-play-box-multiple</v-icon>
            上传视频文件（支持点击卡片重新选择）
            <v-spacer></v-spacer>
            <v-btn size="small" variant="tonal" color="info" @click.stop="togglePreviewPlay">
              {{ previewPlayable ? '关闭播放控制' : '仅预览首帧' }}
            </v-btn>
          </v-card-title>
          <v-card-text>
            <div class="video-stage">
              <video
                v-if="previewVideoUrl"
                ref="videoPreviewRef"
                :src="previewVideoUrl"
                :poster="coverImg"
                preload="metadata"
                muted
                playsinline
                disablePictureInPicture
                controlsList="nodownload noplaybackrate noremoteplayback nofullscreen"
                :controls="previewPlayable"
                class="video-preview"
                :class="{ 'is-playable': previewPlayable }"
                @loadedmetadata="handleVideoLoaded"
              ></video>
              <div v-else class="upload-placeholder">
                <v-icon size="42" color="info">mdi-upload</v-icon>
                <div class="mt-2">点击上传视频，建议时长 5 秒以上，清晰度不低于 720P</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
        <input ref="videoFileRef" hidden type="file" accept="video/*" @change="uploadVideo" />
      </v-col>

      <v-col cols="12" md="4">
        <v-card class="preview-card mb-4" variant="outlined" @click="openCoverUpload">
          <v-card-title class="d-flex align-center ga-2">
            <v-icon color="deep-orange-lighten-1">mdi-image-outline</v-icon>
            上传封面图
          </v-card-title>
          <v-card-text>
            <div class="cover-stage">
              <v-img v-if="coverImg" :key="media.cover" class="cover-preview" :src="coverImg" contain />
              <div v-else class="upload-placeholder">
                <v-icon size="42" color="deep-orange-lighten-1">mdi-upload</v-icon>
                <div class="mt-2">点击上传封面，建议比例 9:16 或 16:9</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
        <input ref="coverFileRef" hidden type="file" accept="image/*" @change="uploadCover" />
      </v-col>
    </v-row>

    <v-text-field variant="filled" label="视频文案" v-model="media.caption" clearable></v-text-field>

    <v-autocomplete
      v-model="media.typeId"
      :items="allClassifyList"
      chips
      closable-chips
      color="blue-grey-lighten-2"
      item-title="name"
      item-value="id"
      label="视频分类"
      no-data-text="暂无分类，请先在后台配置"
    >
      <template v-slot:chip="{ props: chipProps, item }">
        <v-chip v-bind="chipProps" :prepend-icon="item.raw.icon || 'mdi-file-document-alert-outline'" :text="item.raw.name"></v-chip>
      </template>

      <template v-slot:item="{ props: itemProps, item }">
        <v-list-item
          v-bind="itemProps"
          :prepend-icon="item?.raw?.icon || 'mdi-file-document-alert-outline'"
          :title="item?.raw?.name"
          :subtitle="item?.raw?.description || '暂无分类描述'"
        ></v-list-item>
      </template>
    </v-autocomplete>

    <v-combobox
      v-model="media.labelNames"
      label="视频标签"
      multiple
      chips
      closable-chips
      hint="输入后按回车添加标签，可添加多个；标签会用于推荐和搜索。"
      persistent-hint
    ></v-combobox>

    <v-divider></v-divider>

    <v-card-actions>
      <v-btn color="warning" class="font-weight-bold" variant="tonal" @click="clearUp">取消</v-btn>
      <v-spacer></v-spacer>
      <v-btn color="success" class="font-weight-bold" variant="tonal" @click="pushVideo">{{ submitButtonText }}</v-btn>
    </v-card-actions>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
      <template v-slot:actions>
        <v-btn color="blue" variant="text" @click="snackbar.show = false">知道了</v-btn>
      </template>
    </v-snackbar>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, toRef, watch } from 'vue';
import { apiClassifyGetAll } from '../../../apis/classify';
import { apiFileGet, apiUploadFile } from '../../../apis/file';
import { apiVideoPush } from '../../../apis/user/videoManger';

const props = defineProps({
  currentVideo: {
    type: Object,
    default: () => ({})
  },
  clear: {
    type: Function,
    default: () => {}
  },
  setLoading: {
    type: Function,
    default: () => {}
  },
  save: {
    type: Function,
    default: () => {}
  }
});

const media = toRef(props, 'currentVideo');
const videoFileRef = ref(null);
const coverFileRef = ref(null);
const videoPreviewRef = ref(null);
const MAX_VIDEO_DURATION_SECONDS = 20 * 60;
const selectedVideoDurationSeconds = ref(null);
const uploadingVideo = ref(false);
const uploadingCover = ref(false);
const previewPlayable = ref(false);
const localVideoPreviewUrl = ref('');
const localCoverPreviewUrl = ref('');
const allClassifyList = ref([]);
const snackbar = ref({
  show: false,
  text: ''
});

const openVideoUpload = () => {
  if (!uploadingVideo.value) {
    videoFileRef.value?.click();
  }
};

const openCoverUpload = () => {
  if (!uploadingCover.value) {
    coverFileRef.value?.click();
  }
};

const togglePreviewPlay = () => {
  previewPlayable.value = !previewPlayable.value;
};

const resetToFirstFrame = () => {
  const video = videoPreviewRef.value;
  if (!video) {
    return;
  }
  video.pause();
  try {
    video.currentTime = 0.01;
  } catch (e) {
    // ignore seek errors during source switching
  }
};

const handleVideoLoaded = () => {
  const duration = Number(videoPreviewRef.value?.duration);
  if (Number.isFinite(duration) && duration > 0) {
    selectedVideoDurationSeconds.value = duration;
  }
  if (!previewPlayable.value) {
    resetToFirstFrame();
  }
};

const cleanupObjectUrl = url => {
  if (url) {
    URL.revokeObjectURL(url);
  }
};

const resetLocalPreviewState = () => {
  cleanupObjectUrl(localVideoPreviewUrl.value);
  cleanupObjectUrl(localCoverPreviewUrl.value);
  localVideoPreviewUrl.value = '';
  localCoverPreviewUrl.value = '';
  previewPlayable.value = false;
  selectedVideoDurationSeconds.value = null;
  if (videoFileRef.value) {
    videoFileRef.value.value = null;
  }
  if (coverFileRef.value) {
    coverFileRef.value.value = null;
  }
};

const readVideoDuration = file =>
  new Promise((resolve, reject) => {
    const tempUrl = URL.createObjectURL(file);
    const tempVideo = document.createElement('video');
    tempVideo.preload = 'metadata';

    const cleanup = () => {
      tempVideo.removeAttribute('src');
      tempVideo.load();
      URL.revokeObjectURL(tempUrl);
    };

    tempVideo.onloadedmetadata = () => {
      const duration = Number(tempVideo.duration);
      cleanup();
      resolve(duration);
    };

    tempVideo.onerror = () => {
      cleanup();
      reject(new Error('无法读取视频时长'));
    };

    tempVideo.src = tempUrl;
  });

const validateVideoDurationBeforeUpload = async file => {
  try {
    const duration = await readVideoDuration(file);
    if (!Number.isFinite(duration) || duration <= 0) {
      snackbar.value = {
        text: '无法识别视频时长，请更换文件后重试',
        show: true,
        color: 'error'
      };
      return false;
    }
    if (duration > MAX_VIDEO_DURATION_SECONDS) {
      snackbar.value = {
        text: '视频时长超过20分钟，无法上传',
        show: true,
        color: 'error'
      };
      return false;
    }
    selectedVideoDurationSeconds.value = duration;
    return true;
  } catch (e) {
    snackbar.value = {
      text: '读取视频时长失败，请更换文件后重试',
      show: true,
      color: 'error'
    };
    return false;
  }
};

const hasExceededDurationLimit = () => {
  const previewDuration = Number(videoPreviewRef.value?.duration);
  const duration = Number.isFinite(previewDuration) && previewDuration > 0
    ? previewDuration
    : Number(selectedVideoDurationSeconds.value);
  return Number.isFinite(duration) && duration > MAX_VIDEO_DURATION_SECONDS;
};

const uploadVideo = async () => {
  if (!videoFileRef.value?.files?.[0]) return;
  const file = videoFileRef.value.files[0];
  const pass = await validateVideoDurationBeforeUpload(file);
  if (!pass) {
    videoFileRef.value.value = null;
    return;
  }

  cleanupObjectUrl(localVideoPreviewUrl.value);
  localVideoPreviewUrl.value = URL.createObjectURL(file);
  previewPlayable.value = false;

  uploadingVideo.value = true;
  props.setLoading(true);
  apiUploadFile(file, {
    next: () => {},
    error: () => {
      uploadingVideo.value = false;
      props.setLoading(false);
      snackbar.value = {
        text: '视频上传失败，请稍后重试',
        show: true,
        color: 'error'
      };
    },
    complete: (e, fileId) => {
      uploadingVideo.value = false;
      props.setLoading(false);
      if (!fileId.state) {
        snackbar.value = {
          text: fileId.message,
          show: true,
          color: 'error'
        };
        return;
      }
      media.value.url = fileId.data;
      media.value.videoPreviewUrl = e?.url || '';
      snackbar.value = {
        text: '视频上传成功',
        show: true,
        color: 'success'
      };
      videoFileRef.value.value = null;
    }
  });
};

const uploadCover = () => {
  if (!coverFileRef.value?.files?.[0]) return;
  const file = coverFileRef.value.files[0];
  cleanupObjectUrl(localCoverPreviewUrl.value);
  localCoverPreviewUrl.value = URL.createObjectURL(file);

  uploadingCover.value = true;
  props.setLoading(true);
  apiUploadFile(file, {
    next: () => {},
    error: () => {
      uploadingCover.value = false;
      props.setLoading(false);
      snackbar.value = {
        text: '封面上传失败，请稍后重试',
        show: true,
        color: 'error'
      };
    },
    complete: (e, fileId) => {
      uploadingCover.value = false;
      props.setLoading(false);
      if (!fileId.state) {
        snackbar.value = {
          text: fileId.message,
          show: true,
          color: 'error'
        };
        return;
      }
      media.value.cover = fileId.data;
      media.value.coverPreviewUrl = e?.url || '';
      snackbar.value = {
        text: '封面上传成功',
        show: true,
        color: 'success'
      };
      coverFileRef.value.value = null;
    }
  });
};

const clearUp = () => {
  resetLocalPreviewState();
  props.clear();
};

const previewVideoUrl = computed(() => {
  if (localVideoPreviewUrl.value) {
    return localVideoPreviewUrl.value;
  }
  if (media.value?.videoPreviewUrl) {
    return media.value.videoPreviewUrl;
  }
  if (!media.value?.url) {
    return '';
  }
  return apiFileGet(media.value.url);
});

watch(previewPlayable, value => {
  if (!value) {
    resetToFirstFrame();
  }
});

watch(previewVideoUrl, async () => {
  await nextTick();
  if (!previewPlayable.value) {
    resetToFirstFrame();
  }
});

watch(
  () => [media.value?.url, media.value?.cover, media.value?.videoPreviewUrl, media.value?.coverPreviewUrl],
  values => {
    const noMedia = values.every(v => !v);
    // 父组件发布成功后会重置 currentVideo；这里同步清理本地预览缓存，避免视频/封面回显残留。
    if (noMedia && (localVideoPreviewUrl.value || localCoverPreviewUrl.value)) {
      resetLocalPreviewState();
    }
  }
);

const coverImg = computed(() => {
  if (localCoverPreviewUrl.value) {
    return localCoverPreviewUrl.value;
  }
  if (media.value?.coverPreviewUrl) {
    return media.value.coverPreviewUrl;
  }
  if (!media.value?.cover) {
    return '';
  }
  return apiFileGet(media.value.cover);
});

const submitButtonText = computed(() => (media.value?.id ? '保存修改' : '发布视频'));

onMounted(() => {
  apiClassifyGetAll().then(({ data }) => {
    if (!data.state) {
      allClassifyList.value = [];
      return;
    }
    allClassifyList.value = data.data;
  });
});

onBeforeUnmount(() => {
  resetLocalPreviewState();
});

const pushVideo = () => {
  if (hasExceededDurationLimit()) {
    snackbar.value = {
      text: '视频时长超过20分钟，无法发布或保存修改',
      show: true,
      color: 'error'
    };
    return;
  }
  apiVideoPush(media.value).then(({ data }) => {
    props.save(data);
  });
};
</script>

<style scoped>
.preview-card {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.video-stage {
  width: 100%;
  height: 460px;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
  position: relative;
}

.video-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
  pointer-events: none;
}

.video-preview.is-playable {
  pointer-events: auto;
}

.cover-stage {
  width: 100%;
  height: 460px;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
  position: relative;
}

.cover-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.72);
  cursor: pointer;
}
</style>
