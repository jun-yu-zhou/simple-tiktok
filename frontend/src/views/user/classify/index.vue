<template>
  <v-container>
    <MyClassify :classify-data="myClassifyList" :close-event="removeSubscribe" :disabled="saving" />
    <AllClassify class="mt-8" :classify-data="allClassifyList" :close-event="addSubscribe" :disabled="saving" />

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
      <template #actions>
        <v-btn color="blue" variant="text" @click="snackbar.show = false">关闭</v-btn>
      </template>
    </v-snackbar>
  </v-container>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { apiClassifyGetAll, apiClassifySubscribe, apiGetClassifyByUser, apiGetNoSubscribe } from '../../../apis/classify';
import AllClassify from './all.vue';
import MyClassify from './my.vue';

const allClassifyList = ref([]);
const myClassifyList = ref([]);
const saving = ref(false);
const snackbar = ref({
  show: false,
  text: '',
  color: 'success'
});

const showMsg = (text, ok = true) => {
  snackbar.value = {
    show: true,
    text,
    color: ok ? 'success' : 'error'
  };
};

const extractTypeId = item => {
  if (item == null) {
    return null;
  }
  if (typeof item === 'number') {
    return item;
  }
  const value = item.id ?? item.typeId;
  if (value == null) {
    return null;
  }
  const id = Number(value);
  return Number.isFinite(id) ? id : null;
};

const refreshData = async () => {
  const [allTypeResp, myResp, noSubscribeResp] = await Promise.all([
    apiClassifyGetAll(),
    apiGetClassifyByUser(),
    apiGetNoSubscribe()
  ]);

  const allTypes = allTypeResp?.data?.state && Array.isArray(allTypeResp.data.data)
    ? allTypeResp.data.data
    : [];
  const typeMap = new Map(allTypes.map(item => [item.id, item]));

  const subscribeIds = myResp?.data?.state && Array.isArray(myResp.data.data)
    ? myResp.data.data.map(extractTypeId).filter(id => id != null)
    : [];
  myClassifyList.value = subscribeIds
    .map(id => typeMap.get(id))
    .filter(Boolean);

  if (noSubscribeResp?.data?.state && Array.isArray(noSubscribeResp.data.data)) {
    allClassifyList.value = noSubscribeResp.data.data;
  } else {
    allClassifyList.value = allTypes.filter(item => !subscribeIds.includes(item.id));
  }
};

const updateSubscribe = async nextIds => {
  if (saving.value) return;
  saving.value = true;
  try {
    const { data } = await apiClassifySubscribe(nextIds);
    if (!data?.state) {
      showMsg(data?.message || '订阅更新失败', false);
      return;
    }
    await refreshData();
    window.dispatchEvent(new CustomEvent('classify-updated'));
    showMsg(data?.message || '订阅更新成功', true);
  } catch (_e) {
    showMsg('订阅更新失败', false);
  } finally {
    saving.value = false;
  }
};

const addSubscribe = item => {
  const currentIds = myClassifyList.value.map(extractTypeId).filter(id => id != null);
  const addId = extractTypeId(item);
  if (!addId || currentIds.includes(addId)) {
    return;
  }
  updateSubscribe([...currentIds, addId]);
};

const removeSubscribe = index => {
  if (index < 0 || index >= myClassifyList.value.length) {
    return;
  }
  const currentIds = myClassifyList.value.map(extractTypeId).filter(id => id != null);
  currentIds.splice(index, 1);
  updateSubscribe(currentIds);
};

onMounted(() => {
  refreshData();
});
</script>
