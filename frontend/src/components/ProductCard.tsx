import { Link } from 'react-router-dom';
import { Product } from '../types';
import { formatWon } from '../lib/format';
import ProductImage from './ProductImage';

interface Props {
  product: Product;
}

/** 메뉴 카드. 누르면 상품 상세(옵션/세트 선택)로 이동한다. */
export default function ProductCard({ product }: Props) {
  return (
    <Link to={`/products/${product.id}`} className="card block overflow-hidden transition active:scale-[0.99]">
      <ProductImage src={product.imageUrl} name={product.name} className="aspect-[4/3] w-full" />

      <div className="p-4">
        <h3 className="text-[15px] font-bold leading-tight">{product.name}</h3>
        {product.description && (
          <p className="mt-1 line-clamp-2 text-[13px] leading-snug text-ink-faint">
            {product.description}
          </p>
        )}

        <div className="mt-3 flex items-center justify-between">
          <span className="text-[15px] font-extrabold text-brand">{formatWon(product.price)}</span>
          <span className="h-9 rounded-lg bg-brand-50 px-4 text-sm font-semibold leading-9 text-brand-600">
            담기
          </span>
        </div>
      </div>
    </Link>
  );
}
