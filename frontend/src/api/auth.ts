import { http } from './client';
import { TokenResponse, UserResponse } from '../types';

export const authApi = {
  signUp: (body: { email: string; password: string; name: string }) =>
    http.post<UserResponse>('/auth/signup', body),

  login: (body: { email: string; password: string }) =>
    http.post<TokenResponse>('/auth/login', body),

  me: () => http.get<UserResponse>('/users/me'),
};
