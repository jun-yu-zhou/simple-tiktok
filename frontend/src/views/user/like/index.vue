<template>
  <v-card>
    <v-card-title inset class="float-left">粉丝</v-card-title>
    <div class="float-none"></div>

    <v-tabs v-model="currentType" align-tabs="end" @update:model-value="onTypeChange">
      <v-tab value="follows">关注</v-tab>
      <v-tab value="fans">粉丝</v-tab>
    </v-tabs>
    <v-divider />

    <v-list lines="two">
      <template v-for="item in currentItems" :key="item.id">
        <v-list-item
          :title="item.nickName"
          :subtitle="item.description || '这个人很懒，什么都没留下'"
          @click="goUser(item.id)"
        >
          <template #prepend>
            <v-avatar>
              <v-img :src="avatarOf(item)" cover @error="markAvatarError(item)" />
            </v-avatar>
          </template>

          <template #append>
            <v-btn color="grey-lighten-1" variant="text" @mousedown.stop @click.stop.prevent="unLikeOrLike(item.id)">
              {{ currentType === 'fans' ? (item.each ? '取消互关' : '互相关注') : '取消关注' }}
            </v-btn>
          </template>
        </v-list-item>
        <v-divider />
      </template>
    </v-list>

    <v-card
      v-if="currentItems.length === 0"
      height="300px"
      class="ma-4"
      variant="tonal"
      style="text-align: center; line-height: 300px"
    >
      好像没有什么内容呢
    </v-card>

    <v-pagination v-else-if="pageInfo.pages > 1" v-model="pageInfo.page" :length="pageInfo.pages" />

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
      <template #actions>
        <v-btn color="blue" variant="text" @click="snackbar.show = false">了解</v-btn>
      </template>
    </v-snackbar>
  </v-card>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { apiGetFilePublicUrl } from '../../../apis/file';
import { apiInitFollowFeed } from '../../../apis/video';
import { apiFollows, apiGetLike } from '../../../apis/user/like';
import { apiGetUserInfo } from '../../../apis/user/user';
import { useUserStore } from '../../../stores';

const router = useRouter();
const userStore = useUserStore();
const currentType = ref('fans');
const currentItems = ref([]);
const avatarErrorMap = ref({});
const querySeq = ref(0);
const snackbar = ref({
  show: false,
  text: ''
});
const pageInfo = ref({
  page: 1,
  pages: 1,
  limit: 10
});

const avatarKeyOf = item => item?.avatar || item?.userAvatar || '';
const avatarOf = item => {
  const key = avatarKeyOf(item);
  if (!key || avatarErrorMap.value[key]) {
    return '/logo.png';
  }
  return item.avatarUrl || '/logo.png';
};
const markAvatarError = item => {
  const key = avatarKeyOf(item);
  if (!key) {
    return;
  }
  avatarErrorMap.value = {
    ...avatarErrorMap.value,
    [key]: true
  };
};
const resolveAvatarUrl = async item => {
  const avatarKey = avatarKeyOf(item);
  if (avatarKey) {
    if (/^https?:\/\//i.test(avatarKey)) {
      return avatarKey;
    }
    try {
      const url = await apiGetFilePublicUrl(avatarKey);
      if (url) {
        return url;
      }
    } catch (_e) {}
  }
  try {
    const { data } = await apiGetUserInfo(item.id);
    const key = data?.data?.avatar;
    if (!key) {
      return '/logo.png';
    }
    if (/^https?:\/\//i.test(key)) {
      return key;
    }
    const url = await apiGetFilePublicUrl(key);
    return url || '/logo.png';
  } catch (_e) {
    return '/logo.png';
  }
};

const getLike = async () => {
  const seq = ++querySeq.value;
  currentItems.value = [];
  avatarErrorMap.value = {};
  try {
    const { data } = await apiGetLike(currentType.value, userStore.lookId, pageInfo.value.page, pageInfo.value.limit);
    if (!data?.state || seq !== querySeq.value) {
      return;
    }
    const payload = data.data || {};
    const records = Array.isArray(payload.records) ? payload.records : [];
    const total = Number(payload.total || records.length || 0);
    const pages = Math.max(1, Math.ceil(total / Math.max(pageInfo.value.limit, 1)));
    pageInfo.value.pages = pages;
    if (pageInfo.value.page > pages) {
      pageInfo.value.page = pages;
      return;
    }
    const list = await Promise.all(
      records.map(async item => {
        const avatarUrl = await resolveAvatarUrl(item);
        return { ...item, avatarUrl };
      })
    );
    if (seq !== querySeq.value) {
      return;
    }
    currentItems.value = list;
  } catch (_e) {}
};

const unLikeOrLike = async id => {
  const { data } = await apiFollows(id);
  snackbar.value = {
    text: data?.message || '操作完成',
    show: true
  };
  if (!data?.state) {
    return;
  }
  // 关注成功后补拉一次关注收件箱，避免“关注的人”页读不到新内容。
  if (data?.data === true) {
    await apiInitFollowFeed();
  }
  await getLike();
};

const goUser = id => {
  if (!id) {
    return;
  }
  router.push(`/user?lookId=${id}`);
};

const onTypeChange = () => {
  if (pageInfo.value.page !== 1) {
    pageInfo.value.page = 1;
    return;
  }
  getLike();
};

watch(
  () => pageInfo.value.page,
  () => {
    getLike();
  },
  { immediate: true }
);

watch(() => userStore.lookId, () => {
  if (pageInfo.value.page !== 1) {
    pageInfo.value.page = 1;
    return;
  }
  getLike();
});
</script>
