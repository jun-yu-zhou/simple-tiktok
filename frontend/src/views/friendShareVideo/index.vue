<template>
  <v-card style="height: 100%;" elevation="2" color="background">
    <Video
      v-if="videoList.length > 0"
      :next-video="nextVideo"
      :hide-close="true"
      :video-list="videoList"
    />
    <v-card v-else class="mx-auto mt-15" elevation="5" max-width="500">
      <div class="py-12 text-center">
        <v-icon class="mb-6" icon="mdi-account-multiple-outline" size="128" />
        <div class="text-h4 font-weight-bold">还没有收到好友分享视频</div>
      </div>

      <v-divider />

      <div class="pa-4 text-end">
        <v-btn
          class="text-none"
          color="medium-emphasis"
          min-width="92"
          rounded
          variant="outlined"
          to="/pushVideo"
        >
          去看推荐
        </v-btn>
      </div>
    </v-card>
  </v-card>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { apiGetFriendShareVideo } from '../../apis/video.js';
import Video from '../../components/video/index.vue';

const videoList = ref([]);

const nextVideo = index => {
  if ((videoList.value.length - index) <= 3) {
    getVideo();
  }
};

const resolveCursorTime = video => {
  const created = Number(video?.gmtCreated);
  if (Number.isFinite(created) && created > 0) {
    return created;
  }
  return null;
};

const getVideo = () => {
  let time = null;
  if (videoList.value.length > 0) {
    time = resolveCursorTime(videoList.value[videoList.value.length - 1]);
  }
  apiGetFriendShareVideo(time).then(({ data }) => {
    if (!data.state) {
      return;
    }
    videoList.value = videoList.value.concat(data.data);
  });
};

onMounted(() => {
  getVideo();
});
</script>
