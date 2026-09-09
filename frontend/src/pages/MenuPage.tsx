import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productApi } from '../api/product';
import { orderApi } from '../api/order';
import { Product, ApiError } from '../types';
import { useAuth } from '../store/auth';
import { formatWon } from '../lib/format';
import ProductCard from '../components/ProductCard';
import CouponBanner from '../components/CouponBanner';
import Modal from '../components/ui/Modal';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';

type Quantities = Record<number, number>;

export default function MenuPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [qty, setQty] = useState<Quantities>({});

  const [couponOpen, setCouponOpen] = useState(false);
  const [orderState, setOrderState] = useState<'idle' | 'submitting'>('idle');
  const [successOrderId, setSuccessOrderId] = useState<number | null>(null);
  const [loginPrompt, setLoginPrompt] = useState(false);

  useEffect(() => {
    productApi
      .list()
      .then(setProducts)
      .catch((e: ApiError) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const setQuantity = (id: number, delta: number) =>
    setQty((prev) => {
      const next = Math.max(0, (prev[id] ?? 0) + delta);
      const updated = { ...prev, [id]: next };
      if (next === 0) delete updated[id];
      return updated;
    });

  const { count, total } = useMemo(() => {
    let c = 0;
    let t = 0;
    for (const p of products) {
      const q = qty[p.id] ?? 0;
      c += q;
      t += q * p.price;
    }
    return { count: c, total: t };
  }, [products, qty]);

  const submitOrder = async () => {
    if (!isAuthenticated) {
      setLoginPrompt(true);
      return;
    }
    setOrderState('submitting');
    try {
      const items = Object.entries(qty).map(([productId, quantity]) => ({
        productId: Number(productId),
        quantity,
      }));
      const order = await orderApi.create({ items });
      setQty({});
      setSuccessOrderId(order.id);
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setOrderState('idle');
    }
  };

  if (loading) return <Spinner label="메뉴를 불러오는 중…" />;

  return (
    <div className="space-y-5">
      <CouponBanner onClick={() => setCouponOpen(true)} />

      <div>
        <h1 className="mb-3 px-1 text-lg font-extrabold">인기 버거</h1>
        {error && (
          <p className="mb-3 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>
        )}
        <div className="grid grid-cols-2 gap-3">
          {products.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              quantity={qty[p.id] ?? 0}
              onIncrease={() => setQuantity(p.id, +1)}
              onDecrease={() => setQuantity(p.id, -1)}
            />
          ))}
        </div>
      </div>

      {/* 하단 주문 바 */}
      {count > 0 && (
        <div className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-md p-4">
          <button
            onClick={submitOrder}
            disabled={orderState === 'submitting'}
            className="flex w-full items-center justify-between rounded-xl2 bg-brand px-5 py-4 text-white shadow-lg
              transition hover:bg-brand-600 disabled:opacity-70"
          >
            <span className="flex items-center gap-2 text-sm font-semibold">
              <span className="flex h-6 min-w-6 items-center justify-center rounded-full bg-white/25 px-1.5 text-xs">
                {count}
              </span>
              주문하기
            </span>
            <span className="text-base font-extrabold">{formatWon(total)}</span>
          </button>
        </div>
      )}

      {/* 쿠폰 배너 클릭 (Step 3~5 에서 실제 발급 연동) */}
      <Modal open={couponOpen} onClose={() => setCouponOpen(false)}>
        <div className="text-center">
          <div className="mb-3 text-4xl">🎟️</div>
          <h3 className="text-lg font-extrabold">선착순 쿠폰 이벤트</h3>
          <p className="mt-2 text-sm text-ink-soft">
            쿠폰 발급 기능은 곧 오픈됩니다. (동시성 제어 로직 구현 예정)
          </p>
          <Button className="mt-5" fullWidth onClick={() => setCouponOpen(false)}>
            확인
          </Button>
        </div>
      </Modal>

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
      <Modal open={successOrderId !== null} onClose={() => setSuccessOrderId(null)}>
        <div className="text-center">
          <div className="mb-3 text-4xl">✅</div>
          <h3 className="text-lg font-extrabold">주문이 완료됐어요!</h3>
          <p className="mt-2 text-sm text-ink-soft">맛있는 버거가 곧 준비됩니다 🍔</p>
          <div className="mt-5 flex gap-2">
            <Button variant="secondary" fullWidth onClick={() => setSuccessOrderId(null)}>
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
