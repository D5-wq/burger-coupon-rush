// 백엔드 없이(데모 배포) 프론트가 완전히 동작하도록 하는 인메모리 목 API.
// 유저/주문은 localStorage 에 저장해 새로고침해도 유지된다.
import {
  ApiError,
  ApplyScope,
  CouponSummary,
  MyCoupon,
  Order,
  OptionGroup,
  OrderCreateRequest,
  OrderItemOption,
  Product,
  ProductDetail,
  TokenResponse,
  UserResponse,
} from '../types';

const delay = (ms = 350) => new Promise((r) => setTimeout(r, ms));

const USER_KEY = 'bcr_demo_user';
const ORDERS_KEY = 'bcr_demo_orders';
const COUPON_STOCK_KEY = 'bcr_demo_coupon_stock'; // couponId → 남은 재고
const MY_COUPON_KEY = 'bcr_demo_my_coupons'; // [{ couponId, used }]

const PRODUCTS: Product[] = [
  {
    id: 1,
    name: '클래식 치즈버거',
    price: 6500,
    imageUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=70',
    description: '두툼한 소고기 패티에 체다 치즈, 신선한 양상추와 토마토',
  },
  {
    id: 2,
    name: '더블 베이컨 버거',
    price: 8900,
    imageUrl: 'https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=500&q=70',
    description: '패티 두 장과 바삭한 베이컨, 스모키한 BBQ 소스',
  },
  {
    id: 3,
    name: '스모키 불고기 버거',
    price: 7200,
    imageUrl: 'https://images.unsplash.com/photo-1550547660-d9450f859349?w=500&q=70',
    description: '한국식 불고기 양념 패티와 구운 양파',
  },
  {
    id: 4,
    name: '스파이시 치킨 버거',
    price: 6900,
    imageUrl: 'https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=500&q=70',
    description: '바삭한 통닭다리살에 매콤한 핫소스',
  },
  {
    id: 5,
    name: '머쉬룸 스위스 버거',
    price: 7800,
    imageUrl: 'https://images.unsplash.com/photo-1572802419224-296b0aeee0d9?w=500&q=70',
    description: '볶은 양송이버섯과 스위스 치즈의 진한 풍미',
  },
  {
    id: 6,
    name: '아보카도 비프 버거',
    price: 8500,
    imageUrl: 'https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=500&q=70',
    description: '부드러운 아보카도와 소고기 패티의 조화',
  },
  {
    id: 7,
    name: '트러플 마요 버거',
    price: 9500,
    imageUrl: 'https://images.unsplash.com/photo-1610440042657-612c34d95e9f?w=500&q=70',
    description: '트러플 마요네즈와 카라멜라이즈드 어니언',
  },
  {
    id: 8,
    name: '클래식 비프 버거',
    price: 5900,
    imageUrl: 'https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?w=500&q=70',
    description: '기본에 충실한 소고기 패티 단품 버거',
  },
];

// 데모 모드에서 모든 상품이 공유하는 옵션 템플릿(백엔드 시드 구조와 동일한 형태).
const OPTION_GROUPS: OptionGroup[] = [
  {
    id: 1,
    name: '구성',
    selectionType: 'SINGLE',
    required: true,
    minSelect: 1,
    maxSelect: 1,
    displayOrder: 0,
    items: [
      { id: 1, name: '단품', extraPrice: 0, defaultQuantity: 0, maxQuantity: 1, displayOrder: 0 },
      { id: 2, name: '세트 (사이드+음료)', extraPrice: 2500, defaultQuantity: 0, maxQuantity: 1, displayOrder: 1 },
    ],
  },
  {
    id: 2,
    name: '재료 추가·제거',
    selectionType: 'MULTI',
    required: false,
    minSelect: 0,
    maxSelect: 10,
    displayOrder: 1,
    items: [
      { id: 3, name: '양파', extraPrice: 0, defaultQuantity: 1, maxQuantity: 2, displayOrder: 0 },
      { id: 4, name: '양상추', extraPrice: 0, defaultQuantity: 1, maxQuantity: 2, displayOrder: 1 },
      { id: 5, name: '토마토', extraPrice: 0, defaultQuantity: 1, maxQuantity: 2, displayOrder: 2 },
      { id: 6, name: '피클', extraPrice: 0, defaultQuantity: 1, maxQuantity: 3, displayOrder: 3 },
      { id: 7, name: '패티 추가', extraPrice: 1500, defaultQuantity: 0, maxQuantity: 2, displayOrder: 4 },
      { id: 8, name: '치즈 추가', extraPrice: 1000, defaultQuantity: 0, maxQuantity: 2, displayOrder: 5 },
    ],
  },
  {
    id: 3,
    name: '사이드 변경 (세트)',
    selectionType: 'SINGLE',
    required: false,
    minSelect: 0,
    maxSelect: 1,
    displayOrder: 2,
    items: [
      { id: 9, name: '감자튀김', extraPrice: 0, defaultQuantity: 0, maxQuantity: 1, displayOrder: 0 },
      { id: 10, name: '치즈스틱', extraPrice: 500, defaultQuantity: 0, maxQuantity: 1, displayOrder: 1 },
      { id: 11, name: '양념감자', extraPrice: 800, defaultQuantity: 0, maxQuantity: 1, displayOrder: 2 },
      { id: 12, name: '어니언링', extraPrice: 800, defaultQuantity: 0, maxQuantity: 1, displayOrder: 3 },
    ],
  },
  {
    id: 4,
    name: '음료 (세트)',
    selectionType: 'SINGLE',
    required: false,
    minSelect: 0,
    maxSelect: 1,
    displayOrder: 3,
    items: [
      { id: 13, name: '코카콜라 R', extraPrice: 0, defaultQuantity: 0, maxQuantity: 1, displayOrder: 0 },
      { id: 14, name: '코카콜라 L', extraPrice: 500, defaultQuantity: 0, maxQuantity: 1, displayOrder: 1 },
      { id: 15, name: '제로콜라 R', extraPrice: 0, defaultQuantity: 0, maxQuantity: 1, displayOrder: 2 },
      { id: 16, name: '스프라이트 R', extraPrice: 0, defaultQuantity: 0, maxQuantity: 1, displayOrder: 3 },
    ],
  },
];

// optionItemId → 소속 그룹명/항목 정보. createOrder 에서 스냅샷 계산에 쓴다.
const OPTION_ITEM_INDEX = new Map<number, { groupName: string; itemName: string; extraPrice: number }>();
for (const g of OPTION_GROUPS) {
  for (const it of g.items) {
    OPTION_ITEM_INDEX.set(it.id, { groupName: g.name, itemName: it.name, extraPrice: it.extraPrice });
  }
}

// 데모 쿠폰 시드(백엔드 CouponInitializer 와 동일한 구성).
interface CouponSeed {
  couponId: number;
  name: string;
  discountRate: number;
  applyScope: ApplyScope;
  productId: number | null;
  totalQuantity: number;
}
const COUPON_SEEDS: CouponSeed[] = [
  { couponId: 1, name: '전체 주문 10% 할인 쿠폰', discountRate: 10, applyScope: 'ORDER', productId: null, totalQuantity: 200 },
  { couponId: 2, name: '클래식 치즈버거 30% 할인 쿠폰', discountRate: 30, applyScope: 'PRODUCT', productId: 1, totalQuantity: 100 },
  { couponId: 3, name: '더블 베이컨 버거 25% 할인 쿠폰', discountRate: 25, applyScope: 'PRODUCT', productId: 2, totalQuantity: 50 },
  { couponId: 4, name: '스파이시 치킨 버거 15% 할인 쿠폰', discountRate: 15, applyScope: 'PRODUCT', productId: 4, totalQuantity: 80 },
];
const COUPON_BY_ID = new Map(COUPON_SEEDS.map((c) => [c.couponId, c]));

function productNameOf(productId: number | null): string | null {
  if (productId == null) return null;
  return PRODUCTS.find((p) => p.id === productId)?.name ?? null;
}

function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}
function saveJson(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* 무시 */
  }
}

/** couponId → 남은 재고. 최초 접근 시 시드 총량으로 초기화. */
function loadCouponStock(): Record<number, number> {
  const stored = loadJson<Record<number, number>>(COUPON_STOCK_KEY, {});
  let changed = false;
  for (const c of COUPON_SEEDS) {
    if (stored[c.couponId] == null) {
      stored[c.couponId] = c.totalQuantity;
      changed = true;
    }
  }
  if (changed) saveJson(COUPON_STOCK_KEY, stored);
  return stored;
}

interface MyCouponState {
  couponId: number;
  used: boolean;
}
function loadMyCoupons(): MyCouponState[] {
  return loadJson<MyCouponState[]>(MY_COUPON_KEY, []);
}

/** 데모 쿠폰 적용: 범위별 할인 계산 + 사용 처리(중복 방지). 할인액을 반환. */
function applyMockCoupon(
  couponId: number,
  items: Array<{ productId: number; lineTotal: number }>,
  totalPrice: number,
): number {
  const seed = COUPON_BY_ID.get(couponId);
  if (!seed) throw new ApiError('COUPON_404', '쿠폰을 찾을 수 없습니다.', 404);
  const mine = loadMyCoupons();
  const held = mine.find((m) => m.couponId === couponId);
  if (!held) throw new ApiError('COUPON_400_NOT_ISSUED', '보유하지 않은 쿠폰입니다.', 400);
  if (held.used) throw new ApiError('COUPON_409_USED', '이미 사용한 쿠폰입니다.', 409);

  const base =
    seed.applyScope === 'ORDER'
      ? totalPrice
      : items.filter((it) => it.productId === seed.productId).reduce((s, it) => s + it.lineTotal, 0);
  if (base <= 0) {
    throw new ApiError('COUPON_400_NOT_APPLICABLE', '이 주문에 적용할 수 없는 쿠폰입니다.', 400);
  }

  held.used = true;
  saveJson(MY_COUPON_KEY, mine);
  return Math.floor((base * seed.discountRate) / 100);
}

function loadUser(): UserResponse | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserResponse) : null;
  } catch {
    return null;
  }
}

function loadOrders(): Order[] {
  try {
    const raw = localStorage.getItem(ORDERS_KEY);
    return raw ? (JSON.parse(raw) as Order[]) : [];
  } catch {
    return [];
  }
}

export const mockApi = {
  async products(): Promise<Product[]> {
    await delay();
    return PRODUCTS;
  },

  async product(id: number): Promise<ProductDetail> {
    await delay(150);
    const p = PRODUCTS.find((x) => x.id === id);
    if (!p) throw new ApiError('PRODUCT_404', '상품을 찾을 수 없습니다.', 404);
    return { ...p, optionGroups: OPTION_GROUPS };
  },

  async signUp(body: { email: string; name: string }): Promise<UserResponse> {
    await delay();
    const user: UserResponse = { id: 1, email: body.email, name: body.name, role: 'USER' };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    return user;
  },

  async login(body: { email: string }): Promise<TokenResponse> {
    await delay();
    const existing = loadUser();
    const user: UserResponse = existing?.email === body.email
      ? existing
      : { id: 1, email: body.email, name: '데모유저', role: 'USER' };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    return { accessToken: 'demo-token', tokenType: 'Bearer', expiresInMs: 86_400_000 };
  },

  async me(): Promise<UserResponse> {
    await delay(120);
    const user = loadUser();
    if (!user) throw new ApiError('AUTH_401_2', '인증이 필요합니다.', 401);
    return user;
  },

  async createOrder(body: OrderCreateRequest): Promise<Order> {
    await delay();
    const items = body.items.map((line) => {
      const p = PRODUCTS.find((x) => x.id === line.productId)!;
      const options: OrderItemOption[] = (line.options ?? []).map((o) => {
        const meta = OPTION_ITEM_INDEX.get(o.optionItemId);
        return {
          groupName: meta?.groupName ?? '옵션',
          itemName: meta?.itemName ?? `#${o.optionItemId}`,
          extraPrice: meta?.extraPrice ?? 0,
          quantity: o.quantity,
        };
      });
      const extra = options.reduce((s, o) => s + o.extraPrice * o.quantity, 0);
      return {
        productId: p.id,
        productName: p.name,
        unitPrice: p.price,
        quantity: line.quantity,
        lineTotal: (p.price + extra) * line.quantity,
        options,
      };
    });
    const totalPrice = items.reduce((s, it) => s + it.lineTotal, 0);

    // 쿠폰 적용(범위별 할인 + 사용 처리) — 백엔드 규칙과 동일하게.
    let discountAmount = 0;
    if (body.couponId != null) {
      discountAmount = applyMockCoupon(body.couponId, items, totalPrice);
    }

    const orders = loadOrders();
    const order: Order = {
      id: (orders[0]?.id ?? 0) + 1,
      items,
      totalPrice,
      discountAmount,
      finalPrice: totalPrice - discountAmount,
      status: 'CREATED',
      createdAt: new Date().toISOString(),
    };
    localStorage.setItem(ORDERS_KEY, JSON.stringify([order, ...orders]));
    return order;
  },

  async myOrders(): Promise<Order[]> {
    await delay();
    return loadOrders();
  },

  // ── 쿠폰 ──────────────────────────────────────────────────────────
  async coupons(): Promise<CouponSummary[]> {
    await delay(200);
    const stock = loadCouponStock();
    return COUPON_SEEDS.map((c) => {
      const left = stock[c.couponId] ?? c.totalQuantity;
      return {
        couponId: c.couponId,
        name: c.name,
        discountRate: c.discountRate,
        applyScope: c.applyScope,
        productId: c.productId,
        productName: productNameOf(c.productId),
        totalQuantity: c.totalQuantity,
        stock: left,
        soldOut: left <= 0,
        open: true,
      };
    });
  },

  async issueCoupon(couponId: number): Promise<void> {
    await delay(300);
    const seed = COUPON_BY_ID.get(couponId);
    if (!seed) throw new ApiError('COUPON_404', '쿠폰을 찾을 수 없습니다.', 404);
    const mine = loadMyCoupons();
    if (mine.some((m) => m.couponId === couponId)) {
      throw new ApiError('COUPON_409_DUP', '이미 발급받은 쿠폰입니다.', 409);
    }
    const stock = loadCouponStock();
    if ((stock[couponId] ?? 0) <= 0) {
      throw new ApiError('COUPON_409_SOLD_OUT', '쿠폰이 모두 소진되었습니다.', 409);
    }
    stock[couponId] -= 1;
    saveJson(COUPON_STOCK_KEY, stock);
    mine.unshift({ couponId, used: false });
    saveJson(MY_COUPON_KEY, mine);
  },

  async myCoupons(): Promise<MyCoupon[]> {
    await delay(200);
    return loadMyCoupons()
      .map((m) => {
        const seed = COUPON_BY_ID.get(m.couponId);
        if (!seed) return null;
        return {
          couponId: seed.couponId,
          name: seed.name,
          discountRate: seed.discountRate,
          applyScope: seed.applyScope,
          productId: seed.productId,
          productName: productNameOf(seed.productId),
          used: m.used,
          issuedAt: new Date().toISOString(),
        } as MyCoupon;
      })
      .filter((x): x is MyCoupon => x !== null);
  },

  logout() {
    localStorage.removeItem(USER_KEY);
  },
};
