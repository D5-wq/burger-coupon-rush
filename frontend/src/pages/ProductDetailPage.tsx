import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { productApi } from '../api/product';
import { ApiError, OptionGroup, ProductDetail, SelectedOption } from '../types';
import { useCart } from '../store/cart';
import { formatWon } from '../lib/format';
import ProductImage from '../components/ProductImage';
import Spinner from '../components/ui/Spinner';
import { cn } from '../lib/cn';

// SINGLE 그룹: 선택된 항목 id(또는 null). MULTI 그룹: 항목 id → 수량.
type SingleState = Record<number, number | null>; // groupId → optionItemId | null
type MultiState = Record<number, Record<number, number>>; // groupId → { itemId: qty }

export default function ProductDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const cart = useCart();

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [single, setSingle] = useState<SingleState>({});
  const [multi, setMulti] = useState<MultiState>({});
  const [qty, setQty] = useState(1);

  useEffect(() => {
    const pid = Number(id);
    if (!pid) {
      setError('잘못된 상품입니다.');
      setLoading(false);
      return;
    }
    productApi
      .get(pid)
      .then((p) => {
        setProduct(p);
        initSelections(p, setSingle, setMulti);
      })
      .catch((e: ApiError) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  // 선택 옵션 → SelectedOption[] (수량>0 인 것만)
  const selectedOptions = useMemo<SelectedOption[]>(() => {
    if (!product) return [];
    const out: SelectedOption[] = [];
    for (const g of product.optionGroups) {
      if (g.selectionType === 'SINGLE') {
        const itemId = single[g.id];
        if (itemId == null) continue;
        const item = g.items.find((i) => i.id === itemId);
        if (item) out.push(toSelected(g, item.id, item.name, item.extraPrice, 1));
      } else {
        const m = multi[g.id] ?? {};
        for (const item of g.items) {
          const q = m[item.id] ?? 0;
          if (q > 0) out.push(toSelected(g, item.id, item.name, item.extraPrice, q));
        }
      }
    }
    return out;
  }, [product, single, multi]);

  const unitTotal = useMemo(() => {
    const base = product?.price ?? 0;
    return base + selectedOptions.reduce((s, o) => s + o.extraPrice * o.quantity, 0);
  }, [product, selectedOptions]);

  const validationError = useMemo(
    () => (product ? validate(product, single, multi) : null),
    [product, single, multi],
  );

  if (loading) return <Spinner label="상품 정보를 불러오는 중…" />;
  if (error || !product)
    return (
      <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
        {error ?? '상품을 찾을 수 없습니다.'}
      </p>
    );

  const groups = [...product.optionGroups].sort((a, b) => a.displayOrder - b.displayOrder);

  const chooseSingle = (groupId: number, itemId: number, required: boolean) =>
    setSingle((prev) => ({
      ...prev,
      // 선택 해제 허용(필수 그룹은 제외)
      [groupId]: !required && prev[groupId] === itemId ? null : itemId,
    }));

  const bumpMulti = (groupId: number, itemId: number, delta: number, max: number) =>
    setMulti((prev) => {
      const g = { ...(prev[groupId] ?? {}) };
      const next = Math.min(max, Math.max(0, (g[itemId] ?? 0) + delta));
      g[itemId] = next;
      return { ...prev, [groupId]: g };
    });

  const addToCart = () => {
    if (validationError) return;
    cart.add(product, selectedOptions, qty);
    navigate('/cart');
  };

  return (
    <div className="space-y-5">
      <button
        onClick={() => navigate(-1)}
        className="text-sm font-semibold text-ink-soft hover:text-ink"
      >
        ← 메뉴로
      </button>

      <div className="card overflow-hidden">
        <ProductImage src={product.imageUrl} name={product.name} className="aspect-[16/10] w-full" />
        <div className="p-4">
          <h1 className="text-xl font-extrabold">{product.name}</h1>
          {product.description && (
            <p className="mt-1 text-sm leading-snug text-ink-faint">{product.description}</p>
          )}
          <p className="mt-2 text-lg font-extrabold text-brand">{formatWon(product.price)}</p>
        </div>
      </div>

      {groups.map((g) => (
        <section key={g.id} className="card p-4">
          <div className="mb-3 flex items-center gap-2">
            <h2 className="text-[15px] font-bold">{g.name}</h2>
            {g.required ? (
              <span className="rounded-md bg-brand-50 px-1.5 py-0.5 text-[10px] font-bold text-brand-600">
                필수
              </span>
            ) : (
              <span className="rounded-md bg-black/[0.05] px-1.5 py-0.5 text-[10px] font-bold text-ink-faint">
                선택
              </span>
            )}
            {g.selectionType === 'MULTI' && g.maxSelect < 99 && (
              <span className="text-[11px] text-ink-faint">최대 {g.maxSelect}개</span>
            )}
          </div>

          {g.selectionType === 'SINGLE' ? (
            <div className="space-y-2">
              {g.items.map((item) => {
                const active = single[g.id] === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => chooseSingle(g.id, item.id, g.required)}
                    className={cn(
                      'flex w-full items-center justify-between rounded-xl border px-4 py-3 text-left text-sm transition',
                      active
                        ? 'border-brand bg-brand-50 font-semibold text-brand-700'
                        : 'border-black/10 hover:bg-black/[0.02]',
                    )}
                  >
                    <span className="flex items-center gap-2">
                      <span
                        className={cn(
                          'flex h-4 w-4 items-center justify-center rounded-full border',
                          active ? 'border-brand bg-brand' : 'border-black/25',
                        )}
                      >
                        {active && <span className="h-1.5 w-1.5 rounded-full bg-white" />}
                      </span>
                      {item.name}
                    </span>
                    {item.extraPrice > 0 && (
                      <span className="tabular-nums text-ink-soft">+{formatWon(item.extraPrice)}</span>
                    )}
                  </button>
                );
              })}
            </div>
          ) : (
            <div className="space-y-2">
              {g.items.map((item) => {
                const q = multi[g.id]?.[item.id] ?? 0;
                return (
                  <div
                    key={item.id}
                    className="flex items-center justify-between rounded-xl border border-black/10 px-4 py-2.5 text-sm"
                  >
                    <div>
                      <span className={cn(q > 0 && 'font-semibold')}>{item.name}</span>
                      {item.extraPrice > 0 && (
                        <span className="ml-2 tabular-nums text-ink-soft">
                          +{formatWon(item.extraPrice)}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2.5">
                      <Step
                        label={`${item.name} 빼기`}
                        disabled={q <= 0}
                        onClick={() => bumpMulti(g.id, item.id, -1, item.maxQuantity)}
                      >
                        −
                      </Step>
                      <span className="w-4 text-center font-bold tabular-nums">{q}</span>
                      <Step
                        label={`${item.name} 더하기`}
                        disabled={q >= item.maxQuantity}
                        onClick={() => bumpMulti(g.id, item.id, +1, item.maxQuantity)}
                      >
                        +
                      </Step>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      ))}

      {/* 수량 */}
      <section className="card flex items-center justify-between p-4">
        <h2 className="text-[15px] font-bold">수량</h2>
        <div className="flex items-center gap-3">
          <Step label="수량 빼기" disabled={qty <= 1} onClick={() => setQty((q) => Math.max(1, q - 1))}>
            −
          </Step>
          <span className="w-5 text-center font-bold tabular-nums">{qty}</span>
          <Step label="수량 더하기" onClick={() => setQty((q) => q + 1)}>
            +
          </Step>
        </div>
      </section>

      {/* 하단 담기 바 */}
      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-md p-4">
        {validationError && (
          <p className="mb-2 rounded-lg bg-amber-50 px-3 py-2 text-center text-xs font-semibold text-amber-700">
            {validationError}
          </p>
        )}
        <button
          onClick={addToCart}
          disabled={!!validationError}
          className="flex w-full items-center justify-between rounded-xl2 bg-brand px-5 py-4 text-white shadow-lg transition hover:bg-brand-600 disabled:opacity-60"
        >
          <span className="text-sm font-semibold">장바구니 담기</span>
          <span className="text-base font-extrabold">{formatWon(unitTotal * qty)}</span>
        </button>
      </div>
    </div>
  );
}

function Step({
  children,
  onClick,
  label,
  disabled,
}: {
  children: React.ReactNode;
  onClick: () => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <button
      aria-label={label}
      onClick={onClick}
      disabled={disabled}
      className="flex h-8 w-8 items-center justify-center rounded-full bg-brand text-lg font-bold leading-none text-white transition hover:bg-brand-600 disabled:bg-black/10 disabled:text-ink-faint"
    >
      {children}
    </button>
  );
}

// ── 헬퍼 ────────────────────────────────────────────────────────────
function toSelected(
  g: OptionGroup,
  optionItemId: number,
  itemName: string,
  extraPrice: number,
  quantity: number,
): SelectedOption {
  return { optionItemId, groupName: g.name, itemName, extraPrice, quantity };
}

function initSelections(
  p: ProductDetail,
  setSingle: (s: SingleState) => void,
  setMulti: (s: MultiState) => void,
) {
  const s: SingleState = {};
  const m: MultiState = {};
  for (const g of p.optionGroups) {
    if (g.selectionType === 'SINGLE') {
      // 필수 SINGLE 은 첫 항목을 기본 선택, 선택형은 미선택.
      s[g.id] = g.required && g.items.length > 0 ? g.items[0].id : null;
    } else {
      const gm: Record<number, number> = {};
      for (const item of g.items) gm[item.id] = item.defaultQuantity;
      m[g.id] = gm;
    }
  }
  setSingle(s);
  setMulti(m);
}

/** 담기 전 검증: 필수 그룹 선택 여부와 MULTI 최소/최대 개수. */
function validate(p: ProductDetail, single: SingleState, multi: MultiState): string | null {
  for (const g of p.optionGroups) {
    if (g.selectionType === 'SINGLE') {
      if (g.required && single[g.id] == null) return `'${g.name}'을(를) 선택해 주세요.`;
    } else {
      const m = multi[g.id] ?? {};
      const count = Object.values(m).reduce((a, b) => a + b, 0);
      if (g.required && count < g.minSelect)
        return `'${g.name}'에서 최소 ${g.minSelect}개 선택해 주세요.`;
      if (count > g.maxSelect) return `'${g.name}'은(는) 최대 ${g.maxSelect}개까지예요.`;
    }
  }
  return null;
}
