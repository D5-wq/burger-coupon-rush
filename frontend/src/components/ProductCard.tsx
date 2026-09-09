import { Product } from '../types';
import { formatWon } from '../lib/format';
import ProductImage from './ProductImage';
import { cn } from '../lib/cn';

interface Props {
  product: Product;
  quantity: number;
  onIncrease: () => void;
  onDecrease: () => void;
}

export default function ProductCard({ product, quantity, onIncrease, onDecrease }: Props) {
  const selected = quantity > 0;

  return (
    <div className={cn('card overflow-hidden', selected && 'ring-2 ring-brand')}>
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

          {quantity === 0 ? (
            <button
              onClick={onIncrease}
              className="h-9 rounded-lg bg-brand-50 px-4 text-sm font-semibold text-brand-600 transition hover:bg-brand-100"
            >
              담기
            </button>
          ) : (
            <div className="flex items-center gap-2.5">
              <StepBtn label="빼기" onClick={onDecrease}>
                −
              </StepBtn>
              <span className="w-5 text-center text-sm font-bold tabular-nums">{quantity}</span>
              <StepBtn label="더하기" onClick={onIncrease}>
                +
              </StepBtn>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function StepBtn({
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
