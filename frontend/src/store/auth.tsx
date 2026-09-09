import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authApi } from '../api/auth';
import { tokenStore } from '../api/client';
import { mockApi } from '../api/mock';
import { IS_DEMO } from '../lib/env';
import { UserResponse } from '../types';

interface AuthContextValue {
  user: UserResponse | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string, name: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  // 새로고침 시 토큰이 있으면 내 정보를 복원한다.
  useEffect(() => {
    const token = tokenStore.get();
    if (!token) {
      setLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authApi.login({ email, password });
    tokenStore.set(accessToken);
    const me = await authApi.me();
    setUser(me);
  }, []);

  const signUp = useCallback(
    async (email: string, password: string, name: string) => {
      await authApi.signUp({ email, password, name });
      await login(email, password);
    },
    [login],
  );

  const logout = useCallback(() => {
    tokenStore.clear();
    if (IS_DEMO) mockApi.logout();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: !!user, loading, login, signUp, logout }),
    [user, loading, login, signUp, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
