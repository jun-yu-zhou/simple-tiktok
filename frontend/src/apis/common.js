import request from './request';

export const apiCommonSearch = name => {
  return request.get('/index/search', {
    params: {
      search: name
    }
  });
};