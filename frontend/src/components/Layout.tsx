import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/auth';
import { cn } from '../lib/cn';
import { IS_DEMO } from '../lib/env';

export default function Layout() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="mx-auto flex min-h-full max-w-md flex-col bg-[#FAFAF8]">
      <header className="sticky top-0 z-40 border-b border-black/5 bg-[#FAFAF8]/90 backdrop-blur">
        <div className="flex h-14 items-center justify-between px-4">
          <Link to="/" className="flex items-center gap-1.5 text-[17px] font-extrabold">
            <span>🍔</span>
            <span>
              Burger<span className="text-brand">Rush</span>
            </span>
            {IS_DEMO && (
              <span className="ml-1 rounded-md bg-brand-50 px-1.5 py-0.5 text-[10px] font-bold text-brand-600">
                DEMO
              </span>
            )}
          </Link>

          <nav className="flex items-center gap-1 text-sm font-semibold">
            <TopLink to="/">메뉴</TopLink>
            {isAuthenticated && <TopLink to="/orders">내 주문</TopLink>}
            {isAuthenticated ? (
              <button
                onClick={handleLogout}
                className="ml-1 rounded-lg px-3 py-1.5 text-ink-soft transition hover:bg-black/[0.04]"
                title={user?.name}
              >
                로그아웃
              </button>
            ) : (
              <Link
                to="/login"
                className="ml-1 rounded-lg bg-brand px-3.5 py-1.5 text-white transition hover:bg-brand-600"
              >
                로그인
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className="flex-1 px-4 pb-28 pt-4">
        <Outlet />
      </main>
    </div>
  );
}

function TopLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        cn(
          'rounded-lg px-3 py-1.5 transition',
          isActive ? 'text-brand' : 'text-ink-soft hover:bg-black/[0.04]',
        )
      }
    >
      {children}
    </NavLink>
  );
}
