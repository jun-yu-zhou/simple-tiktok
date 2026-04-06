import { createPinia } from 'pinia';
import videojs from 'video.js';
import 'video.js/dist/video-js.css';
import { createApp } from 'vue';
import App from './App.vue';
import vuetify from './plugins/vuetify';
import router from './router';

const simpleTikTokApp = createApp(App);
simpleTikTokApp.use(createPinia());
simpleTikTokApp.use(router);
simpleTikTokApp.use(vuetify);
simpleTikTokApp.config.globalProperties.$video = videojs;
simpleTikTokApp.mount('#app');