// 백엔드 없이(데모 배포) 프론트가 완전히 동작하도록 하는 인메모리 목 API.
// 유저/주문은 localStorage 에 저장해 새로고침해도 유지된다.
import {
  ApiError,
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
    const orders = loadOrders();
    const order: Order = {
      id: (orders[0]?.id ?? 0) + 1,
      items,
      totalPrice,
      discountAmount: 0,
      finalPrice: totalPrice,
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

  logout() {
    localStorage.removeItem(USER_KEY);
  },
};
