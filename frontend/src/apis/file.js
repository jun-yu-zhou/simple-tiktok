import request from './request';

const OSS_URL_CACHE = new Map();

const isHttpUrl = value => typeof value === 'string' && /^https?:\/\//i.test(value);

const cacheUrl = (objectName, url) => {
  if (!objectName || !url) {
    return;
  }
  OSS_URL_CACHE.set(objectName, url);
};

export const apiUploadFile = async (file, callBack = { next: () => {}, error: () => {}, complete: () => {} }) => {
  try {
    callBack.next({ total: { percent: 0 } });
    const formData = new FormData();
    formData.append('file', file);
    const resp = await request.post('/oss/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: evt => {
        const percent = evt.total ? (evt.loaded / evt.total) * 100 : 0;
        callBack.next({ total: { percent } });
      }
    });

    const payload = resp?.data?.data || {};
    cacheUrl(payload.objectName, payload.url);

    callBack.next({ total: { percent: 100 } });
    callBack.complete(
      { key: payload.objectName, ...payload },
      {
        state: true,
        code: 200,
        message: '上传成功',
        data: payload.objectName,
        payload
      }
    );
    return { unsubscribe: () => {} };
  } catch (e) {
    callBack.error(e?.message || e);
    throw e;
  }
};

export const apiFileGet = objectName => {
  if (!objectName) {
    return '';
  }
  if (isHttpUrl(objectName)) {
    return objectName;
  }
  return OSS_URL_CACHE.get(objectName) || '';
};

export const apiGetFilePublicUrl = async objectName => {
  if (!objectName) {
    return '';
  }
  if (isHttpUrl(objectName)) {
    return objectName;
  }
  const resp = await request.get(`/oss/url/${encodeURIComponent(objectName)}`);
  const url = resp?.data?.data || '';
  cacheUrl(objectName, url);
  return url;
};
