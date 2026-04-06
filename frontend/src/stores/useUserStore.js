import { defineStore } from 'pinia';

const TOKEN_KEY = 'simple-tiktok:token';

export default defineStore('user', {
  state: () => ({
    info: {},
    token: sessionStorage.getItem(TOKEN_KEY),
    lookId: null
  })
});