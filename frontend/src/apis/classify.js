import request from './request';

export const apiClassifyGetAll = () => {
  return request.get('/type/list');
};

export const apiClassifyGetById = id => {
  return request.get(`/type/${id}`);
};

export const apiGetClassifyByUser = () => {
  return request.get('/type/subscribe');
};

export const apiClassifySubscribe = (ids, token) => {
  const values = Array.isArray(ids) ? ids : [ids];
  const types = values.filter(v => v !== undefined && v !== null).join(',');
  return request.post('/type/subscribe/batch', null, {
    params: { types },
    headers: token ? { token } : undefined
  });
};

export const apiGetNoSubscribe = () => {
  return request.get('/type/noSubscribe');
};

export const apiClassifySubscribeOne = typeId => {
  return request.post(`/type/subscribe/${typeId}`);
};

export const apiClassifyUnsubscribeOne = typeId => {
  return request.post(`/type/unsubscribe/${typeId}`);
};
