import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { CartLine, ProductDetail, SelectedOption } from '../types';

const CART_KEY = 'bcr_cart';

interface CartContextValue {
  lines: CartLine[];
  count: number; // 총 수량
  total: number; // 옵션 포함 합계
  /** 상품 + 선택 옵션 + 수량을 담는다. 동일 구성(상품+옵션)이면 수량을 합친다. */
  add: (product: ProductDetail, options: SelectedOption[], quantity: number) => void;
  setQuantity: (key: string, quantity: number) => void;
  remove: (key: string) => void;
  clear: () => void;
}

const CartContext = createContext<CartContextValue | null>(null);

function loadLines(): CartLine[] {
  try {
    const raw = localStorage.getItem(CART_KEY);
    return raw ? (JSON.parse(raw) as CartLine[]) : [];
  } catch {
    return [];
  }
}

/** 상품 + 옵션 구성을 고유하게 식별하는 서명(옵션 순서에 무관). */
function signatureOf(productId: number, options: SelectedOption[]): string {
  const opts = [...options]
    .map((o) => `${o.optionItemId}:${o.quantity}`)
    .sort()
    .join(',');
  return `${productId}|${opts}`;
}

function buildLine(
  product: ProductDetail,
  options: SelectedOption[],
  quantity: number,
): CartLine {
  const extra = options.reduce((s, o) => s + o.extraPrice * o.quantity, 0);
  const unitTotal = product.price + extra;
  return {
    key: signatureOf(product.id, options),
    productId: product.id,
    productName: product.name,
    imageUrl: product.imageUrl,
    quantity,
    unitPrice: product.price,
    options,
    unitTotal,
    lineTotal: unitTotal * quantity,
  };
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(loadLines);

  useEffect(() => {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(lines));
    } catch {
      /* 저장 실패는 무시(용량/프라이빗 모드) */
    }
  }, [lines]);

  const add = useCallback(
    (product: ProductDetail, options: SelectedOption[], quantity: number) => {
      const line = buildLine(product, options, quantity);
      setLines((prev) => {
        const idx = prev.findIndex((l) => l.key === line.key);
        if (idx === -1) return [...prev, line];
        const next = [...prev];
        const merged = { ...next[idx] };
        merged.quantity += quantity;
        merged.lineTotal = merged.unitTotal * merged.quantity;
        next[idx] = merged;
        return next;
      });
    },
    [],
  );

  const setQuantity = useCallback((key: string, quantity: number) => {
    setLines((prev) =>
      prev
        .map((l) =>
          l.key === key
            ? { ...l, quantity, lineTotal: l.unitTotal * quantity }
            : l,
        )
        .filter((l) => l.quantity > 0),
    );
  }, []);

  const remove = useCallback((key: string) => {
    setLines((prev) => prev.filter((l) => l.key !== key));
  }, []);

  const clear = useCallback(() => setLines([]), []);

  const { count, total } = useMemo(() => {
    let c = 0;
    let t = 0;
    for (const l of lines) {
      c += l.quantity;
      t += l.lineTotal;
    }
    return { count: c, total: t };
  }, [lines]);

  const value = useMemo<CartContextValue>(
    () => ({ lines, count, total, add, setQuantity, remove, clear }),
    [lines, count, total, add, setQuantity, remove, clear],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
