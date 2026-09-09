import { http } from './client';
import { TokenResponse, UserResponse } from '../types';
import { IS_DEMO } from '../lib/env';
import { mockApi } from './mock';

export const authApi = {
  signUp: (body: { email: string; password: string; name: string }) =>
    IS_DEMO ? mockApi.signUp(body) : http.post<UserResponse>('/auth/signup', body),

  login: (body: { email: string; password: string }) =>
    IS_DEMO ? mockApi.login(body) : http.post<TokenResponse>('/auth/login', body),

  me: () => (IS_DEMO ? mockApi.me() : http.get<UserResponse>('/users/me')),
};
