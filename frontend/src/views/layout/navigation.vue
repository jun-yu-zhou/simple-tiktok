<template>
  <v-navigation-drawer color="#252632">
    <v-list v-if="userStore.token">
      <v-list-item :prepend-avatar="userAvatarUrl" :title="userStore.info.nickName" :subtitle="userStore.info.description"></v-list-item>
    </v-list>
    <v-list v-else>
      <v-list-item prepend-icon="mdi-account" title="未登录" subtitle="请先登录，享受更多服务"></v-list-item>
    </v-list>
    <v-divider />

    <v-list density="compact" nav>
      <v-list-item prepend-icon="mdi-home" title="热门视频" to="/"></v-list-item>
      <v-list-item prepend-icon="mdi-video" title="推荐视频" to="/pushVideo"></v-list-item>
      <v-list-item v-if="userStore.token" prepend-icon="mdi-account-multiple" title="好友分享" to="/friendShareVideo"></v-list-item>
      <!-- <v-list-item prepend-icon="mdi-label-multiple" title="视频分类" to="/classify"></v-list-item> -->
      <template v-if="userStore.token">
        <v-list-item prepend-icon="mdi-account" title="个人中心" to="/user"></v-list-item>
        <v-list-item prepend-icon="mdi-heart" title="关注的人" to="/followVideo"></v-list-item>
      </template>
      <v-list-item :prepend-icon="item.icon || 'mdi-file-document-alert-outline'" :title="item.name" v-for="item in allClassifyList" :to="`/video/${item.id}`"></v-list-item>
    </v-list>
  </v-navigation-drawer>
</template>
<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { apiClassifyGetAll, apiGetClassifyByUser } from '../../apis/classify';
import { apiGetFilePublicUrl } from '../../apis/file';
import { useUserStore } from '../../stores';

const userStore = useUserStore();
const allClassifyList = ref([]);
const userAvatarUrl = ref('/logo.png');

const refreshUserAvatar = async () => {
  const avatar = userStore.info?.avatar;
  if (!avatar) {
    userAvatarUrl.value = '/logo.png';
    return;
  }
  if (typeof avatar === 'string' && /^https?:\/\//i.test(avatar)) {
    userAvatarUrl.value = avatar;
    return;
  }
  try {
    const url = await apiGetFilePublicUrl(avatar);
    userAvatarUrl.value = url || '/logo.png';
  } catch (_e) {
    userAvatarUrl.value = '/logo.png';
  }
};

const refreshClassifyList = async () => {
  try {
    const allResp = await apiClassifyGetAll();
    const allTypes = allResp?.data?.state && Array.isArray(allResp.data.data) ? allResp.data.data : [];
    if (!userStore.token) {
      allClassifyList.value = allTypes;
      return;
    }
    const subResp = await apiGetClassifyByUser();
    const subIds = subResp?.data?.state && Array.isArray(subResp.data.data)
      ? subResp.data.data.map(id => Number(id)).filter(id => Number.isFinite(id))
      : [];
    allClassifyList.value = allTypes.filter(item => subIds.includes(Number(item.id)));
  } catch (_e) {
    allClassifyList.value = [];
  }
};

const onClassifyUpdated = () => {
  refreshClassifyList();
};

watch(() => userStore.token, () => {
  refreshClassifyList();
  refreshUserAvatar();
});
watch(() => userStore.info?.avatar, () => {
  refreshUserAvatar();
});

onMounted(() => {
  refreshClassifyList();
  refreshUserAvatar();
  window.addEventListener('classify-updated', onClassifyUpdated);
});

onUnmounted(() => {
  window.removeEventListener('classify-updated', onClassifyUpdated);
});
</script>
<style lang="scss" scoped>
.v-navigation-drawer {
  border: none !important;
}
</style>
