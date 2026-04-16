import request from '../request';

export const apiGetLike = (type = 'fans', userId = '', page = 1, limit = 10) => {
  const parsedUserId = Number(userId);
  const hasValidUserId = Number.isFinite(parsedUserId) && parsedUserId > 0;
  const params = {
    page,
    limit
  };
  if (hasValidUserId) {
    params.userId = parsedUserId;
  }
  return request.get(`/customer/${type}`, {
    params
  });
};

export const apiFollows = followsUserId => {
  return request.post('/customer/follows', null, {
    params: {
      followsUserId
    }
  });
};

export const apiIsFollowing = targetUserId => {
  return request.get('/customer/isFollowing', {
    params: {
      targetUserId
    }
  });
};
