<template>
  <v-layout v-if="currentVideo" full-height class="video-page-layout">
    <v-navigation-drawer
      v-if="drawer"
      location="right"
      :width="350"
      permanent
      class="right-preview-drawer"
    >
      <v-card v-if="drawerMode === 'similar'" elevation="0" color="background" class="pa-4" id="videoPlayList">
        <VideoCard
          :overlay="currentIndex == index"
          class="mb-4"
          :video-info="videoItem"
          v-for="(videoItem, index) in similarList"
          :key="index"
          @click="currentIndex = index"
        />
      </v-card>
      <v-card v-else-if="drawerMode === 'author'" elevation="0" color="background" class="pa-4" id="videoAuthorPanel">
        <div class="author-panel-head">
          <v-avatar size="42" class="elevation-2">
            <v-img :src="authorPanelAvatarUrl" cover @error="authorPanelAvatarUrl = '/logo.png'" />
          </v-avatar>
          <div class="author-panel-meta">
            <div class="author-panel-name">{{ authorPanel.nickName || '用户' }}</div>
            <div class="author-panel-sub">作者视频</div>
          </div>
          <v-spacer />
          <v-btn
            v-if="canToggleAuthorFollow"
            size="small"
            color="pink"
            variant="tonal"
            :loading="followPending"
            :disabled="followPending"
            @click="toggleAuthorFollow"
          >
            {{ authorPanel.followed ? '取关' : '关注' }}
          </v-btn>
        </div>
        <v-divider class="my-3" />
        <v-progress-linear v-if="authorPanel.loading" indeterminate />
        <v-card v-else-if="authorPanelVideos.length === 0" variant="tonal" class="pa-4 text-center">
          暂无视频
        
        </v-card>
        <VideoCard
          v-for="(videoItem, index) in authorPanelVideos"
          :key="videoItem.id || index"
          class="mb-4"
          :video-info="videoItem"
          :overlay="currentVideo && videoItem.id === currentVideo.id"
          @click="playAuthorVideo(videoItem, index)"
        />
      </v-card>
      <v-card v-else-if="drawerMode === 'share'" elevation="0" color="background" class="pa-4" id="videoSharePanel">
        <div class="share-panel-head">
          <div class="share-panel-title">好友分享</div>
          <div class="share-panel-sub">仅展示互相关注好友</div>
          <v-spacer />
          <v-btn
            icon="mdi-refresh"
            size="small"
            variant="text"
            :disabled="shareFriendsLoading"
            @click="openSharePanel(true)"
          />
        </div>
        <v-divider class="my-3" />
        <v-progress-linear v-if="shareFriendsLoading" indeterminate />
        <v-card v-else-if="shareFriends.length === 0" variant="tonal" class="pa-4 text-center">
          暂无可分享好友
        </v-card>
        <v-list v-else bg-color="transparent" class="share-friend-list py-0">
          <v-list-item
            v-for="friend in shareFriends"
            :key="friend.id"
            :title="friend.nickName || '用户'"
            :subtitle="friend.description || '这个人很神秘'"
          >
            <template #prepend>
              <v-avatar size="40" class="mr-3">
                <v-img :src="friend.avatarUrl || '/logo.png'" cover />
              </v-avatar>
            </template>
            <template #append>
              <v-btn
                size="small"
                color="success"
                variant="tonal"
                :loading="Boolean(shareSubmittingMap[friend.id])"
                :disabled="Boolean(shareSubmittingMap[friend.id])"
                @click.stop="shareCurrentVideoToFriend(friend)"
              >
                分享
              </v-btn>
            </template>
          </v-list-item>
        </v-list>
      </v-card>
      <v-card v-else-if="drawerMode === 'comment'" elevation="0" color="background" class="pa-4" id="videoCommentPanel">
        <div class="comment-panel-head">
          <div class="comment-panel-title">评论</div>
          <div class="comment-panel-sub">默认展示主评论，点击展开回复</div>
          <v-spacer />
          <v-btn
            icon="mdi-refresh"
            size="small"
            variant="text"
            :disabled="commentLoading"
            @click="openCommentPanel(true)"
          />
        </div>
        <v-divider class="my-3" />
        <v-textarea
          :model-value="commentInput"
          @update:model-value="commentInput = $event"
          variant="outlined"
          rows="2"
          density="compact"
          hide-details
          placeholder="写下你的评论..."
        />
        <div class="comment-publish-row">
          <v-spacer />
          <v-btn
            size="small"
            color="primary"
            variant="tonal"
            :loading="commentSubmitting"
            :disabled="commentSubmitting || !commentInput || !commentInput.trim()"
            @click="publishComment()"
          >
            发布
          </v-btn>
        </div>
        <v-divider class="my-3" />
        <v-progress-linear v-if="commentLoading" indeterminate />
        <v-card v-else-if="commentList.length === 0" variant="tonal" class="pa-4 text-center">
          暂无评论
        </v-card>
        <div v-else class="comment-list-wrap">
          <div v-for="comment in commentList" :key="comment.id" class="comment-item">
            <div class="comment-main">
              <v-avatar size="34">
                <v-img :src="comment.avatarUrl || '/logo.png'" cover />
              </v-avatar>
              <div class="comment-main-body">
                <div class="comment-main-meta">
                  <span class="comment-user">{{ comment.userNickName || '用户' }}</span>
                  <span class="comment-time">{{ formatCommentTime(comment.gmtCreated) }}</span>
                </div>
                <div class="comment-content">{{ comment.content || '' }}</div>
                <div class="comment-actions">
                  <v-btn size="x-small" variant="text" @click="beginReply(comment)">
                    回复
                  </v-btn>
                  <v-btn
                    v-if="Number(comment.childCount || 0) > 0"
                    size="x-small"
                    variant="text"
                    @click="toggleReplies(comment)"
                  >
                    {{ commentExpandedMap[comment.id] ? '收起回复' : ('展开回复(' + comment.childCount + ')') }}
                  </v-btn>
                </div>
              </div>
            </div>

            <div v-if="commentExpandedMap[comment.id]" class="comment-reply-wrap">
              <div
                v-for="reply in (commentRepliesMap[comment.id] || [])"
                :key="reply.id"
                class="comment-reply-item"
              >
                <v-avatar size="28">
                  <v-img :src="reply.avatarUrl || '/logo.png'" cover />
                </v-avatar>
                <div class="comment-reply-body">
                  <div class="comment-main-meta">
                    <span class="comment-user">{{ reply.userNickName || '用户' }}</span>
                    <span class="comment-time">{{ formatCommentTime(reply.gmtCreated) }}</span>
                  </div>
                  <div class="comment-content">
                    <template v-if="reply.replyToNickName">
                      回复 <span class="comment-reply-target">@{{ reply.replyToNickName }}</span>：
                    </template>
                    {{ reply.content || '' }}
                  </div>
                  <div class="comment-actions">
                    <v-btn size="x-small" variant="text" @click="beginReply(comment, reply)">
                      回复
                    </v-btn>
                  </div>
                </div>
              </div>

              <div class="comment-reply-load-more" v-if="!commentRepliesDoneMap[comment.id]">
                <v-btn
                  size="x-small"
                  variant="text"
                  :loading="Boolean(commentReplyLoadingMap[comment.id])"
                  :disabled="Boolean(commentReplyLoadingMap[comment.id])"
                  @click="loadReplies(comment)"
                >
                  加载更多回复
                </v-btn>
              </div>

              <div class="comment-reply-input-row">
                <v-text-field
                  density="compact"
                  variant="outlined"
                  hide-details
                  :placeholder="replyInputPlaceholder(comment.id)"
                  :model-value="commentReplyInputMap[comment.id] || ''"
                  @update:model-value="updateReplyInput(comment.id, $event)"
                />
                <v-btn
                  size="small"
                  color="primary"
                  variant="tonal"
                  :loading="Boolean(commentReplySubmittingMap[comment.id])"
                  :disabled="Boolean(commentReplySubmittingMap[comment.id]) || !String(commentReplyInputMap[comment.id] || '').trim()"
                  @click="submitReply(comment)"
                >
                  发送
                </v-btn>
                <v-btn
                  v-if="commentReplyTargetMap[comment.id]"
                  size="small"
                  variant="text"
                  @click="clearReplyTarget(comment.id)"
                >
                  取消
                </v-btn>
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </v-navigation-drawer>
    <v-main class="video-main-panel">
      <div ref="videoStageRef" class="video-stage" :class="{ 'controls-visible': showControls }" @wheel.prevent="handleStageWheel">
        <v-card ref="stageCardRef" rounded="0" width="100%" height="100%" class="video-stage-card">
        <video ref="video" class="video-js vjs-default-skin" controls :poster="currentVideo.playCover">
          <source :src="currentVideo.playUrl" :type="currentVideo.videoType || 'video/mp4'" />
        </video>
        <div class="video-caption-overlay" :class="{ 'caption-raised': showControls }">
          <div class="video-caption-author" v-if="authorText && !simpleMode && !hideAuthor">@{{ authorText }}</div>
          <div class="video-caption-text" v-if="captionText">{{ captionText }}</div>
        </div>
        <div style="position: absolute;left: 15px;top: 15px;z-index: 99999;">
          <v-btn size="40" color="bg" icon v-if="!hideClose" @click="closeVideo">
            <v-icon :size="20">mdi-close</v-icon>
          </v-btn>
        </div>
          <v-card
            v-if="!simpleMode || statsOnlyActions"
            class="pa-2 video-action-panel"
            elevation="0"
          >
          <div
            v-if="!statsOnlyActions"
            class="author-avatar-wrap"
            @click="openAuthorPanel()"
          >
            <v-avatar class="elevation-2">
              <v-img :src="authorAvatarUrl" cover @error="authorAvatarUrl = '/logo.png'" />
            </v-avatar>
            <v-icon v-if="showFollowPlus" class="author-plus-icon">mdi-plus</v-icon>
          </div>
          <v-btn
            v-if="!statsOnlyActions"
            size="40"
            icon
            class="similar-trigger-btn"
            :class="{ 'is-active': isSimilarDrawerActive }"
            @click="openRgihtD()"
          >
            <v-icon :size="20">mdi-compare</v-icon>
          </v-btn>
          <v-badge color="red" :content="likeCount" location="bottom">
            <v-btn size="40" :color="'pink'" icon @click="starVideo()">
              <v-icon :size="20">mdi-heart</v-icon>
            </v-btn>
          </v-badge>
          <v-badge color="red" :content="commentCount" location="bottom">
            <v-btn size="40" color="info" icon @click="openCommentPanel()">
              <v-icon :size="20">mdi-comment-text-outline</v-icon>
            </v-btn>
          </v-badge>

          <FavoriteCom :video-id="currentVideo.id" :callback="favoriteCallBack">
            <template #default="{ props }">
              <v-badge color="red" :content="favoriteCount" location="bottom">
                <v-btn v-bind="props" size="40" color="warning" icon>
                  <v-icon :size="20">mdi-star</v-icon>
                </v-btn>
              </v-badge>
            </template>
          </FavoriteCom>
          <v-badge color="red" :content="shareCount" location="bottom">
            <v-btn size="40" color="success" icon @click="openSharePanel()">
            <v-icon :size="20">mdi-near-me</v-icon>
          </v-btn>
          </v-badge>
          <v-btn
            size="40"
            color="grey-darken-1"
            icon
            :loading="uninterestedPending"
            :disabled="uninterestedPending"
            @click="markUninterested()"
          >
            <v-icon :size="20">mdi-thumb-down-outline</v-icon>
          </v-btn>

        </v-card>
        <div
          ref="controlZone"
          class="control-hover-zone"
          @mouseenter="showControls = true"
          @mousemove="keepControlsVisible"
          @mouseleave="hideControlsSoon"
        ></div>
        </v-card>
        <v-snackbar v-model="snackbar.show" :color="snackbar.color">
        {{ snackbar.text }}

        <template v-slot:actions>
          <v-btn color="blue" variant="text" @click="snackbar.show = false">
            了解
          
          </v-btn>
        </template>
        </v-snackbar>
      </div>
    </v-main>
  </v-layout>
</template>
<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { apiFileGet, apiGetFilePublicUrl } from '../../apis/file';
import { apiFollows, apiGetLike } from '../../apis/user/like';
import { apiAddHistory, apiCountComments, apiGetUserVideoById, apiGetVideoBySimilar, apiInitFollowFeed, apiListComments, apiPublishComment, apiReplyComment, apiSetUserVideoModel, apiShareVideoToFriend, apiStarVideo, apiUninterestedVideo, apiVideoByPush } from '../../apis/video';
import FavoriteCom from '../../components/favorite/index.vue';
import VideoCard from '../../components/video/card.vue';
import { useUserStore } from '../../stores';
const props = defineProps({
  videoInfo: {
    type: Object,
    default: null
  },
  videoList: {
    type: Array,
    default: []
  },
  hideClose: {
    type: Boolean,
    default: false
  },
  simpleMode: {
    type: Boolean,
    default: false
  },
  hideAuthor: {
    type: Boolean,
    default: false
  },
  statsOnlyActions: {
    type: Boolean,
    default: false
  },
  nextVideo: {
    type: Function,
    default: () => {

    }
  },
  closeVideo: {
    type: Function,
    default: () => { }
  }
})
const snackbar = ref({
  show: false,
  text: ""
})
const userStore = useUserStore()
const controlZone = ref(null)
const stageCardRef = ref(null)
const videoStageRef = ref(null)
const showControls = ref(false)
let hideControlsTimer = null
let controlBarEl = null

const isHttpUrl = value => typeof value === 'string' && /^https?:\/\//i.test(value)
const resolveObjectNameUrlMap = async names => {
  const uniqueNames = Array.from(new Set(
    (Array.isArray(names) ? names : [])
      .filter(name => typeof name === 'string' && name.trim() !== '' && !isHttpUrl(name))
  ))
  if (uniqueNames.length === 0) {
    return {}
  }
  const entries = await Promise.all(uniqueNames.map(async name => {
    try {
      const url = await apiGetFilePublicUrl(name)
      return [name, url || '']
    } catch (_e) {
      return [name, '']
    }
  }))
  return entries.reduce((map, [name, url]) => {
    map[name] = url
    return map
  }, {})
}
const shareFriendsLoading = ref(false)
const shareFriends = ref([])
const shareSubmittingMap = ref({})
const uninterestedPending = ref(false)
const commentLoading = ref(false)
const commentSubmitting = ref(false)
const commentInput = ref('')
const commentCountValue = ref(0)
const commentLoadedVideoId = ref(0)
const commentList = ref([])
const commentExpandedMap = ref({})
const commentRepliesMap = ref({})
const commentRepliesPageMap = ref({})
const commentRepliesDoneMap = ref({})
const commentReplyLoadingMap = ref({})
const commentReplyInputMap = ref({})
const commentReplySubmittingMap = ref({})
const commentReplyTargetMap = ref({})
const SIMILAR_LIST_LIMIT = 120

const normalizeShareFriend = item => ({
  id: Number(item?.id || 0),
  nickName: item?.nickName || item?.nickname || '',
  description: item?.description || '',
  avatar: item?.avatar || '',
  avatarUrl: '/logo.png'
})

const resolveShareFriendAvatars = async list => {
  const objectNames = list
    .map(friend => friend.avatar)
    .filter(name => typeof name === 'string' && name.trim() !== '' && !isHttpUrl(name))
  if (objectNames.length === 0) {
    shareFriends.value = list.map(friend => ({
      ...friend,
      avatarUrl: isHttpUrl(friend.avatar) ? friend.avatar : '/logo.png'
    }))
    return
  }
  const urlMap = await resolveObjectNameUrlMap(objectNames)
  shareFriends.value = list.map(friend => {
    if (isHttpUrl(friend.avatar)) {
      return {
        ...friend,
        avatarUrl: friend.avatar
      }
    }
    return {
      ...friend,
      avatarUrl: urlMap[friend.avatar] || '/logo.png'
    }
  })
}

const loadShareFriends = async (forceRefresh = false) => {
  if (!userStore.token) {
    shareFriends.value = []
    return
  }
  if (!forceRefresh && shareFriends.value.length > 0) {
    return
  }
  shareFriendsLoading.value = true
  try {
    const { data } = await apiGetLike('follows', '', 1, 200)
    const records = Array.isArray(data?.data?.records) ? data.data.records : []
    const mutualFriends = records
      .filter(item => Boolean(item?.each))
      .map(normalizeShareFriend)
      .filter(item => item.id > 0)
    await resolveShareFriendAvatars(mutualFriends)
  } catch (_e) {
    shareFriends.value = []
  } finally {
    shareFriendsLoading.value = false
  }
}

const shareCurrentVideoToFriend = async friend => {
  const friendId = Number(friend?.id || 0)
  const videoId = Number(currentVideo.value?.id || 0)
  if (friendId <= 0 || videoId <= 0) {
    return
  }
  if (shareSubmittingMap.value[friendId]) {
    return
  }
  shareSubmittingMap.value = {
    ...shareSubmittingMap.value,
    [friendId]: true
  }
  try {
    const { data } = await apiShareVideoToFriend(videoId, friendId)
    if (data?.state && data?.data === true && currentVideo.value) {
      currentVideo.value.shareCount = Number(currentVideo.value.shareCount || 0) + 1
    }
    snackbar.value = {
      show: true,
      text: data?.message || (data?.state ? '分享成功' : '分享失败'),
      color: data?.state ? undefined : 'error'
    }
  } catch (_e) {
    snackbar.value = {
      show: true,
      text: '分享失败',
      color: 'error'
    }
  } finally {
    shareSubmittingMap.value = {
      ...shareSubmittingMap.value,
      [friendId]: false
    }
  }
}

const handleMouseWheel = (event) =>{
  console.log(event.deltaY)
  if(event.deltaY >0) {
    if (currentIndex.value >= similarList.value.length - 1) {
        return;
      }
      currentIndex.value++;
  }else {
    if (currentIndex.value < 1) {
        return;
      }
      currentIndex.value--
  }
}
const handleStageWheel = (event) => {
  handleMouseWheel(event)
}
const drawer = ref(false)
const drawerMode = ref('similar')
const switchedToPushFeed = ref(false)
const instance = getCurrentInstance().proxy
const video = ref()
const videoPlayer = ref()
const similarList = ref([
  props.videoInfo
].slice(0, SIMILAR_LIST_LIMIT))
const canRequestMoreFromParent = computed(() => Number(similarList.value?.length || 0) < SIMILAR_LIST_LIMIT)

const currentIndex = ref(0)
let currentVideoUrlResolveSeq = 0
const currentVideo = computed(() => {

  let temp = currentIndex.value >= 0 ? similarList.value[currentIndex.value] : props.videoInfo
  if (!temp) {
    return null
  }
  temp.playUrl = temp.playUrl || apiFileGet(temp.url)
  temp.playCover = temp.playCover || apiFileGet(temp.cover)
  console.log(temp, "aa")
  return temp
})
const resolveCurrentVideoPlayUrls = async video => {
  if (!video) {
    return null
  }
  const resolveSeq = ++currentVideoUrlResolveSeq
  let playUrl = isHttpUrl(video.url) ? video.url : ''
  let playCover = isHttpUrl(video.cover) ? video.cover : ''

  if (video.url && !isHttpUrl(video.url)) {
    try {
      playUrl = await apiGetFilePublicUrl(video.url)
    } catch (_e) {
      playUrl = ''
    }
  }
  if (video.cover && !isHttpUrl(video.cover)) {
    try {
      playCover = await apiGetFilePublicUrl(video.cover)
    } catch (_e) {
      playCover = ''
    }
  }
  if (resolveSeq !== currentVideoUrlResolveSeq) {
    return null
  }
  video.playUrl = playUrl || ''
  video.playCover = playCover || ''
  return video
}
const captionText = computed(() => {
  return currentVideo.value?.caption || currentVideo.value?.description || ''
})
const likeCount = computed(() => Number(currentVideo.value?.likeCount ?? currentVideo.value?.startCount ?? 0))
const commentCount = computed(() => Number(commentCountValue.value || currentVideo.value?.commentCount || currentVideo.value?.commentsCount || 0))
const favoriteCount = computed(() => Number(currentVideo.value?.favoriteCount ?? currentVideo.value?.favoritesCount ?? 0))
const shareCount = computed(() => Number(currentVideo.value?.shareCount ?? 0))
const isSimilarDrawerActive = computed(() => drawer.value && drawerMode.value === 'similar')
const authorAvatarUrl = ref('/logo.png')
const resolveAuthorAvatar = async () => {
  const avatar = currentVideo.value?.user?.avatar
  if (!avatar) {
    authorAvatarUrl.value = '/logo.png'
    return
  }
  if (typeof avatar === 'string' && /^https?:\/\//i.test(avatar)) {
    authorAvatarUrl.value = avatar
    return
  }
  try {
    const url = await apiGetFilePublicUrl(avatar)
    authorAvatarUrl.value = url || '/logo.png'
  } catch (_e) {
    authorAvatarUrl.value = '/logo.png'
  }
}
const authorText = computed(() => {
  return currentVideo.value?.user?.nickName
    || currentVideo.value?.user?.nickname
    || currentVideo.value?.user?.name
    || "用户"
})
const followPending = ref(false)
const followStateMap = ref({})
const getAuthorId = video => Number(video?.user?.id || video?.userId || 0)
const getFollowedState = video => {
  const authorId = getAuthorId(video)
  if (authorId > 0 && Object.prototype.hasOwnProperty.call(followStateMap.value, authorId)) {
    return Boolean(followStateMap.value[authorId])
  }
  return Boolean(video?.followedAuthor)
}
const syncFollowStateForAuthor = (authorId, followed) => {
  if (!authorId || authorId <= 0) {
    return
  }
  followStateMap.value = {
    ...followStateMap.value,
    [authorId]: Boolean(followed)
  }
  if (currentVideo.value && getAuthorId(currentVideo.value) === authorId) {
    currentVideo.value.followedAuthor = Boolean(followed)
  }
  similarList.value = (similarList.value || []).map(item => {
    if (getAuthorId(item) !== authorId) {
      return item
    }
    return {
      ...item,
      followedAuthor: Boolean(followed)
    }
  })
  authorPanelVideos.value = (authorPanelVideos.value || []).map(item => {
    if (getAuthorId(item) !== authorId) {
      return item
    }
    return {
      ...item,
      followedAuthor: Boolean(followed)
    }
  })
}
const showFollowPlus = computed(() => {
  if (!userStore.token) {
    return false
  }
  const authorId = getAuthorId(currentVideo.value)
  const myId = Number(userStore.info?.id || 0)
  if (authorId > 0 && myId > 0 && authorId === myId) {
    return false
  }
  return !getFollowedState(currentVideo.value)
})
const authorPanel = ref({
  userId: null,
  nickName: '',
  avatar: '',
  followed: false,
  loading: false
})
const authorPanelVideos = ref([])
const authorPanelAvatarUrl = ref('/logo.png')
const canToggleAuthorFollow = computed(() => {
  if (!userStore.token) {
    return false
  }
  const authorId = Number(authorPanel.value.userId || 0)
  const myId = Number(userStore.info?.id || 0)
  if (authorId <= 0) {
    return false
  }
  return !(myId > 0 && authorId === myId)
})
const resolvePanelAvatar = async () => {
  const avatar = authorPanel.value.avatar
  if (!avatar) {
    authorPanelAvatarUrl.value = '/logo.png'
    return
  }
  if (typeof avatar === 'string' && /^https?:\/\//i.test(avatar)) {
    authorPanelAvatarUrl.value = avatar
    return
  }
  try {
    const url = await apiGetFilePublicUrl(avatar)
    authorPanelAvatarUrl.value = url || '/logo.png'
  } catch (_e) {
    authorPanelAvatarUrl.value = '/logo.png'
  }
}
const loadAuthorVideos = async userId => {
  authorPanel.value.loading = true
  authorPanelVideos.value = []
  try {
    const { data } = await apiGetUserVideoById(userId, 1, 100)
    const records = Array.isArray(data?.data?.records) ? data.data.records : []
    authorPanelVideos.value = records.filter(item => Number(item?.auditStatus || 0) === 1)
  } catch (_e) {
    authorPanelVideos.value = []
  } finally {
    authorPanel.value.loading = false
  }
}
const openAuthorPanel = async () => {
  const authorId = getAuthorId(currentVideo.value)
  if (authorId <= 0) {
    return
  }
  if (drawer.value && drawerMode.value === 'author' && Number(authorPanel.value.userId || 0) === authorId) {
    drawer.value = false
    drawerMode.value = 'similar'
    await restoreMainFeed()
    return
  }
  authorPanel.value.userId = authorId
  authorPanel.value.nickName = currentVideo.value?.user?.nickName || currentVideo.value?.user?.nickname || currentVideo.value?.userNickName || '用户'
  authorPanel.value.avatar = currentVideo.value?.user?.avatar || currentVideo.value?.userAvatar || ''
  authorPanel.value.followed = getFollowedState(currentVideo.value)
  drawerMode.value = 'author'
  drawer.value = true
  await resolvePanelAvatar()
  await loadAuthorVideos(authorId)
}
const playAuthorVideo = (videoItem, index) => {
  if (!videoItem) {
    return
  }
  similarList.value = authorPanelVideos.value.slice(0, SIMILAR_LIST_LIMIT)
  currentIndex.value = Math.min(index, Math.max(0, similarList.value.length - 1))
}
const toggleAuthorFollow = async () => {
  if (!canToggleAuthorFollow.value || followPending.value) {
    return
  }
  const authorId = Number(authorPanel.value.userId || 0)
  if (authorId <= 0) {
    return
  }
  const previous = Boolean(authorPanel.value.followed)
  const optimistic = !previous
  authorPanel.value.followed = optimistic
  syncFollowStateForAuthor(authorId, optimistic)
  followPending.value = true
  try {
    const { data } = await apiFollows(authorId)
    const followed = typeof data?.data === 'boolean' ? data.data : optimistic
    authorPanel.value.followed = followed
    syncFollowStateForAuthor(authorId, followed)
    if (followed) {
      apiInitFollowFeed()
    }
    snackbar.value = {
      text: data?.message || '操作成功',
      show: true
    }
  } catch (_e) {
    authorPanel.value.followed = previous
    syncFollowStateForAuthor(authorId, previous)
    snackbar.value = {
      text: '关注状态更新失败',
      show: true,
      color: 'error'
    }
  } finally {
    followPending.value = false
  }
}
const restoreMainFeed = async () => {
  const currentId = Number(currentVideo.value?.id || 0)
  if (props.videoList && props.videoList.length > 0) {
    similarList.value = props.videoList.slice(0, SIMILAR_LIST_LIMIT)
    const index = similarList.value.findIndex(item => Number(item?.id || 0) === currentId)
    currentIndex.value = index >= 0 ? index : 0
    return
  }
  await switchToPushFeed(true)
}
const switchToPushFeed = async (force = false) => {
  // 仅在首页没有传入固定列表时，回退到推送流
  if (props.videoList && props.videoList.length > 0) {
    return
  }
  if (!force && switchedToPushFeed.value) {
    return
  }
  try {
    const { data } = await apiVideoByPush()
    if (!data?.state || !Array.isArray(data.data) || data.data.length === 0) {
      return
    }
    switchedToPushFeed.value = true
    similarList.value = data.data.slice(0, SIMILAR_LIST_LIMIT)
    currentIndex.value = 0
  } catch (_e) {
    // 推送流失败时保持当前列表
  }
}
const openSharePanel = async (forceRefresh = false) => {
  if (drawer.value && drawerMode.value === 'share' && !forceRefresh) {
    drawer.value = false
    await restoreMainFeed()
    return
  }
  drawerMode.value = 'share'
  drawer.value = true
  await loadShareFriends(forceRefresh)
}
const normalizeComment = item => ({
  id: Number(item?.id || 0),
  videoId: Number(item?.videoId || 0),
  userId: Number(item?.userId || 0),
  userNickName: item?.userNickName || item?.userNickname || item?.nickName || '用户',
  userAvatar: item?.userAvatar || '',
  avatarUrl: '/logo.png',
  content: item?.content || '',
  rootId: Number(item?.rootId || 0),
  parentId: Number(item?.parentId || 0),
  replyToUserId: item?.replyToUserId == null ? null : Number(item.replyToUserId),
  replyToNickName: item?.replyToNickName || '',
  childCount: Number(item?.childCount || 0),
  hasMoreChildren: Boolean(item?.hasMoreChildren),
  gmtCreated: Number(item?.gmtCreated || 0),
  gmtUpdated: Number(item?.gmtUpdated || 0)
})
const resolveCommentAvatars = async list => {
  if (!Array.isArray(list) || list.length === 0) {
    return []
  }
  const objectNames = list
    .map(item => item.userAvatar)
    .filter(name => typeof name === 'string' && name.trim() !== '' && !isHttpUrl(name))
  const urlMap = objectNames.length > 0 ? await resolveObjectNameUrlMap(objectNames) : {}
  return list.map(item => {
    if (isHttpUrl(item.userAvatar)) {
      return { ...item, avatarUrl: item.userAvatar }
    }
    return { ...item, avatarUrl: urlMap[item.userAvatar] || '/logo.png' }
  })
}
const formatCommentTime = ts => {
  const t = Number(ts || 0)
  if (t <= 0) {
    return ''
  }
  const now = Date.now()
  const diff = now - t
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  if (diff < 60 * 60 * 1000) {
    return `${Math.max(1, Math.floor(diff / (60 * 1000)))}分钟前`
  }
  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.max(1, Math.floor(diff / (60 * 60 * 1000)))}小时前`
  }
  const d = new Date(t)
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  const h = `${d.getHours()}`.padStart(2, '0')
  const min = `${d.getMinutes()}`.padStart(2, '0')
  return `${m}-${day} ${h}:${min}`
}
const loadComments = async () => {
  const videoId = Number(currentVideo.value?.id || 0)
  if (videoId <= 0) {
    commentList.value = []
    commentLoadedVideoId.value = 0
    return
  }
  commentLoading.value = true
  try {
    const { data } = await apiListComments(videoId, 0, 1, 20)
    const rows = Array.isArray(data?.data) ? data.data.map(normalizeComment) : []
    commentList.value = await resolveCommentAvatars(rows)
    commentLoadedVideoId.value = videoId
    commentExpandedMap.value = {}
    commentRepliesMap.value = {}
    commentRepliesPageMap.value = {}
    commentRepliesDoneMap.value = {}
    commentReplyLoadingMap.value = {}
    commentReplyInputMap.value = {}
    commentReplySubmittingMap.value = {}
    commentReplyTargetMap.value = {}
  } catch (_e) {
    commentList.value = []
    commentLoadedVideoId.value = 0
  } finally {
    commentLoading.value = false
  }
}
const refreshCommentCount = async () => {
  const videoId = Number(currentVideo.value?.id || 0)
  if (videoId <= 0) {
    commentCountValue.value = 0
    return
  }
  try {
    const { data } = await apiCountComments(videoId)
    const count = Number(data?.data || 0)
    commentCountValue.value = Number.isFinite(count) ? Math.max(0, count) : 0
    if (currentVideo.value) {
      currentVideo.value.commentCount = commentCountValue.value
    }
  } catch (_e) {
    commentCountValue.value = Number(currentVideo.value?.commentCount || currentVideo.value?.commentsCount || 0)
  }
}
const loadReplies = async (rootComment, reset = false) => {
  const videoId = Number(currentVideo.value?.id || 0)
  const rootId = Number(rootComment?.id || 0)
  if (videoId <= 0 || rootId <= 0) {
    return
  }
  if (commentReplyLoadingMap.value[rootId]) {
    return
  }
  const nextPage = reset ? 1 : Number(commentRepliesPageMap.value[rootId] || 0) + 1
  commentReplyLoadingMap.value = {
    ...commentReplyLoadingMap.value,
    [rootId]: true
  }
  try {
    const { data } = await apiListComments(videoId, rootId, nextPage, 20)
    const rows = Array.isArray(data?.data) ? data.data.map(normalizeComment) : []
    const withAvatar = await resolveCommentAvatars(rows)
    const prev = reset ? [] : (commentRepliesMap.value[rootId] || [])
    const merged = reset ? withAvatar : [...prev, ...withAvatar]
    const uniqueMap = new Map()
    merged.forEach(item => {
      uniqueMap.set(item.id, item)
    })
    commentRepliesMap.value = {
      ...commentRepliesMap.value,
      [rootId]: Array.from(uniqueMap.values())
    }
    commentRepliesPageMap.value = {
      ...commentRepliesPageMap.value,
      [rootId]: nextPage
    }
    commentRepliesDoneMap.value = {
      ...commentRepliesDoneMap.value,
      [rootId]: withAvatar.length < 20
    }
  } catch (_e) {
    if (reset) {
      commentRepliesMap.value = {
        ...commentRepliesMap.value,
        [rootId]: []
      }
    }
  } finally {
    commentReplyLoadingMap.value = {
      ...commentReplyLoadingMap.value,
      [rootId]: false
    }
  }
}
const toggleReplies = async rootComment => {
  const rootId = Number(rootComment?.id || 0)
  if (rootId <= 0) {
    return
  }
  const expanded = Boolean(commentExpandedMap.value[rootId])
  commentExpandedMap.value = {
    ...commentExpandedMap.value,
    [rootId]: !expanded
  }
  if (!expanded && !Array.isArray(commentRepliesMap.value[rootId])) {
    await loadReplies(rootComment, true)
  }
}
const beginReply = async (rootComment, targetComment = null) => {
  const rootId = Number(rootComment?.id || 0)
  if (rootId <= 0) {
    return
  }
  commentExpandedMap.value = {
    ...commentExpandedMap.value,
    [rootId]: true
  }
  if (!Array.isArray(commentRepliesMap.value[rootId])) {
    await loadReplies(rootComment, true)
  }
  const target = targetComment || rootComment
  commentReplyTargetMap.value = {
    ...commentReplyTargetMap.value,
    [rootId]: {
      parentId: Number(target?.id || rootId),
      replyToNickName: target?.userNickName || '',
      replyToUserId: Number(target?.userId || 0)
    }
  }
}
const clearReplyTarget = rootId => {
  commentReplyTargetMap.value = {
    ...commentReplyTargetMap.value,
    [rootId]: null
  }
  commentReplyInputMap.value = {
    ...commentReplyInputMap.value,
    [rootId]: ''
  }
}
const replyInputPlaceholder = rootId => {
  const target = commentReplyTargetMap.value[rootId]
  if (target?.replyToNickName) {
    return `回复 @${target.replyToNickName}`
  }
  return '写下你的回复...'
}
const updateReplyInput = (rootId, value) => {
  commentReplyInputMap.value = {
    ...commentReplyInputMap.value,
    [rootId]: value
  }
}
const publishComment = async () => {
  const content = String(commentInput.value || '').trim()
  const videoId = Number(currentVideo.value?.id || 0)
  if (!content || videoId <= 0 || commentSubmitting.value) {
    return
  }
  commentSubmitting.value = true
  try {
    const { data } = await apiPublishComment(videoId, content)
    const ok = Boolean(data?.state) && Boolean(data?.data)
    snackbar.value = {
      show: true,
      text: ok ? '评论成功' : (data?.message || '评论未通过审核或提交失败'),
      color: ok ? undefined : 'warning'
    }
    if (!ok) {
      return
    }
    commentInput.value = ''
    await loadComments()
    await refreshCommentCount()
  } catch (_e) {
    snackbar.value = {
      show: true,
      text: '评论提交失败',
      color: 'error'
    }
  } finally {
    commentSubmitting.value = false
  }
}
const submitReply = async rootComment => {
  const videoId = Number(currentVideo.value?.id || 0)
  const rootId = Number(rootComment?.id || 0)
  if (videoId <= 0 || rootId <= 0) {
    return
  }
  const content = String(commentReplyInputMap.value[rootId] || '').trim()
  if (!content || commentReplySubmittingMap.value[rootId]) {
    return
  }
  const target = commentReplyTargetMap.value[rootId]
  const parentId = Number(target?.parentId || rootId)
  commentReplySubmittingMap.value = {
    ...commentReplySubmittingMap.value,
    [rootId]: true
  }
  try {
    const { data } = await apiReplyComment(videoId, rootId, parentId, content)
    const ok = Boolean(data?.state) && Boolean(data?.data)
    snackbar.value = {
      show: true,
      text: ok ? '回复成功' : (data?.message || '回复未通过审核或提交失败'),
      color: ok ? undefined : 'warning'
    }
    if (!ok) {
      return
    }
    commentReplyInputMap.value = {
      ...commentReplyInputMap.value,
      [rootId]: ''
    }
    commentReplyTargetMap.value = {
      ...commentReplyTargetMap.value,
      [rootId]: null
    }
    rootComment.childCount = Number(rootComment.childCount || 0) + 1
    rootComment.hasMoreChildren = true
    await loadReplies(rootComment, true)
    await refreshCommentCount()
  } catch (_e) {
    snackbar.value = {
      show: true,
      text: '回复提交失败',
      color: 'error'
    }
  } finally {
    commentReplySubmittingMap.value = {
      ...commentReplySubmittingMap.value,
      [rootId]: false
    }
  }
}
const openCommentPanel = async (forceRefresh = false) => {
  const videoId = Number(currentVideo.value?.id || 0)
  if (drawer.value && drawerMode.value === 'comment' && !forceRefresh) {
    drawer.value = false
    await restoreMainFeed()
    return
  }
  drawerMode.value = 'comment'
  drawer.value = true
  // 只要不是当前视频的评论缓存，就强制重载，避免串到上一条视频
  if (forceRefresh || commentList.value.length === 0 || commentLoadedVideoId.value !== videoId) {
    await loadComments()
  }
}
const openRgihtD = () => {
  if (!drawer.value) {
    drawerMode.value = 'similar'
    drawer.value = true
    return
  }
  if (drawerMode.value === 'similar') {
    drawer.value = false
    restoreMainFeed()
    return
  }
  drawerMode.value = 'similar'
}
const normalizePlayerBox = () => {
  if (!videoPlayer.value) {
    return
  }
  try {
    // 强制关闭 fluid/fill，避免播放器容器高度被挤压
    videoPlayer.value.fluid(false)
    videoPlayer.value.fill(false)
    videoPlayer.value.removeClass('vjs-fluid')
    videoPlayer.value.removeClass('vjs-fill')
    videoPlayer.value.removeClass('vjs-16-9')
    videoPlayer.value.removeClass('vjs-4-3')
    const playerEl = videoPlayer.value.el && videoPlayer.value.el()
    if (playerEl) {
      playerEl.style.width = '100%'
      playerEl.style.height = '100%'
      playerEl.style.paddingTop = '0'
    }
  } catch (_e) {
    // 忽略播放器尺寸异常
  }
}
const syncPlayerLayout = () => {
  const doResize = () => {
    if (!videoPlayer.value || !stageCardRef.value?.$el) {
      return
    }
    try {
      const stageEl = stageCardRef.value.$el
      const width = stageEl.clientWidth
      const height = stageEl.clientHeight
      if (width > 0 && height > 0) {
        videoPlayer.value.dimensions(width, height)
      }
      normalizePlayerBox()
      videoPlayer.value.trigger('resize')
      videoPlayer.value.trigger('componentresize')
    } catch (_e) {
      // 忽略播放器重排异常
    }
  }
  nextTick(() => {
    doResize()
    setTimeout(doResize, 80)
  })
}
const hideControlsSoon = () => {
  if (hideControlsTimer) {
    clearTimeout(hideControlsTimer)
  }
  const delay = videoPlayer.value && videoPlayer.value.isFullscreen && videoPlayer.value.isFullscreen() ? 1200 : 90
  hideControlsTimer = setTimeout(() => {
    const zoneHover = controlZone.value && controlZone.value.matches && controlZone.value.matches(':hover')
    const barHover = controlBarEl && controlBarEl.matches && controlBarEl.matches(':hover')
    if (zoneHover || barHover) {
      showControls.value = true
      hideControlsSoon()
      return
    }
    showControls.value = false
  }, delay)
}
const keepControlsVisible = () => {
  if (hideControlsTimer) {
    clearTimeout(hideControlsTimer)
  }
  showControls.value = true
}
const favoriteCallBack = (e) => {
  if (e == "已收藏") {
    if (typeof currentVideo.value.favoriteCount === 'number') {
      currentVideo.value.favoriteCount++
    } else {
      currentVideo.value.favoritesCount = Number(currentVideo.value.favoritesCount || 0) + 1
    }
  } else if (e == "移出" || e == "取消收藏") {
    if (typeof currentVideo.value.favoriteCount === 'number') {
      currentVideo.value.favoriteCount = Math.max(0, Number(currentVideo.value.favoriteCount || 0) - 1)
    } else {
      currentVideo.value.favoritesCount = Math.max(0, Number(currentVideo.value.favoritesCount || 0) - 1)
    }
  }
  snackbar.value = {
    show: true,
    text: e,
    color: e === "未登录" ? "error" : undefined
  }
}
const isAddHistory = ref(true)
const isLikeVideo = ref(false)
const windowKeyEvent = (event) => {
  switch (event.which) {
    case 38:
      if (currentIndex.value < 1) {
        return;
      }
      currentIndex.value--
      break
    case 40:
      if (currentIndex.value >= similarList.value.length - 1) {
        return;
      }
      currentIndex.value++;
      break
    case 27:
      props.closeVideo()
      break
    case 70:
      videoPlayer.value.requestFullscreen()
      break
  }
}
onUnmounted(() => {
  window.removeEventListener("keydown", windowKeyEvent)
  if (controlBarEl) {
    controlBarEl.removeEventListener('mouseenter', keepControlsVisible)
    controlBarEl.removeEventListener('mouseleave', hideControlsSoon)
    controlBarEl = null
  }
  if (hideControlsTimer) {
    clearTimeout(hideControlsTimer)
  }
})

const firstInitVideo = () => {
  console.log(currentVideo)
  if (videoPlayer.value || !currentVideo.value) return;
  videoPlayer.value = instance.$video(video.value, {
    playbackRates: [0.5, 1, 1.5, 2],
    notSupportedMessage: "暂不支持该视频类型",
    autoplay: true,
    fluid: false,
    fill: false,
    responsive: false,
    controlBar: {
      children: [
        'playToggle',
        'volumePanel',
        'progressControl',
        'durationDisplay',
        'playbackRateMenuButton',
        'fullscreenToggle'
      ]
    }
  })
  videoPlayer.value.on("useractive", () => {
    showControls.value = true
  })
  videoPlayer.value.on("userinactive", () => {
    hideControlsSoon()
  })
  videoPlayer.value.on("mousemove", () => {
    keepControlsVisible()
    hideControlsSoon()
  })
  videoPlayer.value.on("fullscreenchange", () => {
    keepControlsVisible()
    hideControlsSoon()
  })
  controlBarEl = videoPlayer.value.controlBar?.el?.() || null
  if (controlBarEl) {
    controlBarEl.addEventListener('mouseenter', keepControlsVisible)
    controlBarEl.addEventListener('mouseleave', hideControlsSoon)
  }
  syncPlayerLayout()
  videoPlayer.value.volume(localStorage.getItem("volume") || 1)
  window.addEventListener("keydown", windowKeyEvent)
  videoPlayer.value.on("volumechange", () => {
    localStorage.setItem("volume", videoPlayer.value.volume())
  })
  videoPlayer.value.on("timeupdate", function () {
    if (this.currentTime() >= 3 && isAddHistory.value) {
      isAddHistory.value = false
      apiAddHistory(currentVideo.value.id)
    }
    let duration = this.duration()
    let score = this.currentTime() >= (duration / 5)
    if (score) {
      if (!isLikeVideo.value)
        apiSetUserVideoModel(currentVideo.value.id, currentVideo.value.labelNames, 1)
      isLikeVideo.value = true;
    } else isLikeVideo.value = false

  })
  nextTick(() => {
    videoPlayer.value.play().catch(() => {})
  })
  if (props.videoList.length == 0 && !switchedToPushFeed.value) {
    apiGetVideoBySimilar(props.videoInfo.labelNames, props.videoInfo.id).then(({ data }) => {
      similarList.value = similarList.value.concat(data.data).slice(0, SIMILAR_LIST_LIMIT)
    })
  }

}
const likeUser = () => {
  if (!showFollowPlus.value) {
    return
  }
  apiFollows(currentVideo.value.user.id).then(({ data }) => {
    const followed = typeof data?.data === "boolean" ? data.data : true
    currentVideo.value.followedAuthor = followed
    if (followed) {
      apiInitFollowFeed()
    }
    snackbar.value = {
      text: data.message,
      show: true
    }
  })
}
onMounted(async () => {
  await resolveCurrentVideoPlayUrls(currentVideo.value)
  firstInitVideo()
  window.addEventListener('resize', syncPlayerLayout)
  refreshCommentCount()
})

const starVideo = () => {

  apiStarVideo(currentVideo.value.id).then(({ data }) => {
    snackbar.value = {
      show: true,
      text: data.message
    }
    if (!data.state) {
      return;
    }
    if (data.message == "已点赞") {
      if (typeof currentVideo.value.likeCount === 'number') {
        currentVideo.value.likeCount++
      } else {
        currentVideo.value.startCount = Number(currentVideo.value.startCount || 0) + 1
      }
    } else {
      if (typeof currentVideo.value.likeCount === 'number') {
        currentVideo.value.likeCount = Math.max(0, Number(currentVideo.value.likeCount || 0) - 1)
      } else {
        currentVideo.value.startCount = Math.max(0, Number(currentVideo.value.startCount || 0) - 1)
      }
    }


  })
}
const playNextAfterUninterested = () => {
  if (currentIndex.value < similarList.value.length - 1) {
    currentIndex.value++
    return
  }
  // 已到当前列表末尾时，先请求外部补充列表，再尝试切到下一条
  props.nextVideo(currentIndex.value)
  if (!canRequestMoreFromParent.value) {
    return
  }
  props.nextVideo(currentIndex.value)
  setTimeout(() => {
    if (currentIndex.value < similarList.value.length - 1) {
      currentIndex.value++
    }
  }, 120)
}
const markUninterested = async () => {
  const videoId = Number(currentVideo.value?.id || 0)
  if (videoId <= 0 || uninterestedPending.value) {
    return
  }
  uninterestedPending.value = true
  try {
    const { data } = await apiUninterestedVideo(videoId)
    snackbar.value = {
      show: true,
      text: data?.message || (data?.state ? '已标记不感兴趣' : '操作失败'),
      color: data?.state ? undefined : 'error'
    }
    if (data?.state) {
      playNextAfterUninterested()
    }
  } catch (_e) {
    snackbar.value = {
      show: true,
      text: '操作失败',
      color: 'error'
    }
  } finally {
    uninterestedPending.value = false
  }
}
const playVideo = (n) => {
  if (n) {
    if (canRequestMoreFromParent.value) {
      props.nextVideo(currentIndex.value)
    }
    // 当前播放的视频才换链，避免提前批量请求
    resolveCurrentVideoPlayUrls(n).then(resolvedVideo => {
      const target = resolvedVideo || n
      // videoPlayer.value.reset()
      setTimeout(() => {
        firstInitVideo()
        if (!videoPlayer.value) {
          return
        }
        isAddHistory.value = true
        videoPlayer.value.src([
          {
            src: target.playUrl,
            type: target.videoType,
            poster: target.playCover
          }
        ])
        videoPlayer.value.load()
        videoPlayer.value.play()
        apiSetUserVideoModel(target.id, target.labelNames, -0.5)
      }, 10)
    })
  }
}
watch(() => props.videoList, () => {
  if (props.videoList && props.videoList.length > 0) {
    similarList.value = props.videoList.slice(0, SIMILAR_LIST_LIMIT)
    if (currentIndex.value >= similarList.value.length) {
      currentIndex.value = Math.max(0, similarList.value.length - 1)
    }
  }
}, {
  immediate: true,
  deep: true
})
watch(() => currentVideo.value?.user?.avatar, () => {
  resolveAuthorAvatar()
}, { immediate: true })
watch(() => currentVideo.value?.followedAuthor, v => {
  if (drawerMode.value === 'author') {
    authorPanel.value.followed = Boolean(v)
  }
})
watch(
  () => currentVideo.value?.id,
  (id, prevId) => {
    if (!id || id === prevId) {
      return
    }
    // 切换视频时清空评论缓存，确保重新打开评论面板不会看到上一条视频的数据
    commentList.value = []
    commentLoadedVideoId.value = 0
    commentExpandedMap.value = {}
    commentRepliesMap.value = {}
    commentRepliesPageMap.value = {}
    commentRepliesDoneMap.value = {}
    commentReplyLoadingMap.value = {}
    commentReplyInputMap.value = {}
    commentReplySubmittingMap.value = {}
    commentReplyTargetMap.value = {}
    playVideo(currentVideo.value)
    refreshCommentCount()
    if (drawer.value && drawerMode.value === 'comment') {
      loadComments()
    }
  }
)
watch(drawer, () => {
  syncPlayerLayout()
})

onUnmounted(() => {
  window.removeEventListener('resize', syncPlayerLayout)
})

</script>   
<style scoped>
.video-page-layout {
  width: 100%;
  height: 100%;
}

.video-main-panel {
  height: 100%;
}

.right-preview-drawer {
  background-color: #252632 !important;
}

.video-stage {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 100%;
}

.video-stage-card {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.video-action-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: absolute;
  right: 24px;
  top: 58%;
  transform: translateY(-50%);
  background-color: transparent;
  z-index: 10050;
}

.similar-trigger-btn {
  background: linear-gradient(160deg, #3a9cff 0%, #00c7e6 100%);
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(0, 130, 200, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.similar-trigger-btn:hover {
  transform: translateY(-1px) scale(1.03);
  box-shadow: 0 10px 22px rgba(0, 130, 200, 0.34);
  filter: saturate(1.1);
}

.similar-trigger-btn.is-active {
  background: linear-gradient(160deg, #1a8cff 0%, #00b6d4 100%);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.35), 0 12px 26px rgba(0, 130, 200, 0.4);
}

.author-avatar-wrap {
  position: relative;
  width: fit-content;
  cursor: pointer;
}

.author-plus-icon {
  position: absolute;
  left: 50%;
  bottom: -8px;
  transform: translateX(-50%);
  color: #ffffff;
  font-size: 14px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #ff4d5a;
  border: 2px solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.author-panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-panel-meta {
  min-width: 0;
}

.author-panel-name {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.author-panel-sub {
  color: #9aa6c1;
  font-size: 12px;
}

.share-panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.share-panel-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}

.share-panel-sub {
  color: #9aa6c1;
  font-size: 12px;
}

.share-friend-list :deep(.v-list-item-title) {
  color: #f3f7ff;
}

.share-friend-list :deep(.v-list-item-subtitle) {
  color: #9aa6c1;
}

.comment-panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-panel-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}

.comment-panel-sub {
  color: #9aa6c1;
  font-size: 12px;
}

.comment-publish-row {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.comment-list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  padding: 10px;
}

.comment-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comment-main-body {
  min-width: 0;
  flex: 1;
}

.comment-main-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-user {
  color: #f3f7ff;
  font-size: 13px;
  font-weight: 600;
}

.comment-time {
  color: #9aa6c1;
  font-size: 11px;
}

.comment-content {
  color: #e8edf8;
  font-size: 13px;
  line-height: 1.5;
  margin-top: 4px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
}

.comment-reply-wrap {
  margin-left: 44px;
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-reply-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.comment-reply-body {
  min-width: 0;
  flex: 1;
}

.comment-reply-target {
  color: #7fc8ff;
}

.comment-reply-load-more {
  margin-left: 36px;
}

.comment-reply-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.video-caption-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  padding: 54px 18px 14px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.72) 80%);
  pointer-events: none;
  transition: padding-bottom 0.12s ease;
}

.video-caption-overlay.caption-raised {
  padding-bottom: 52px;
}

.video-caption-author {
  color: #f3f7ff;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

.video-caption-text {
  color: #ffffff;
  font-size: 16px;
  line-height: 1.6;
  max-width: min(72vw, 760px);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.video-stage :deep(.video-js) {
  width: 100% !important;
  height: 100% !important;
  background: #000;
}

.video-stage :deep(.video-js.vjs-fluid),
.video-stage :deep(.video-js.vjs-fill),
.video-stage :deep(.video-js.vjs-16-9),
.video-stage :deep(.video-js.vjs-4-3) {
  padding-top: 0 !important;
  height: 100% !important;
}

.video-stage :deep(.video-js .vjs-tech) {
  width: 100% !important;
  height: 100% !important;
  object-fit: contain !important;
}

.video-stage :deep(.video-js .vjs-control-bar) {
  z-index: 10000;
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none;
  transition: opacity 0.08s ease;
  transform: translateY(6px);
}

.video-stage.controls-visible :deep(.video-js .vjs-control-bar) {
  opacity: 1 !important;
  visibility: visible !important;
  pointer-events: auto;
  transform: translateY(0);
}

.video-stage :deep(.video-js .vjs-duration) {
  display: block !important;
  order: 4;
  margin-left: auto;
  margin-right: 8px;
}

.video-stage :deep(.video-js .vjs-playback-rate) {
  order: 5;
}

.video-stage :deep(.video-js .vjs-fullscreen-control) {
  order: 99;
  margin-left: 8px;
}

.control-hover-zone {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: clamp(180px, 32vh, 280px);
  z-index: 9999;
  background: transparent;
}
</style>
