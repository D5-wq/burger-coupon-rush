import { Navigate } from 'react-router-dom';
import { useAuth } from '../store/auth';
import Spinner from './ui/Spinner';

/** 인증이 필요한 라우트. 로딩 중엔 스피너, 미인증이면 로그인으로 보낸다. */
export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <Spinner />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}
