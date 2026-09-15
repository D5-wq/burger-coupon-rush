import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productApi } from '../api/product';
import { Product, ApiError } from '../types';
import ProductCard from '../components/ProductCard';
import CouponBanner from '../components/CouponBanner';
import Spinner from '../components/ui/Spinner';

export default function MenuPage() {
  const navigate = useNavigate();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
      <CouponBanner onClick={() => navigate('/coupons')} />

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
    </div>
  );
}
