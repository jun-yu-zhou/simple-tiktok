<template>
  <v-card elevation="0">
    <v-list lines="three" class="mr-2 ml-2">
      <template v-for="item in videoList" :key="item.id">
        <v-list-item class="pa-0" max-height="110px" style="overflow: hidden;">
          <template #prepend>
            <v-img width="200" height="100" class="mr-4" :src="item.coverUrl" cover @click="playVideo(item)"></v-img>
            <v-chip class="ma-2" color="error" :variant="'flat'" v-if="item.auditStatus === 2">
              {{ item.msg || '已拒绝' }}
            </v-chip>
            <v-chip
              v-else-if="item.auditStatus === 0"
              style="position: absolute; left: 0; top: 0;"
              class="ma-2"
              color="warning"
              :variant="'flat'"
            >
              审核中
            </v-chip>
            <v-chip
              v-else-if="item.auditStatus === 1"
              style="position: absolute; left: 0; top: 0;"
              class="ma-2"
              color="success"
              :variant="'flat'"
            >
              已通过
            </v-chip>
          </template>

          <v-list-item-title class="font-weight-bold" v-text="item.title"></v-list-item-title>
          <div
            v-if="item.description && item.description !== item.title"
            v-text="item.description"
            style="line-height: 25px; overflow: hidden;"
          ></div>
          <v-chip-group>
            <v-chip v-for="label in (item.labelNames || '').split(',').filter(Boolean)" :key="label">{{ label }}</v-chip>
          </v-chip-group>

          <template #append>
            <v-btn-group :variant="'outlined'">
              <v-btn color="blue" @click="edit(item)">修改</v-btn>
              <v-btn color="red" @click="videoInfo = item; dialog = true">删除</v-btn>
            </v-btn-group>
          </template>
        </v-list-item>
        <v-divider class="ma-2" />
        <div class="mt-2 mb-2"></div>
      </template>
    </v-list>

    <v-pagination v-if="pageInfo.pages > 1" v-model="pageInfo.page" :length="pageInfo.pages"></v-pagination>

    <v-dialog v-model="dialog" persistent width="auto">
      <v-card>
        <v-card-title class="text-h5">确认删除这个视频吗？</v-card-title>
        <v-card-text>标题：{{ videoInfo.title }}</v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green-darken-1" variant="text" @click="removeVideo()">确认</v-btn>
          <v-btn color="green-darken-1" variant="text" @click="dialog = false">取消</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
      <template #actions>
        <v-btn color="blue" variant="text" @click="snackbar.show = false">关闭</v-btn>
      </template>
    </v-snackbar>

    <v-dialog :model-value="videoDialog" fullscreen transition="dialog-bottom-transition">
      <v-card v-if="currentVideo">
        <Video
          :video-info="currentVideo"
          :close-video="() => playVideo(null)"
          :simple-mode="true"
          :hide-author="true"
          :stats-only-actions="true"
        />
      </v-card>
    </v-dialog>

    <v-dialog v-model="editDialog" max-width="1120" class="video-edit-dialog">
      <v-card class="video-edit-shell">
        <v-card-title class="video-edit-title">
          <div class="d-flex align-center ga-2">
            <v-icon color="cyan-lighten-2">mdi-file-edit-outline</v-icon>
            <span>修改作品</span>
          </div>
          <v-spacer />
          <v-btn icon="mdi-close" size="small" variant="text" @click="clear" />
        </v-card-title>
        <v-card-subtitle class="video-edit-subtitle">仅可修改分类与标签，视频内容、封面、文案保持不变</v-card-subtitle>
        <v-divider />
        <v-card-text class="video-edit-body">
          <VideoEdit :current-video="videoInfo" :clear="clear" :save="saveEdit" />
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script setup>
import { ref, watch } from 'vue';
import { apiGetFilePublicUrl } from '../../../apis/file';
import { apiRemoveVideo } from '../../../apis/user/videoManger';
import { apiGetVideoByUser } from '../../../apis/video';
import Video from '../../../components/video/index.vue';
import VideoEdit from './edit.vue';

const pageInfo = ref({
  page: 1,
  pages: 1,
  limit: 10
});
const videoList = ref([]);
const dialog = ref(false);
const editDialog = ref(false);
const videoInfo = ref({});

const snackbar = ref({
  show: false,
  text: ''
});

let latestRequestId = 0;
const getVideo = async () => {
  const requestId = ++latestRequestId;
  const { data } = await apiGetVideoByUser(pageInfo.value.page, pageInfo.value.limit);
  if (requestId !== latestRequestId || !data.state) {
    return;
  }
  const payload = data.data || {};
  const sourceRecords = payload.records || [];
  const records = await Promise.all(sourceRecords.map(async item => {
    let coverUrl = '';
    if (item.cover) {
      try {
        coverUrl = await apiGetFilePublicUrl(item.cover);
      } catch (_e) {
        coverUrl = '';
      }
    }
    return {
      ...item,
      coverUrl
    };
  }));
  if (requestId !== latestRequestId) {
    return;
  }
  videoList.value = records;
  const total = Number(payload.total || records.length || 0);
  pageInfo.value.pages = Math.max(1, Math.ceil(total / Math.max(pageInfo.value.limit, 1)));
};

const edit = info => {
  const temp = Object.assign({}, info);
  if (Array.isArray(temp.labelNames)) {
    temp.labelNames = temp.labelNames;
  } else {
    temp.labelNames = String(temp.labelNames || '').split(',').filter(Boolean);
  }
  videoInfo.value = temp;
  editDialog.value = !!info;
};

const removeVideo = () => {
  apiRemoveVideo(videoInfo.value.id).then(({ data }) => {
    snackbar.value = {
      text: data.message,
      show: true,
      color: data.state ? 'success' : 'error'
    };
    dialog.value = false;
    videoInfo.value = {};
    if (data.state) {
      getVideo();
    }
  });
};

const saveEdit = data => {
  snackbar.value = {
    text: data.message,
    show: true
  };
  if (data.state) {
    clear();
    getVideo();
  }
};

const clear = () => {
  videoInfo.value = {};
  editDialog.value = false;
};

const currentVideo = ref(null);
const videoDialog = ref(false);
const playVideo = video => {
  videoDialog.value = false;
  currentVideo.value = video;
  videoDialog.value = !!video;
};

watch(
  () => pageInfo.value.page,
  () => {
    getVideo();
  },
  { immediate: true }
);
</script>

<style scoped>
.video-edit-shell {
  border: 1px solid rgba(112, 230, 255, 0.18);
  border-radius: 14px;
  background:
    radial-gradient(1200px 360px at -10% -40%, rgba(31, 173, 255, 0.12), transparent 55%),
    radial-gradient(900px 320px at 110% -20%, rgba(255, 110, 77, 0.12), transparent 58%),
    #202631;
}

.video-edit-title {
  padding: 18px 20px 6px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.video-edit-subtitle {
  padding: 0 20px 14px;
  color: rgba(255, 255, 255, 0.7);
}

.video-edit-body {
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  padding: 16px 20px 20px;
}

@media (max-width: 960px) {
  .video-edit-body {
    max-height: calc(100vh - 120px);
    padding: 12px;
  }
}
</style>
