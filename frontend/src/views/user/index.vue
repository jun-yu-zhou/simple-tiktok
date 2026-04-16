<template>
  <v-container :fluid="true" style="height: 500px;">
    <v-card>
      <v-img :height="220" aspect-ratio="16/9" cover src="https://cdn.vuetifyjs.com/images/parallax/material.jpg">
        <v-list style="position: absolute; left: 0; bottom: 0; width: 100%; background-color: rgba(1,1,1,0.5); color: white;">
          <v-list-item :title="userInfo.nickName" :subtitle="userInfo.description">
            <template #prepend>
              <v-avatar :image="avatarImg" size="50" />
            </template>
          </v-list-item>
        </v-list>
      </v-img>

      <v-tabs v-model="tab" color="#7bbfea">
        <v-tab value="aa" :to="`/user/home${isSelf ? '' : '?lookId=' + userInfo.id}`">主页</v-tab>
        <v-tab value="one" to="/user/video" v-if="isSelf">创作中心</v-tab>
        <v-tab value="two" to="/user/favorites" v-if="isSelf">收藏夹</v-tab>
        <v-tab value="3" to="/user/history" v-if="isSelf">浏览历史</v-tab>
        <v-tab value="two3" to="/user/classify" v-if="isSelf">订阅分类</v-tab>
        <v-tab value="4" :to="`/user/like${isSelf ? '' : '?lookId=' + userInfo.id}`">关注/粉丝</v-tab>
        <v-spacer></v-spacer>
        <v-btn
          class="ma-2"
          color="primary"
          variant="tonal"
          :loading="followLoading"
          @click="toggleFollow"
          v-if="!isSelf"
        >
          {{ followedByCurrent ? '取消关注' : '关注' }}
        </v-btn>
        <v-btn class="ma-2" variant="text" @click="editDialog = !editDialog" v-if="isSelf">编辑信息</v-btn>
      </v-tabs>
    </v-card>

    <router-view class="mt-2" />

    <v-dialog v-model="editDialog" max-width="760">
      <v-card title="编辑用户信息">
        <v-divider />
        <v-card-text>
          <v-form>
            <div class="profile-edit-layout">
              <div class="avatar-column">
                <v-hover v-slot="{ isHovering, props }">
                  <div v-bind="props" class="avatar-upload-shell" @click="avatarFileRef?.click()">
                    <v-avatar size="180" class="avatar-upload-preview">
                      <v-img :src="avatarImg" cover />
                    </v-avatar>
                    <div class="avatar-upload-mask" v-show="isHovering || uploading > -1">
                      <v-icon v-if="uploading == -1" size="34">mdi-camera-plus</v-icon>
                      <v-progress-circular v-else :model-value="uploading" :size="48" :width="4"></v-progress-circular>
                    </div>
                  </div>
                </v-hover>
                <div class="avatar-help-text">点击头像上传新图片</div>
              </div>

              <div class="info-column">
                <v-text-field v-model="userInfo.nickName" label="昵称" placeholder="请输入昵称"></v-text-field>
                <v-text-field v-model="userInfo.description" label="简介" placeholder="请输入简介"></v-text-field>
                <v-radio-group v-model="userInfo.sex" label="性别" inline>
                  <v-radio :value="1" label="男"></v-radio>
                  <v-radio :value="0" label="女"></v-radio>
                </v-radio-group>
              </div>
            </div>
            <input hidden @change="uploadAvatar" ref="avatarFileRef" type="file" accept="image/*" />
          </v-form>
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-btn text="取消" color="warning" @click="editDialog = false" />
          <v-spacer />
          <v-btn text="保存" color="success" @click="saveInfo()" />
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
      <template #actions>
        <v-btn color="blue" variant="text" @click="snackbar.show = false">了解</v-btn>
      </template>
    </v-snackbar>
  </v-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { apiGetFilePublicUrl, apiUploadFile } from '../../apis/file';
import { apiInitFollowFeed } from '../../apis/video';
import { apiFollows, apiIsFollowing } from '../../apis/user/like';
import { apiChangeUserInfo, apiGetUserInfo } from '../../apis/user/user';
import router from '../../router';
import { useUserStore } from '../../stores';
const TOKEN_KEY = 'simple-tiktok:token';

const tab = ref();
const userStore = useUserStore();
const route = useRoute();
const userInfo = ref({});
const editDialog = ref(false);
const avatarFileRef = ref();
const uploading = ref(-1);
const avatarRemoteUrl = ref('');
const followedByCurrent = ref(false);
const followLoading = ref(false);
const snackbar = ref({
  show: false,
  text: ''
});

const isSelf = computed(() => {
  if (!userInfo.value) return false;
  return userStore.$state.info.id == userInfo.value.id;
});

const avatarImg = computed(() => {
  if (!userInfo.value) {
    getUserInfo();
    return '/logo.png';
  }
  if (avatarFileRef.value && avatarFileRef.value.files && avatarFileRef.value.files[0]) {
    const URLRef = window.URL || window.webkitURL;
    return URLRef.createObjectURL(avatarFileRef.value.files[0]);
  }
  if (userInfo.value == null) {
    return '/logo.png';
  }
  if (avatarRemoteUrl.value) {
    return avatarRemoteUrl.value;
  }
  if (!userInfo.value.avatar) return '/logo.png';
  return '/logo.png';
});

const uploadAvatar = () => {
  if (!avatarFileRef.value.files[0]) return;
  apiUploadFile(avatarFileRef.value.files[0], {
    next: e => {
      uploading.value = e.total.percent;
    },
    error: () => {
      uploading.value = -1;
      snackbar.value = {
        text: '上传失败',
        show: true,
        color: 'error'
      };
    },
    complete: (_e, fileId) => {
      uploading.value = -1;
      if (!fileId.state) {
        snackbar.value = {
          text: fileId.message,
          show: true
        };
        return;
      }
      userInfo.value.avatar = fileId.data;
      avatarRemoteUrl.value = _e?.url || avatarRemoteUrl.value;
      snackbar.value = {
        text: '上传成功',
        show: true,
        color: 'success'
      };
    }
  });
};

const getUserInfo = () => {
  if (route.query && route.query.lookId) {
    userStore.$patch({
      lookId: route.query.lookId
    });
  } else {
    userStore.$patch({
      lookId: userStore.$state.info.id
    });
  }
  apiGetUserInfo(userStore.$state.lookId).then(({ data }) => {
    if (data.state) {
      userInfo.value = data.data;
      refreshFollowState();
    }
  });
};

const refreshFollowState = async () => {
  const targetId = Number(userInfo.value?.id || 0);
  if (!targetId || isSelf.value) {
    followedByCurrent.value = false;
    return;
  }
  try {
    const { data } = await apiIsFollowing(targetId);
    if (data?.state) {
      followedByCurrent.value = Boolean(data.data);
    }
  } catch (_e) {}
};

const toggleFollow = async () => {
  const targetId = Number(userInfo.value?.id || 0);
  if (!targetId || isSelf.value || followLoading.value) {
    return;
  }
  followLoading.value = true;
  try {
    const { data } = await apiFollows(targetId);
    snackbar.value = {
      text: data?.message || '操作完成',
      show: true,
      color: data?.state ? 'success' : 'error'
    };
    if (!data?.state) {
      return;
    }
    followedByCurrent.value = Boolean(data.data);
    if (followedByCurrent.value) {
      await apiInitFollowFeed();
    }
  } finally {
    followLoading.value = false;
  }
};

const saveInfo = () => {
  apiChangeUserInfo(userInfo.value).then(({ data }) => {
    snackbar.value = {
      text: data.message,
      show: true,
      color: data.state ? 'success' : 'error'
    };
    if (!data.state) {
      return;
    }
    editDialog.value = false;
    apiGetUserInfo(userInfo.value.id).then(({ data: userResp }) => {
      if (userResp.state) {
        userStore.$patch({
          info: userResp.data
        });
      }
    });
  });
};

watch(
  () => route.query,
  () => {
    getUserInfo();
  },
  {
    immediate: true
  }
);

watch(
  () => userInfo.value?.avatar,
  async avatar => {
    if (!avatar) {
      avatarRemoteUrl.value = '';
      return;
    }
    try {
      avatarRemoteUrl.value = await apiGetFilePublicUrl(avatar);
    } catch (_e) {
      avatarRemoteUrl.value = '';
    }
  },
  { immediate: true }
);

onMounted(() => {
  const token = userStore.$state.token || sessionStorage.getItem(TOKEN_KEY);
  if (!token) {
    router.push({ path: '/' });
    return;
  }
  if (!userStore.$state.token) {
    userStore.$patch({ token });
  }
  userStore.$patch({
    lookId: route.query.lookId || userStore.info.id
  });
  getUserInfo();
});
</script>

<style scoped>
.profile-edit-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 18px;
  align-items: start;
}

.avatar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.avatar-upload-shell {
  position: relative;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-upload-preview {
  width: 180px;
  height: 180px;
}

.avatar-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-help-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.info-column {
  min-width: 0;
}

@media (max-width: 760px) {
  .profile-edit-layout {
    grid-template-columns: 1fr;
  }
}
</style>
