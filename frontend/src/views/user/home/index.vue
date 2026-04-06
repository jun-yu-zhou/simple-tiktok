<template>
    <v-card title="我的视频" elevation="0">
        <v-divider/>
        <VideoListVue
          :video-list="videoList"
          :simple-mode="true"
          :hide-author="true"
          :stats-only-actions="true"
          :hide-card-user-name="true"
        />
        <v-pagination
        v-if="pageInfo.pages>1"
              v-model="pageInfo.page"
              :length="pageInfo.pages"
            ></v-pagination>
    </v-card>
</template>
<script setup>
import { ref, watch } from 'vue';
import { apiGetUserVideoById } from '../../../apis/video';
import VideoListVue from '../../../components/video/list.vue';
import { useUserStore } from '../../../stores';
const userStore = useUserStore()
const videoList = ref([])
const pageInfo = ref({
    page: 1,
    pages: 1,
    limit: 10
})
const getVideo = ()=>{
    apiGetUserVideoById(userStore.$state.lookId, pageInfo.value.page, pageInfo.value.limit).then(({data})=>{
        if(!data.state){
            return
        }
        const payload = data.data || {}
        videoList.value = payload.records || []
        pageInfo.value.pages = Math.max(1, Number(payload.pages || 1))
    })
}
watch(()=>pageInfo.value.page, ()=>{
    getVideo()
}, {immediate:true})

watch(() => userStore.lookId, () => {
    pageInfo.value.page = 1
    getVideo()
})
</script>
