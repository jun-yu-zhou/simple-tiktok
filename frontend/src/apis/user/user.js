import request from '../request';

export const apiGetUserInfo = (userId = '') => {
  const parsedUserId = Number(userId);
  if (!Number.isFinite(parsedUserId) || parsedUserId <= 0) {
    return request.get('/customer/getInfo');
  }
  return request.get(`/customer/getInfo/${parsedUserId}`);
};

export const apiChangeUserInfo = info => {
  return request.put('/user/profile', info);
};

export const apiGetUserSearchHistory = () => {
  return request.get('/index/search/history');
};

export const apiClearUserSearchHistory = () => {
  return request.delete('/index/search/history');
};
