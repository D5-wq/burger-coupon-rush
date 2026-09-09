import axios, { AxiosError } from 'axios';
import { ApiEnvelope, ApiError } from '../types';

const TOKEN_KEY = 'bcr_access_token';

export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
});

// 요청마다 JWT 를 Authorization 헤더로 자동 첨부
api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 공통 응답 언랩:
 * - 성공: envelope.data 만 반환
 * - 실패: 백엔드 error(code/message) 를 ApiError 로 변환해 throw
 */
async function request<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  try {
    const res = await promise;
    const body = res.data;
    if (!body.success || body.data === null) {
      const err = body.error;
      throw new ApiError(err?.code ?? 'UNKNOWN', err?.message ?? '알 수 없는 오류', 200);
    }
    return body.data;
  } catch (e) {
    if (e instanceof ApiError) throw e;
    const axErr = e as AxiosError<ApiEnvelope<unknown>>;
    const status = axErr.response?.status ?? 0;
    if (status === 401) {
      tokenStore.clear();
    }
    const body = axErr.response?.data;
    const code = body?.error?.code ?? 'NETWORK_ERROR';
    const message = body?.error?.message ?? (status === 0 ? '서버에 연결할 수 없습니다.' : '요청 처리 중 오류가 발생했습니다.');
    throw new ApiError(code, message, status);
  }
}

export const http = {
  get: <T>(url: string) => request<T>(api.get(url)),
  post: <T>(url: string, body?: unknown) => request<T>(api.post(url, body)),
  put: <T>(url: string, body?: unknown) => request<T>(api.put(url, body)),
  del: <T>(url: string) => request<T>(api.delete(url)),
};

export default api;
