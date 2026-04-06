<template>
  <v-card v-if="videoInfo" hover ripple :elevation="0" rounded="lg">
    <v-img
      :src="coverSrc"
      class="align-end"
      gradient="to bottom, rgba(0,0,0,.1), rgba(0,0,0,.5)"
      height="300px"
      cover
    >
      <v-card-text class="text-white pa-0" v-if="!overlay">
        <v-card-actions class="ml-1 mr-1 pa-0">
          <span class="ma-2 font-weight-bold" text-color="white">
            <v-icon>mdi-heart</v-icon>
            {{ likeCount }} 点赞
          </span>
          <v-spacer />
          <v-btn variant="tonal" density="comfortable">{{ videoInfo.duration || '--:--' }}</v-btn>
        </v-card-actions>
      </v-card-text>
    </v-img>

    <v-card-actions>
      <span style="max-height: 20px; color: white" class="ml-1 overflow-hidden">{{ videoInfo.caption || videoInfo.title }}</span>
      <v-spacer />
      <v-btn
        v-if="videoInfo.user && showAuthorBtn"
        size="small"
        color="white"
        variant="tonal"
        @click.stop
        :to="userStore.token ? `/user?lookId=${videoInfo.user.id}` : ''"
      >
        @{{ videoInfo.user.nickName || '用户' }}
      </v-btn>
    </v-card-actions>

    <v-overlay scrim="black" :model-value="overlay" contained persistent width="100%" style="top: 0">
      <v-card color="rgba(1,1,1,0.5)" height="350px">
        <v-card-title class="pb-0">播放中</v-card-title>
        <v-chip-group class="ml-2 mr-2">
          <v-chip v-for="item in labels" :key="item">{{ item }}</v-chip>
        </v-chip-group>
        <v-card-subtitle>
          <v-row>
            <v-col>{{ videoInfo.historyCount || 0 }} 播放</v-col>
            <v-col>{{ likeCount }} 点赞</v-col>
            <v-col>{{ favoriteCount }} 收藏</v-col>
          </v-row>
        </v-card-subtitle>
      </v-card>
    </v-overlay>
  </v-card>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { apiFileGet, apiGetFilePublicUrl } from '../../apis/file';
import { useUserStore } from '../../stores';

const userStore = useUserStore();
const props = defineProps({
  videoInfo: {
    type: Object,
    default: null
  },
  overlay: {
    type: Boolean,
    default: false
  },
  hideUserName: {
    type: Boolean,
    default: false
  }
});

const videoInfo = computed(() => props.videoInfo || {});
const coverSrc = ref('/not-found.png')
const resolveCoverSrc = async cover => {
  if (!cover) {
    coverSrc.value = '/not-found.png'
    return
  }
  if (typeof cover === 'string' && /^https?:\/\//i.test(cover)) {
    coverSrc.value = cover
    return
  }
  const cached = apiFileGet(cover)
  if (cached) {
    coverSrc.value = cached
    return
  }
  try {
    const url = await apiGetFilePublicUrl(cover)
    coverSrc.value = url || '/not-found.png'
  } catch (_e) {
    coverSrc.value = '/not-found.png'
  }
}
const labels = computed(() => {
  const value = videoInfo.value.labelNames;
  if (Array.isArray(value)) {
    return value.filter(Boolean);
  }
  return String(value || '').split(',').map(s => s.trim()).filter(Boolean);
});
const likeCount = computed(() => Number(videoInfo.value.likeCount ?? videoInfo.value.startCount ?? 0));
const favoriteCount = computed(() => Number(videoInfo.value.favoriteCount ?? videoInfo.value.favoritesCount ?? 0));
const showAuthorBtn = computed(() => {
  if (props.hideUserName) {
    return false;
  }
  const authorId = Number(videoInfo.value?.user?.id || videoInfo.value?.userId || 0);
  const selfId = Number(userStore.info?.id || 0);
  if (authorId > 0 && selfId > 0 && authorId === selfId) {
    return false;
  }
  return true;
});

watch(
  () => videoInfo.value.cover,
  cover => {
    resolveCoverSrc(cover)
  },
  { immediate: true }
)
</script>
