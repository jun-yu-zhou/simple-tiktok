<template>
  <div>
    <v-card class="mx-auto" color="#36393f" max-width="650" theme="dark" variant="flat">
      <v-sheet color="#202225">
        <v-card-item>
          <template #prepend>
            <v-card-title>
              <v-icon icon="mdi-account" start></v-icon>
              {{ TEXT.registerTitle }}
            </v-card-title>
          </template>
        </v-card-item>
      </v-sheet>

      <v-card class="ma-4" color="#2f3136" rounded="lg" variant="flat">
        <v-card-item>
          <v-card-title class="text-body-2 d-flex align-center">
            <v-icon color="#949cf7" icon="mdi-step-forward" start></v-icon>
            <span class="text-medium-emphasis font-weight-bold">{{ stepData[step].title }}</span>
            <v-spacer></v-spacer>
            <v-chip class="ms-2 text-medium-emphasis" color="grey-darken-4" size="small" variant="flat">
              {{ step }}
            </v-chip>
          </v-card-title>
        </v-card-item>

        <v-divider></v-divider>

        <v-window v-model="step">
          <v-window-item :value="1">
            <v-form :disabled="isLoading" autocomplete="off">
              <v-card-text>
                <v-text-field
                  v-model="registerInfo.email"
                  :label="TEXT.email"
                  :hint="TEXT.emailHint"
                  :placeholder="TEXT.emailPlaceholder"
                ></v-text-field>
                <v-row no-gutters>
                  <v-col>
                    <v-img :src="captchaImg" class="captcha-img" @click="getCaptchaImg"></v-img>
                  </v-col>
                  <v-col>
                    <v-text-field
                      v-model="registerInfo.captchaCode"
                      :label="TEXT.captcha"
                      hide-details
                      :placeholder="TEXT.captchaPlaceholder"
                    ></v-text-field>
                  </v-col>
                </v-row>
                <v-btn color="blue" block variant="flat" :loading="isLoading" @click="getEmailCode">
                  {{ TEXT.getEmailCode }}
                </v-btn>
              </v-card-text>
              <v-otp-input v-model="registerInfo.code" placeholder="0"></v-otp-input>
            </v-form>
          </v-window-item>

          <v-window-item :value="2">
            <v-card-text>
              <v-text-field
                v-model="registerInfo.nickName"
                :label="TEXT.nickName"
                autocomplete="off"
              ></v-text-field>
              <v-text-field
                v-model="registerInfo.password"
                :label="TEXT.password"
                type="password"
                autocomplete="new-password"
              ></v-text-field>
              <v-text-field
                v-model="registerInfo.confirmPassword"
                :label="TEXT.confirmPassword"
                type="password"
                autocomplete="new-password"
              ></v-text-field>
              <span class="text-caption text-grey-darken-1">{{ TEXT.passwordHint }}</span>
            </v-card-text>
          </v-window-item>

          <v-window-item :value="3">
            <v-card-text>
              <div class="text-subtitle-1 font-weight-bold mb-2">{{ TEXT.interestTitle }}</div>
              <div class="text-caption text-grey mb-4">{{ TEXT.interestHint }}</div>
              <v-chip-group v-model="selectedTypeIds" column multiple>
                <v-chip
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :value="item.id"
                  :disabled="isLoading"
                  filter
                  variant="outlined"
                >
                  <template #prepend>
                    <v-avatar :image="item.image" :icon="item.icon || 'mdi-shape'" start></v-avatar>
                  </template>
                  {{ item.name }}
                </v-chip>
              </v-chip-group>
            </v-card-text>
          </v-window-item>
        </v-window>

        <v-card-actions>
          <v-btn v-if="step === 2" variant="text" :loading="isLoading" @click="step--">Back</v-btn>
          <v-spacer></v-spacer>
          <v-btn
            v-if="step < 3"
            color="blue"
            variant="flat"
            :loading="isLoading"
            @click="stepData[step].next ? stepData[step].next() : step++"
          >
            Next
          </v-btn>
          <v-btn
            v-else
            color="blue"
            variant="flat"
            :loading="isLoading"
            @click="submitCategories"
          >
            {{ TEXT.finishRegister }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { VOtpInput } from 'vuetify/labs/VOtpInput';
import { apiAuth, apiCheckCode, apiGetCode } from '../../apis/user/auth';
import { apiClassifyGetAll, apiClassifySubscribe } from '../../apis/classify';
import { useUserStore } from '../../stores';
import buildUtils from '../../utils/buildUtil';

const TEXT = {
  registerTitle: '\u6ce8\u518c\u8d26\u53f7',
  email: '\u90ae\u7bb1',
  emailHint: '\u90ae\u7bb1\u5fc5\u987b\u4e3a\u771f\u5b9e\u53ef\u7528\uff0c\u540e\u7eed\u7528\u4e8e\u767b\u5f55\u548c\u627e\u56de\u5bc6\u7801',
  emailPlaceholder: '\u8bf7\u8f93\u5165\u90ae\u7bb1',
  captcha: '\u56fe\u5f62\u9a8c\u8bc1\u7801',
  captchaPlaceholder: '\u8bf7\u8f93\u5165\u56fe\u5f62\u9a8c\u8bc1\u7801',
  getEmailCode: '\u83b7\u53d6\u90ae\u7bb1\u9a8c\u8bc1\u7801',
  nickName: '\u8bf7\u8f93\u5165\u6635\u79f0',
  password: '\u8bf7\u8f93\u5165\u5bc6\u7801',
  confirmPassword: '\u8bf7\u786e\u8ba4\u5bc6\u7801',
  passwordHint: '\u8bf7\u4e3a\u4f60\u7684\u8d26\u53f7\u8bbe\u7f6e\u4e00\u4e2a\u5bc6\u7801',
  stepEmail: '\u90ae\u7bb1\u9a8c\u8bc1',
  stepPassword: '\u8bbe\u7f6e\u5bc6\u7801',
  stepInterest: '\u5206\u7c7b\u8ba2\u9605',
  fillEmailCaptcha: '\u8bf7\u5148\u586b\u5199\u90ae\u7bb1\u548c\u56fe\u5f62\u9a8c\u8bc1\u7801',
  getEmailCodeFailed: '\u83b7\u53d6\u90ae\u7bb1\u9a8c\u8bc1\u7801\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
  fillEmailCode: '\u8bf7\u586b\u5199\u90ae\u7bb1\u548c\u90ae\u7bb1\u9a8c\u8bc1\u7801',
  invalidEmailCode: '\u90ae\u7bb1\u9a8c\u8bc1\u7801\u65e0\u6548',
  verifyFailed: '\u9a8c\u8bc1\u7801\u6821\u9a8c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
  fillNickPassword: '\u8bf7\u5b8c\u6574\u586b\u5199\u6635\u79f0\u548c\u5bc6\u7801',
  passwordNotMatch: '\u4e24\u6b21\u8f93\u5165\u5bc6\u7801\u4e0d\u4e00\u81f4',
  registerAndLoginSuccess: '\u6ce8\u518c\u6210\u529f\uff0c\u8bf7\u9009\u62e9\u611f\u5174\u8da3\u5206\u7c7b',
  registerSuccessNeedLogin: '\u6ce8\u518c\u6210\u529f\uff0c\u8bf7\u624b\u52a8\u767b\u5f55',
  registerFailed: '\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
  interestTitle: '\u9009\u62e9\u611f\u5174\u8da3\u5206\u7c7b',
  interestHint: '\u81f3\u5c11\u9009\u62e9\u4e00\u4e2a\u5206\u7c7b\uff0c\u7528\u4e8e\u521d\u59cb\u5316\u4f60\u7684\u63a8\u8350\u6a21\u578b',
  finishRegister: '\u5b8c\u6210\u6ce8\u518c',
  noInterestSelected: '\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u4e2a\u5206\u7c7b',
  loadCategoryFailed: '\u5206\u7c7b\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
  initInterestFailed: '\u8ba2\u9605\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
  registerDone: '\u6ce8\u518c\u5b8c\u6210\uff0c\u5df2\u4e3a\u4f60\u521d\u59cb\u5316\u63a8\u8350\u6a21\u578b'
};

const step = ref(1);
const stepData = ref({
  1: {
    title: TEXT.stepEmail,
    next: () => verifyCode()
  },
  2: {
    title: TEXT.stepPassword,
    next: () => setPassword()
  },
  3: {
    title: TEXT.stepInterest
  }
});

const { showMessage, closeEvent } = defineProps({
  showMessage: {
    type: Function,
    default: () => {}
  },
  closeEvent: {
    type: Function,
    default: () => {}
  }
});

const isLoading = ref(false);
const captchaImg = ref('');
const loginToken = ref('');
const TOKEN_KEY = 'simple-tiktok:token';
const userStore = useUserStore();
const categoryOptions = ref([]);
const selectedTypeIds = ref([]);
const registerInfo = reactive({
  email: '',
  nickName: '',
  code: '',
  captchaCode: '',
  password: '',
  confirmPassword: '',
  uuid: ''
});

const getCaptchaImg = () => {
  registerInfo.uuid = buildUtils.guid();
  captchaImg.value = apiGetCode(1, registerInfo.uuid);
};

getCaptchaImg();

const getEmailCode = async () => {
  if (!registerInfo.email || !registerInfo.captchaCode) {
    showMessage(TEXT.fillEmailCaptcha, 'error');
    return;
  }
  isLoading.value = true;
  try {
    const { data } = await apiGetCode(0, { ...registerInfo, code: registerInfo.captchaCode });
    showMessage(data.message, data.state ? 'success' : 'error');
    if (!data.state) {
      getCaptchaImg();
    }
  } catch {
    showMessage(TEXT.getEmailCodeFailed, 'error');
    getCaptchaImg();
  } finally {
    isLoading.value = false;
  }
};

const verifyCode = async () => {
  if (!registerInfo.email || !registerInfo.code) {
    showMessage(TEXT.fillEmailCode, 'error');
    return;
  }
  isLoading.value = true;
  try {
    const { data } = await apiCheckCode({ ...registerInfo });
    if (!data.state) {
      showMessage(TEXT.invalidEmailCode, 'error');
      getCaptchaImg();
      return;
    }
    registerInfo.nickName = '';
    registerInfo.password = '';
    registerInfo.confirmPassword = '';
    step.value = 2;
  } catch {
    showMessage(TEXT.verifyFailed, 'error');
  } finally {
    isLoading.value = false;
  }
};

const setPassword = async () => {
  if (!registerInfo.nickName || !registerInfo.password || !registerInfo.confirmPassword) {
    showMessage(TEXT.fillNickPassword, 'error');
    return;
  }
  if (registerInfo.password !== registerInfo.confirmPassword) {
    showMessage(TEXT.passwordNotMatch, 'error');
    return;
  }
  isLoading.value = true;
  try {
    const registerRes = await apiAuth(0, registerInfo);
    if (!registerRes.data.state) {
      showMessage(registerRes.data.message, 'error');
      return;
    }

    const loginRes = await apiAuth(1, {
      email: registerInfo.email,
      password: registerInfo.password
    });
    if (loginRes.data.state && loginRes.data.data?.token) {
      loginToken.value = loginRes.data.data.token;
      userStore.$patch({
        token: loginToken.value,
        info: loginRes.data.data.user || {}
      });
      sessionStorage.setItem(TOKEN_KEY, loginToken.value);
      await loadCategoryOptions();
      selectedTypeIds.value = [];
      showMessage(TEXT.registerAndLoginSuccess, 'success');
      step.value = 3;
      return;
    }

    showMessage(TEXT.registerSuccessNeedLogin, 'warning');
  } catch (error) {
    showMessage(error?.message || TEXT.registerFailed, 'error');
  } finally {
    isLoading.value = false;
  }
};

const loadCategoryOptions = async () => {
  try {
    const { data } = await apiClassifyGetAll();
    if (data?.state && Array.isArray(data.data)) {
      categoryOptions.value = data.data;
      return;
    }
  } catch {
    // ignore
  }
  categoryOptions.value = [];
  throw new Error(TEXT.loadCategoryFailed);
};

const submitCategories = async () => {
  if (!selectedTypeIds.value.length) {
    showMessage(TEXT.noInterestSelected, 'warning');
    return;
  }
  const currentToken = loginToken.value || userStore.$state.token || sessionStorage.getItem(TOKEN_KEY);
  if (!currentToken) {
    showMessage(TEXT.registerSuccessNeedLogin, 'warning');
    return;
  }
  isLoading.value = true;
  try {
    const { data } = await apiClassifySubscribe(selectedTypeIds.value, currentToken);
    if (!data?.state) {
      showMessage(data?.message || TEXT.initInterestFailed, 'error');
      return;
    }
    window.dispatchEvent(new CustomEvent('classify-updated'));
    showMessage(TEXT.registerDone, 'success');
    closeEvent({ info: {}, token: currentToken });
  } catch {
    showMessage(TEXT.initInterestFailed, 'error');
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.captcha-img {
  cursor: pointer;
  min-height: 56px;
}
</style>
