import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/auth';
import { useCart } from '../store/cart';
import { cn } from '../lib/cn';
import { IS_DEMO } from '../lib/env';

export default function Layout() {
  const { isAuthenticated, user, logout } = useAuth();
  const { count } = useCart();
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

          <nav className="flex items-center gap-0.5 whitespace-nowrap text-sm font-semibold">
            <TopLink to="/">메뉴</TopLink>
            {isAuthenticated && <TopLink to="/orders">주문</TopLink>}
            <Link
              to="/cart"
              aria-label="장바구니"
              className="relative rounded-lg px-2 py-1.5 text-ink-soft transition hover:bg-black/[0.04]"
            >
              🛒
              {count > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-brand px-1 text-[10px] font-bold text-white">
                  {count}
                </span>
              )}
            </Link>
            {isAuthenticated ? (
              <button
                onClick={handleLogout}
                className="rounded-lg px-2.5 py-1.5 text-ink-soft transition hover:bg-black/[0.04]"
                title={user?.name}
              >
                로그아웃
              </button>
            ) : (
              <Link
                to="/login"
                className="rounded-lg bg-brand px-3 py-1.5 text-white transition hover:bg-brand-600"
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
          'rounded-lg px-2.5 py-1.5 transition',
          isActive ? 'text-brand' : 'text-ink-soft hover:bg-black/[0.04]',
        )
      }
    >
      {children}
    </NavLink>
  );
}
