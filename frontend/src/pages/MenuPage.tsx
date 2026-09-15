import { useEffect, useState } from 'react';
import { productApi } from '../api/product';
import { Product, ApiError } from '../types';
import ProductCard from '../components/ProductCard';
import CouponBanner from '../components/CouponBanner';
import Modal from '../components/ui/Modal';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';

export default function MenuPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [couponOpen, setCouponOpen] = useState(false);

  useEffect(() => {
    productApi
      .list()
      .then(setProducts)
      .catch((e: ApiError) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

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
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      </div>

      {/* 쿠폰 배너 클릭 (프론트 쿠폰 발급 연동은 이슈 #16 에서) */}
      <Modal open={couponOpen} onClose={() => setCouponOpen(false)}>
        <div className="text-center">
          <div className="mb-3 text-4xl">🎟️</div>
          <h3 className="text-lg font-extrabold">선착순 쿠폰 이벤트</h3>
          <p className="mt-2 text-sm text-ink-soft">
            쿠폰 발급 기능은 곧 오픈됩니다. (동시성 제어 로직 구현 완료)
          </p>
          <Button className="mt-5" fullWidth onClick={() => setCouponOpen(false)}>
            확인
          </Button>
        </div>
      </Modal>
    </div>
  );
}
