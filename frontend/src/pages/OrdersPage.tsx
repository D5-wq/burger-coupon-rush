import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { orderApi } from '../api/order';
import { Order, ApiError } from '../types';
import { formatWon } from '../lib/format';
import Spinner from '../components/ui/Spinner';
import Button from '../components/ui/Button';

const statusLabel: Record<string, string> = {
  CREATED: '주문 접수',
  PAID: '결제 완료',
  CANCELED: '취소됨',
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    orderApi
      .myOrders()
      .then(setOrders)
      .catch((e: ApiError) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner label="주문 내역을 불러오는 중…" />;

  if (error) {
    return <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>;
  }

  if (orders.length === 0) {
    return (
      <div className="mt-16 flex flex-col items-center text-center">
        <div className="text-5xl">🧾</div>
        <p className="mt-4 font-bold">아직 주문 내역이 없어요</p>
        <p className="mt-1 text-sm text-ink-faint">메뉴에서 마음에 드는 버거를 골라보세요!</p>
        <Link to="/" className="mt-5">
          <Button>메뉴 보러 가기</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <h1 className="px-1 text-lg font-extrabold">내 주문</h1>
      {orders.map((order) => (
        <div key={order.id} className="card p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-faint">
              #{order.id} · {new Date(order.createdAt).toLocaleDateString('ko-KR')}
            </span>
            <span className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-600">
              {statusLabel[order.status] ?? order.status}
            </span>
          </div>

          <ul className="mt-3 space-y-1">
            {order.items.map((it) => (
              <li key={it.productId} className="flex justify-between text-sm">
                <span className="text-ink-soft">
                  {it.productName} <span className="text-ink-faint">×{it.quantity}</span>
                </span>
                <span className="tabular-nums">{formatWon(it.lineTotal)}</span>
              </li>
            ))}
          </ul>

          <div className="mt-3 flex items-center justify-between border-t border-black/5 pt-3">
            <span className="text-sm font-semibold">합계</span>
            <span className="text-base font-extrabold text-brand">{formatWon(order.finalPrice)}</span>
          </div>
        </div>
      ))}
    </div>
  );
}
