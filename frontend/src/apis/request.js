import axios from 'axios';
import { useUserStore } from '../stores';

const TOKEN_KEY = 'simple-tiktok:token';

const instance = axios.create({
  baseURL: '/api',
  timeout: 1000000,
  headers: {
    'Content-Type': 'application/json'
  }
});

instance.interceptors.request.use(config => {
  const userStore = useUserStore();
  const token = userStore.$state.token || sessionStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.token = token;
  }
  return config;
}, error => Promise.reject(error));

instance.interceptors.response.use(response => response, error => Promise.reject(error));

export default instance;