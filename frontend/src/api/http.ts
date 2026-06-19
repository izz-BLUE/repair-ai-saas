import axios from 'axios';
import { message } from 'antd';

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
      message.error(apiResponse.message || '请求失败');
      return Promise.reject(new Error(apiResponse.message));
    }
    // 非标准响应直接返回
    return apiResponse;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/admin/login';
      return Promise.reject(error);
    }
    const msg = error.response?.data?.message || error.message || '网络错误';
    message.error(msg);
    return Promise.reject(error);
  },
);

export default http;
