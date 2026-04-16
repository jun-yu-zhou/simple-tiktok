<template>
    <v-dialog v-model="dialog" scrollable :width="300">
        <template v-slot:activator="{ props: activatorProps }">
            <slot :props="{ ...activatorProps, onClick: onActivatorClick }"></slot>
        </template>
        <v-card>
            <v-card-title>选择收藏夹
            </v-card-title>
            <v-divider></v-divider>
            <v-card-text style="height: 300px;">
                <v-radio-group v-model="dialogm1" column>
                    <v-radio
                        v-for="item in favoriteItems"
                        :key="item.id"
                        :label="item.hasVideo ? `${item.name}（已收藏）` : item.name"
                        :value="item.id"
                    ></v-radio>
                </v-radio-group>
            </v-card-text>
            <v-divider></v-divider>
            <v-card-actions>
                <v-btn color="red" variant="text" @click="dialog = false">
                    取消
                </v-btn>
                <v-spacer />
                <FavoriteEdit :close-event="getFavorites">
                    <template #default="{ props }">
                        <v-btn :variant="'text'" v-bind="props">创建收藏夹</v-btn>
                    </template>

                </FavoriteEdit>
                <v-btn color="primary" variant="text" :disabled="!(props.videoId > 0 && dialogm1 > 0)" @click="save()">
                    {{ actionText }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>
<script setup>
import { computed, ref, watch } from 'vue';
import { apiFavoriteVideo, apiGetFavoriteStates, apiGetFavorites } from '../../apis/user/favorites';
import FavoriteEdit from './edit.vue';
import { useUserStore } from '../../stores';
const dialog = ref(false)
const dialogm1 = ref(0)
const favoriteItems = ref([])
const userStore = useUserStore()
const props = defineProps({
    videoId: {
        type: Number,
        default: null
    },
    callback: {
        type: Function,
        default: () => { }
    }
})

const selectedFavorite = computed(() => favoriteItems.value.find(item => item.id === dialogm1.value) || null)
const actionText = computed(() => (selectedFavorite.value?.hasVideo ? '移出' : '收藏'))

const setDefaultFavorite = () => {
    if (!favoriteItems.value.length) {
        dialogm1.value = 0
        return
    }
    // 优先默认选中“已收藏”的收藏夹，让用户一眼能看出当前状态
    const selected = favoriteItems.value.find(item => item.hasVideo) || favoriteItems.value[0]
    dialogm1.value = selected.id
}

const normalizeFavoriteItems = (items) => {
    if (!Array.isArray(items)) {
        return []
    }
    return items.map(item => ({
        ...item,
        hasVideo: Boolean(item?.hasVideo)
    }))
}

const getFavorites = () => {
    // 传入 videoId 时优先拉取“收藏状态”接口；其余场景回退到原收藏夹接口
    const request = props.videoId > 0 ? apiGetFavoriteStates(props.videoId) : apiGetFavorites()
    request.then(({ data }) => {
        if (!data.state) {
            dialog.value = false
            return
        }
        favoriteItems.value = normalizeFavoriteItems(data.data)
        setDefaultFavorite()
    })
}

const onActivatorClick = (event) => {
    if (!userStore.token) {
        event?.stopPropagation?.()
        event?.preventDefault?.()
        props.callback('未登录')
        return
    }
    dialog.value = true
}

watch(dialog, (newV) => {
    if (newV) {
        getFavorites()
    }
})
const save = () => {
    if (props.videoId > 0 && dialogm1.value > 0) {
        apiFavoriteVideo(dialogm1.value, props.videoId).then(({ data }) => {
            if (!data.state) {
                return;
            }
            dialog.value = false
            props.callback(data.message)
        })
    }
}
</script>
