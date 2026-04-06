<template>
  <v-container style="min-height: 500px;">
    <VideoListVue :video-list="videoList" :showHot="false" />
    <v-dialog :model-value="dialog" fullscreen transition="dialog-bottom-transition">
      <v-card v-if="dialog">
        <Video :video-info="searchVideoInfo" :close-video="closeVideo" />
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { apiGetClassifyByUser } from '../../apis/classify';
import { apiGetVideoById, apiSearchVideo, apiVideoByClassfiy, apiVideoByPush } from '../../apis/video';
import Video from '../../components/video/index.vue';
import VideoListVue from '../../components/video/list.vue';
import router from '../../router/index.js';

const userClassifys = ref([]);
const isLoading = ref(false);
const hasMore = ref(true);
const videoList = ref([]);
const currentClassify = ref(0);
const route = useRoute();
const searchVideoInfo = ref(null);
const pageInfo = ref({
  page: 1,
  limit: 15
});

const dialog = computed(() => !!searchVideoInfo.value);

const resetList = () => {
  pageInfo.value.page = 1;
  videoList.value = [];
  hasMore.value = true;
};

const appendUniqueVideos = list => {
  if (!Array.isArray(list) || list.length === 0) {
    return 0;
  }
  const exists = new Set(videoList.value.map(item => item?.id).filter(Boolean));
  const next = [];
  list.forEach(item => {
    const id = item?.id;
    if (!id || exists.has(id)) {
      return;
    }
    exists.add(id);
    next.push(item);
  });
  if (next.length > 0) {
    videoList.value = videoList.value.concat(next);
  }
  return next.length;
};

const getCurrentClassifyVideo = () => {
  if (route.meta.isSearch || isLoading.value || !hasMore.value) return;
  isLoading.value = true;
  const request = currentClassify.value > 0
    ? apiVideoByClassfiy(currentClassify.value, pageInfo.value.page, pageInfo.value.limit)
    : apiVideoByPush();
  request
    .then(({ data }) => {
      if (data?.state && Array.isArray(data.data)) {
        const inserted = appendUniqueVideos(data.data);
        if (currentClassify.value <= 0) {
          // 推荐流接口无分页，首批加载后停止继续滚动拉取
          hasMore.value = false;
        } else if (data.data.length < pageInfo.value.limit || inserted === 0) {
          hasMore.value = false;
        }
      } else {
        hasMore.value = false;
      }
    })
    .finally(() => {
      isLoading.value = false;
    });
};

function closeVideo() {
  router.back();
  searchVideoInfo.value = null;
}

const getSearchVideo = () => {
  if (isLoading.value || !hasMore.value) return;
  isLoading.value = true;
  apiSearchVideo(route.params.key, pageInfo.value.page, pageInfo.value.limit)
    .then(({ data }) => {
      if (!data?.state) {
        hasMore.value = false;
        return;
      }
      const records = Array.isArray(data.data) ? data.data : (data.data?.records || []);
      const inserted = appendUniqueVideos(records);
      if (records.length < pageInfo.value.limit || inserted === 0) {
        hasMore.value = false;
      }
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const initView = () => {
  resetList();
  if (route.meta.isClassify) {
    currentClassify.value = Number(route.params.classify || 0);
    getCurrentClassifyVideo();
  } else if (route.meta.isSearch) {
    currentClassify.value = -1;
    getSearchVideo();
  } else {
    currentClassify.value = 0;
    apiGetClassifyByUser().then(({ data }) => {
      userClassifys.value = data?.data || [];
    });
    getCurrentClassifyVideo();
  }

  if (route.query.play) {
    apiGetVideoById(route.query.play).then(({ data }) => {
      if (data?.state) {
        searchVideoInfo.value = data.data;
      }
    });
  } else {
    searchVideoInfo.value = null;
  }
};

const listenScroll = () => {
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
  const windowHeight = document.documentElement.clientHeight || document.body.clientHeight;
  const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight;
  const distance = scrollHeight - (scrollTop + windowHeight);

  if (distance < 150 && distance > -1 && !isLoading.value && hasMore.value) {
    pageInfo.value.page++;
    if (route.meta.isSearch) {
      getSearchVideo();
    } else {
      getCurrentClassifyVideo();
    }
  }
};

watch(() => route.fullPath, () => {
  initView();
});

onMounted(() => {
  initView();
  window.addEventListener('scroll', listenScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', listenScroll);
});
</script>

<style scoped></style>
