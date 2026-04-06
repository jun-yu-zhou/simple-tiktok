<template>
  <v-app-bar floating name="app-bar" :elevation="0" color="#252632">
    <v-app-bar-nav-icon icon="mdi-menu" @click="clickEvent(1, 1)" />
    <v-app-bar-title>SimpleTikTok</v-app-bar-title>

    <div ref="searchWrapRef" class="header-search-wrap e700-hide">
      <v-text-field
        v-model="searchKey"
        hide-details
        density="comfortable"
        prepend-inner-icon="mdi-magnify"
        single-line
        clearable
        placeholder="搜索视频"
        @focus="openDesktopSearch"
        @click="openDesktopSearch"
        @keyup.enter="search"
        @click:clear="search"
      />
    </div>

    <v-spacer />

    <v-toolbar-items variant="plain" class="pr-2">
      <v-btn class="d700-hide" variant="text" @click="openSearchMobile">
        <v-icon>mdi-magnify</v-icon>
        搜索
      </v-btn>
      <auth />
    </v-toolbar-items>

    <v-dialog v-model="showSearchMobile" max-width="500px" location="top">
      <v-card>
        <v-toolbar flat color="background">
          <v-text-field
            v-model="searchKey"
            hide-details
            prepend-inner-icon="mdi-magnify"
            single-line
            clearable
            placeholder="搜索视频"
            @click:clear="search"
            @keyup.enter="search"
          />
        </v-toolbar>
        <v-divider />
        <v-card-text>
          <div v-if="serarchHistory.length > 0" class="d-flex align-center mb-2">
            <h2 class="text-h6">搜索历史</h2>
            <v-spacer />
            <v-btn size="small" variant="text" @click="clearSearchHistory">清空</v-btn>
          </div>
          <v-chip-group v-model="searchKey" column @update:model-value="search">
            <v-chip v-for="item in serarchHistory" :key="item" filter :value="item" variant="outlined">
              {{ item }}
            </v-chip>
          </v-chip-group>
        </v-card-text>
        <v-divider />
        <v-card-text class="pt-2">
          <hotList elevation="0" />
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-app-bar>

  <Teleport to="body">
    <div
      v-show="showDesktopSearch"
      ref="searchPanelRef"
      class="search-dropdown-portal"
      :style="desktopPanelStyle"
    >
      <v-card class="search-dropdown-card">
        <v-card-text v-if="serarchHistory.length > 0">
          <div v-if="serarchHistory.length > 0" class="d-flex align-center mb-2">
            <h2 class="text-h6">搜索历史</h2>
            <v-spacer />
            <v-btn size="small" variant="text" @click="clearSearchHistory">清空</v-btn>
          </div>
          <v-chip-group v-model="searchKey" column @update:model-value="search">
            <v-chip v-for="item in serarchHistory" :key="item" filter :value="item" variant="outlined">
              {{ item }}
            </v-chip>
          </v-chip-group>
        </v-card-text>
        <v-divider v-if="serarchHistory.length > 0" />
        <v-card-text :class="serarchHistory.length > 0 ? 'pt-2' : 'pt-0'">
          <hotList elevation="0" />
        </v-card-text>
      </v-card>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import hotList from '../../components/hotList.vue';
import { apiClearUserSearchHistory, apiGetUserSearchHistory } from '../../apis/user/user';
import auth from '../auth/index.vue';

const { clickEvent } = defineProps({
  clickEvent: {
    type: Function,
    default: () => {}
  }
});

const router = useRouter();
const searchWrapRef = ref(null);
const searchPanelRef = ref(null);
const showDesktopSearch = ref(false);
const showSearchMobile = ref(false);
const searchKey = ref('');
const serarchHistory = ref([]);
const desktopPanelStyle = ref({
  left: '0px',
  top: '0px',
  width: '520px'
});

const updateDesktopPanelPosition = () => {
  const el = searchWrapRef.value;
  if (!el) {
    return;
  }
  const rect = el.getBoundingClientRect();
  desktopPanelStyle.value = {
    left: `${Math.max(12, rect.left)}px`,
    top: `${rect.bottom + 8}px`,
    width: `${Math.max(320, rect.width)}px`
  };
};

const loadSearchHistory = () => {
  apiGetUserSearchHistory().then(({ data }) => {
    serarchHistory.value = Array.isArray(data?.data) ? data.data : [];
  });
};

const openDesktopSearch = () => {
  loadSearchHistory();
  updateDesktopPanelPosition();
  showDesktopSearch.value = true;
  nextTick(updateDesktopPanelPosition);
};

const openSearchMobile = () => {
  loadSearchHistory();
  showSearchMobile.value = true;
};

const clearSearchHistory = () => {
  apiClearUserSearchHistory().then(({ data }) => {
    if (data?.state) {
      serarchHistory.value = [];
    }
  });
};

const search = () => {
  showDesktopSearch.value = false;
  showSearchMobile.value = false;
  const keyword = String(searchKey.value || '').trim();
  if (!keyword) {
    router.push({ path: '/' });
    return;
  }
  router.push({ path: '/video/search/' + encodeURIComponent(keyword) });
  loadSearchHistory();
  searchKey.value = '';
};

const handleClickOutside = e => {
  const wrap = searchWrapRef.value;
  const panel = searchPanelRef.value;
  const target = e.target;
  if (wrap && wrap.contains(target)) {
    return;
  }
  if (panel && panel.contains(target)) {
    return;
  }
  showDesktopSearch.value = false;
};

const handleWindowChange = () => {
  if (!showDesktopSearch.value) {
    return;
  }
  updateDesktopPanelPosition();
};

onMounted(() => {
  loadSearchHistory();
  document.addEventListener('mousedown', handleClickOutside);
  window.addEventListener('resize', handleWindowChange);
  window.addEventListener('scroll', handleWindowChange, true);
});

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside);
  window.removeEventListener('resize', handleWindowChange);
  window.removeEventListener('scroll', handleWindowChange, true);
});
</script>

<style scoped>
::v-deep(.v-field__outline) {
  --v-field-border-width: 0px;
}

.header-search-wrap {
  width: min(560px, 46vw);
}

.search-dropdown-portal {
  position: fixed;
  z-index: 5000;
}

.search-dropdown-card {
  max-height: 70vh;
  overflow: auto;
}

@media only screen and (min-width: 700px) {
  .d700-hide {
    display: none;
  }
}

@media only screen and (max-width: 700px) {
  .e700-hide {
    display: none;
  }
}
</style>
