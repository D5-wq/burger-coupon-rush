import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import MenuPage from './pages/MenuPage';
import OrdersPage from './pages/OrdersPage';
import LoginPage from './pages/LoginPage';
import SignUpPage from './pages/SignUpPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<MenuPage />} />
        <Route
          path="orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* 인증 페이지는 헤더 없이 단독 레이아웃 */}
      <Route path="/login" element={<AuthPage>{<LoginPage />}</AuthPage>} />
      <Route path="/signup" element={<AuthPage>{<SignUpPage />}</AuthPage>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function AuthPage({ children }: { children: React.ReactNode }) {
  return <div className="mx-auto min-h-full max-w-md px-4 py-6">{children}</div>;
}
