import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../store/cart';
import { useAuth } from '../store/auth';
import { orderApi } from '../api/order';
import { ApiError, OrderCreateRequest } from '../types';
import { formatWon } from '../lib/format';
import ProductImage from '../components/ProductImage';
import Modal from '../components/ui/Modal';
import Button from '../components/ui/Button';

export default function CartPage() {
  const cart = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loginPrompt, setLoginPrompt] = useState(false);
  const [successOrderId, setSuccessOrderId] = useState<number | null>(null);

  const placeOrder = async () => {
    if (!isAuthenticated) {
      setLoginPrompt(true);
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const body: OrderCreateRequest = {
        items: cart.lines.map((l) => ({
          productId: l.productId,
          quantity: l.quantity,
          options: l.options.map((o) => ({
            optionItemId: o.optionItemId,
            quantity: o.quantity,
          })),
        })),
      };
      const order = await orderApi.create(body);
      cart.clear();
      setSuccessOrderId(order.id);
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (cart.lines.length === 0 && successOrderId === null) {
    return (
      <div className="mt-16 flex flex-col items-center text-center">
        <div className="text-5xl">🛒</div>
        <p className="mt-4 font-bold">장바구니가 비어 있어요</p>
        <p className="mt-1 text-sm text-ink-faint">메뉴에서 버거를 골라 담아보세요!</p>
        <Link to="/" className="mt-5">
          <Button>메뉴 보러 가기</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <h1 className="px-1 text-lg font-extrabold">장바구니</h1>

      {error && (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>
      )}

      {cart.lines.map((line) => (
        <div key={line.key} className="card p-4">
          <div className="flex gap-3">
            <ProductImage
              src={line.imageUrl}
              name={line.productName}
              className="h-16 w-16 shrink-0 rounded-lg"
            />
            <div className="min-w-0 flex-1">
              <div className="flex items-start justify-between gap-2">
                <h3 className="text-[15px] font-bold leading-tight">{line.productName}</h3>
                <button
                  onClick={() => cart.remove(line.key)}
                  className="shrink-0 text-xs font-semibold text-ink-faint hover:text-red-500"
                >
                  삭제
                </button>
              </div>

              {/* 선택 옵션 요약 */}
              {line.options.length > 0 && (
                <ul className="mt-1 space-y-0.5">
                  {line.options.map((o) => (
                    <li key={`${line.key}-${o.optionItemId}`} className="text-[12px] text-ink-faint">
                      · {o.groupName}: {o.itemName}
                      {o.quantity > 1 && ` ×${o.quantity}`}
                      {o.extraPrice > 0 && ` (+${formatWon(o.extraPrice * o.quantity)})`}
                    </li>
                  ))}
                </ul>
              )}

              <div className="mt-3 flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <Step
                    label="빼기"
                    onClick={() => cart.setQuantity(line.key, line.quantity - 1)}
                  >
                    −
                  </Step>
                  <span className="w-5 text-center text-sm font-bold tabular-nums">
                    {line.quantity}
                  </span>
                  <Step
                    label="더하기"
                    onClick={() => cart.setQuantity(line.key, line.quantity + 1)}
                  >
                    +
                  </Step>
                </div>
                <span className="text-[15px] font-extrabold tabular-nums text-brand">
                  {formatWon(line.lineTotal)}
                </span>
              </div>
            </div>
          </div>
        </div>
      ))}

      {/* 합계 */}
      <div className="card p-4">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">합계 ({cart.count}개)</span>
          <span className="text-lg font-extrabold text-brand">{formatWon(cart.total)}</span>
        </div>
      </div>

      {/* 하단 주문 바 */}
      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-md p-4">
        <button
          onClick={placeOrder}
          disabled={submitting}
          className="flex w-full items-center justify-between rounded-xl2 bg-brand px-5 py-4 text-white shadow-lg transition hover:bg-brand-600 disabled:opacity-70"
        >
          <span className="text-sm font-semibold">
            {submitting ? '주문 중…' : '주문하기'}
          </span>
          <span className="text-base font-extrabold">{formatWon(cart.total)}</span>
        </button>
      </div>

      {/* 로그인 유도 */}
      <Modal open={loginPrompt} onClose={() => setLoginPrompt(false)}>
        <div className="text-center">
          <div className="mb-3 text-4xl">🔒</div>
          <h3 className="text-lg font-extrabold">로그인이 필요해요</h3>
          <p className="mt-2 text-sm text-ink-soft">주문하려면 먼저 로그인해 주세요.</p>
          <Button className="mt-5" fullWidth onClick={() => navigate('/login')}>
            로그인하러 가기
          </Button>
        </div>
      </Modal>

      {/* 주문 성공 */}
      <Modal open={successOrderId !== null} onClose={() => navigate('/orders')}>
        <div className="text-center">
          <div className="mb-3 text-4xl">✅</div>
          <h3 className="text-lg font-extrabold">주문이 완료됐어요!</h3>
          <p className="mt-2 text-sm text-ink-soft">맛있는 버거가 곧 준비됩니다 🍔</p>
          <div className="mt-5 flex gap-2">
            <Button variant="secondary" fullWidth onClick={() => navigate('/')}>
              계속 둘러보기
            </Button>
            <Button fullWidth onClick={() => navigate('/orders')}>
              내 주문 보기
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

function Step({
  children,
  onClick,
  label,
}: {
  children: React.ReactNode;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      aria-label={label}
      onClick={onClick}
      className="flex h-8 w-8 items-center justify-center rounded-full bg-brand text-lg font-bold leading-none text-white transition hover:bg-brand-600"
    >
      {children}
    </button>
  );
}
