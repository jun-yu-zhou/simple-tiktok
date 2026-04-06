<template>
    <VLayout>
        <v-navigation-drawer permanent :width="200">
            <v-list mandatory :lines="false" nav v-model:selected="currentFavoriteIndex">
                <v-list-subheader class="text-subtitle-1">我的收藏夹</v-list-subheader>
                <FavoriteEdit :edit-data="editFavorite" :close-event="getUserFavorites">
                    <template #default="{ props }">
                        <v-btn v-bind="props" block class="mb-2" color="white" :variant="'outlined'">
                            <v-icon>mdi-plus</v-icon>
                            添加收藏夹</v-btn>
                    </template>
                </FavoriteEdit>
                <v-divider />
                <v-list-item variant="tonal" :active="currentFavoriteIndex == i" v-for="(item, i) in favoriteItems" :key="i"
                    :value="i" color="primary">
                    <template #append>
                        <v-btn :variant="'plain'" icon :density="'compact'" @click="showMenu(i)">
                            <v-icon>mdi-pen</v-icon>
                        </v-btn>
                    </template>
                    <v-list-item-title v-text="item.name"></v-list-item-title>
                </v-list-item>
                <VListSubheader v-if="favoriteItems.length == 0">您没有收藏夹</VListSubheader>
            </v-list>
        </v-navigation-drawer>
        <v-main class="ml-1">
            <VideoList :video-list="videoList"/>
        </v-main>
    </VLayout>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { apiGetFavorites } from '../../../apis/user/favorites';
import { apiGetVideoByFavoriteId } from '../../../apis/video';
import FavoriteEdit from '../../../components/favorite/edit.vue';
import VideoList from '../../../components/video/list.vue';
const favoriteItems = ref([])
const videoList = ref([])
const currentFavoriteIndex = ref([0])
const currentFavorite = computed(() => {
    const selected = Array.isArray(currentFavoriteIndex.value) ? currentFavoriteIndex.value[0] : currentFavoriteIndex.value
    if (selected === undefined || selected === null || selected < 0) {
        return null
    }
    return favoriteItems.value[selected] || null
})
const getUserFavorites = () => {
    apiGetFavorites().then(({ data }) => {
        favoriteItems.value = Array.isArray(data.data) ? data.data : []
        if (favoriteItems.value.length == 0) {
            currentFavoriteIndex.value = []
            return;
        }
        const selected = Array.isArray(currentFavoriteIndex.value) ? currentFavoriteIndex.value[0] : currentFavoriteIndex.value
        if (selected !== undefined && selected !== null && selected > -1 && selected < favoriteItems.value.length) {
            return;
        }
        if (favoriteItems.value.length > 0) {
            currentFavoriteIndex.value = [0]
        }
    })
}
const getFavoriteVideo = () => {
    if (currentFavorite.value == null) {
        return;
    }
    apiGetVideoByFavoriteId(currentFavorite.value.id).then(({ data }) => {
        videoList.value = data.data
    })
}
const isShowMenu = ref(false)
const editFavorite = ref(null)
const showMenu = (index) => {
    editFavorite.value = favoriteItems.value[index]
    isShowMenu.value = true
}
getUserFavorites()
watch(currentFavorite, () => {
    getFavoriteVideo()
})
onMounted(() => {
    getFavoriteVideo()
})
</script>
