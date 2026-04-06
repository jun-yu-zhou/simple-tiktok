import request from './request';
import { apiGetFilePublicUrl } from './file';

const isHttpUrl = value => typeof value === 'string' && /^https?:\/\//i.test(value);

const isObjectName = value => typeof value === 'string' && value.trim() !== '' && !isHttpUrl(value);

const normalizeVideo = video => {
  if (!video) {
    return video;
  }
  const labels = Array.isArray(video.labels) ? video.labels : [];
  return {
    ...video,
    title: video.caption || video.title || '',
    description: video.description || '',
    labelNames: labels.join(','),
    cover: video.coverUrl || video.coverFileName || video.cover,
    url: video.videoUrl || video.videoFileName || video.url,
    startCount: Number(video.likeCount || video.startCount || 0),
    favoritesCount: Number(video.favoriteCount || video.favoritesCount || 0),
    historyCount: Number(video.viewCount || video.historyCount || 0),
    user: {
      ...(video.user || { id: video.userId }),
      nickName: (video.user && video.user.nickName) || video.userNickName || (video.user && video.user.nickname) || '',
      avatar: (video.user && video.user.avatar) || video.userAvatar || ''
    }
  };
};

const normalizeVideoList = list => {
  if (!Array.isArray(list)) {
    return [];
  }
  return list.map(normalizeVideo);
};

const resolveVideoMediaUrls = async list => {
  // 列表数据仅做字段标准化，资源链接改为按实际展示时单条换链
  return normalizeVideoList(list);
};

const resolveSingleVideoMediaUrl = async video => {
  const normalized = normalizeVideo(video);
  if (!normalized) {
    return null;
  }
  let cover = normalized.cover;
  let url = normalized.url;
  if (isObjectName(cover)) {
    try {
      cover = await apiGetFilePublicUrl(cover);
    } catch (_e) {
      cover = normalized.cover;
    }
  }
  if (isObjectName(url)) {
    try {
      url = await apiGetFilePublicUrl(url);
    } catch (_e) {
      url = normalized.url;
    }
  }
  return {
    ...normalized,
    cover,
    url
  };
};

export const apiVideoByClassfiy = async (classfiyId, page = 1, limit = 15) => {
  if (classfiyId > 0) {
    const res = await request.get(`/video/type/${classfiyId}`, { params: { page, limit } });
    res.data.data = await resolveVideoMediaUrls(res.data.data);
    return res;
  }
  return apiVideoByPush();
};

export const apiVideoHotRank = () => request.get('/index/video/hot/rank').then(res => {
  const list = Array.isArray(res?.data?.data) ? res.data.data : [];
  res.data.data = list.map(item => ({
    ...item,
    title: item.caption || item.title || ''
  }));
  return res;
});

export const apiVideoByHot = async () => {
  const res = await request.get('/index/video/hot');
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiVideoByPush = async () => {
  const res = await request.get('/index/pushVideos');
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiGetVideoByFavoriteId = async (favoriteId = 0) => {
  const res = await request.get(`/video/favorites/${favoriteId}`);
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiGetVideoByUser = async (page = 1, limit = 5) => {
  const res = await request.get('/video', { params: { page, limit } });
  const payload = res.data.data || {};
  payload.records = await resolveVideoMediaUrls(payload.records || []);
  res.data.data = payload;
  return res;
};

export const apiGetVideoBySimilar = async (labelNames, id) => {
  const params = new URLSearchParams();
  const cleanId = String(id ?? '').trim();
  if (cleanId) {
    params.append('videoId', cleanId);
  }
  const res = await request.get('/video/similar', { params });
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiStarVideo = videoId => request.post(`/video/like/${videoId}`);
export const apiUninterestedVideo = videoId => request.post(`/video/uninterested/${videoId}`);

export const apiGetHistoryVideo = async () => {
  const res = await request.get('/video/history');
  const grouped = res.data.data || {};
  const keys = Object.keys(grouped);
  for (const key of keys) {
    grouped[key] = await resolveVideoMediaUrls(grouped[key]);
  }
  res.data.data = grouped;
  return res;
};

export const apiSearchVideo = async (searchName, page = 1, limit = 10) => {
  const res = await request.get('/index/search', {
    params: {
      search: searchName,
      page,
      limit
    }
  });
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiGetAuditQueueState = () => request.get('/video/audit/queue/state');

export const apiAddHistory = id => request.post(`/video/history/${id}`);

export const apiGetVideoById = async id => {
  const res = await request.get(`/video/${id}`);
  res.data.data = await resolveSingleVideoMediaUrl(normalizeVideo(res.data.data));
  return res;
};

export const apiSetUserVideoModel = () => Promise.resolve({ data: { state: true } });

const buildCursorParams = lastTime => {
  const params = {};
  // 首屏不传游标，仅在游标是正整数时间戳时才下发 lastTime
  if (lastTime === undefined || lastTime === null || lastTime === '') {
    return params;
  }
  const parsed = Number(lastTime);
  if (Number.isFinite(parsed) && parsed > 0) {
    params.lastTime = parsed;
  }
  return params;
};

export const apiGetFollowVideo = async lastTime => {
  const params = buildCursorParams(lastTime);
  const res = await request.get('/video/follow/feed', { params });
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiInitFollowFeed = () => request.post('/video/init/follow/feed');

export const apiGetFriendShareVideo = async lastTime => {
  const params = buildCursorParams(lastTime);
  const res = await request.get('/video/share/friend/feed', { params });
  res.data.data = await resolveVideoMediaUrls(res.data.data);
  return res;
};

export const apiGetUserVideoById = async (userId, page = 1, limit = 10) => {
  const res = await request.get(`/video/user/open/${userId}`, {
    params: {
      page,
      limit
    }
  });
  const payload = res?.data?.data;
  if (Array.isArray(payload)) {
    const records = await resolveVideoMediaUrls(payload);
    res.data.data = {
      page,
      limit,
      total: records.length,
      pages: 1,
      records
    };
    return res;
  }

  const records = await resolveVideoMediaUrls(payload?.records || []);
  const total = Number(payload?.total || records.length || 0);
  const pageSize = Number(payload?.limit || limit || 10);
  res.data.data = {
    ...payload,
    page: Number(payload?.page || page || 1),
    limit: pageSize,
    total,
    pages: Math.max(1, Math.ceil(total / Math.max(pageSize, 1))),
    records
  };
  return res;
};

export const apiShareVideoToFriend = (videoId, friendUserId) =>
  request.post(`/video/share/friend/${videoId}/${friendUserId}`);

/**
 * 发布主评论
 */
export const apiPublishComment = (videoId, content) =>
  request.post('/video/comment/publish', {
    videoId,
    content,
    rootId: 0,
    parentId: 0
  });

/**
 * 回复评论
 */
export const apiReplyComment = (videoId, rootId, parentId, content) =>
  request.post('/video/comment/reply', {
    videoId,
    rootId,
    parentId,
    content
  });

/**
 * 查询评论列表
 * rootId=0 查询主评论，rootId>0 查询该主评论下回复
 */
export const apiListComments = (videoId, rootId = 0, page = 1, limit = 20) =>
  request.get('/video/comment/list', {
    params: { videoId, rootId, page, limit }
  });

/**
 * 查询视频评论总数
 */
export const apiCountComments = videoId =>
  request.get('/video/comment/count', {
    params: { videoId }
  });
