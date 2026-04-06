import request from '../request';

export const apiAuth = (type = 1, data) => {
  if (type) {
    return request.post('/user/login', {
      email: data.email,
      password: data.password
    });
  }
  return request.post('/user/register', {
    email: data.email,
    password: data.password,
    nickName: data.nickName,
    avatar: data.avatar,
    description: data.description,
    sex: data.sex
  });
};

export const apiForgetPassword = data => {
  return request.post('/login/findPassword', {
    email: data.email,
    code: data.code,
    newPassword: data.newPassword
  });
};

export const apiGetCode = (type = 1, data) => {
  if (type) {
    return request.getUri({ url: `/login/captcha.jpg/${data}` });
  }
  return request.post('/login/getCode', {
    uuid: data.uuid,
    code: data.code,
    email: data.email
  });
};

export const apiCheckCode = data => {
  return request.post('/login/check', {
    email: data.email,
    code: data.code
  });
};

export const apiGetCdnAuthFile = objectName => {
  return request.getUri({
    url: `/oss/url/${encodeURIComponent(objectName)}`
  });
};