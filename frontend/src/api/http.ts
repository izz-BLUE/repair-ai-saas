import axios from 'axios';
import type { MessageInstance } from 'antd/es/message/interface';

// 由 App 组件挂载时注入，避免静态 message 的 context 警告
let messageApi: MessageInstance | null = null;

export const setMessageApi = (api: MessageInstance) => {
  messageApi = api;
};

const showError = (msg: string) => {
  if (messageApi) {
    messageApi.error(msg);
  } else {
    // fallback: 兜底用 console，避免白屏
    console.error('[http]', msg);
  }
};

const http = axios.create({
  timeout: 30000,
});

// 请求拦截：自动携带 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截：401 跳转登录，解包 ApiResponse
http.interceptors.response.use(
  (response) => {
    const apiResponse = response.data;
    // 后端返回 { code, message, data }
    if (apiResponse && apiResponse.code !== undefined) {
      if (apiResponse.code === 'SUCCESS') {
        return apiResponse.data;
      }
      // 业务错误
      showError(apiResponse.message || '请求失败');
      return Promise.reject(new Error(apiResponse.message));
    }
    // 非标准响应直接返回
    return apiResponse;
  },
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || '';
      const isLoginRequest = requestUrl.includes('/api/public/login');
      const isOnLoginPage = window.location.pathname.includes('/login');

      if (!isLoginRequest) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (!isOnLoginPage) {
          window.location.href = '/admin/login';
        }
      }

      // 登录 401 和已在登录页面上的 401：显示错误信息，不跳转
      const msg = error.response?.data?.message || error.message || '网络错误';
      showError(msg);
      return Promise.reject(error);
    }
    const msg = error.response?.data?.message || error.message || '网络错误';
    showError(msg);
    return Promise.reject(error);
  },
);

export default http;
