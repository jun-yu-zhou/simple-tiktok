import request from '../request';

export const apiVideoPush = videoInfo => {
  if (!videoInfo) {
    return Promise.reject(new Error('videoInfo is required'));
  }
  const labels = Array.isArray(videoInfo.labelNames)
    ? videoInfo.labelNames
    : String(videoInfo.labelNames || '').split(',').filter(Boolean);
  const payload = videoInfo.id
    ? {
        id: videoInfo.id,
        labels,
        typeId: videoInfo.typeId
      }
    : {
        id: videoInfo.id,
        caption: videoInfo.caption || videoInfo.description || videoInfo.title || '',
        videoFileName: videoInfo.videoFileName || videoInfo.url || '',
        coverFileName: videoInfo.coverFileName || videoInfo.cover || '',
        labels,
        open: videoInfo.open,
        duration: videoInfo.duration,
        typeId: videoInfo.typeId
      };
  return request.post('/video/save', payload);
};

export const apiRemoveVideo = id => {
  return request.delete(`/video/${id}`);
};
