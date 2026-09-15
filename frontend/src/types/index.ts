// 백엔드 DTO 와 1:1 대응하는 타입들

export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
}

export interface UserResponse {
  id: number;
  email: string;
  name: string;
  role: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
}

/** 목록용 상품 (옵션 없음). */
export interface Product {
  id: number;
  name: string;
  price: number;
  imageUrl: string | null;
  description: string | null;
}

// ── 옵션 (상품 상세) ────────────────────────────────────────────────
export type SelectionType = 'SINGLE' | 'MULTI';

/** 옵션 그룹 안의 선택지. (예: "세트 +2,500", "패티 추가 +1,500") */
export interface OptionItem {
  id: number;
  name: string;
  extraPrice: number;
  defaultQuantity: number;
  maxQuantity: number;
  displayOrder: number;
}

/** 상품에 붙는 옵션 묶음. (예: "구성", "재료 추가·제거", "사이드", "음료") */
export interface OptionGroup {
  id: number;
  name: string;
  selectionType: SelectionType; // SINGLE=하나만, MULTI=여러 개
  required: boolean;
  minSelect: number;
  maxSelect: number;
  displayOrder: number;
  items: OptionItem[];
}

/** 상품 상세 = 상품 + 옵션 그룹들. */
export interface ProductDetail extends Product {
  optionGroups: OptionGroup[];
}

// ── 주문 ────────────────────────────────────────────────────────────
/** 주문 항목에 적용된 옵션 스냅샷 (주문 시점 이름/가격 보존). */
export interface OrderItemOption {
  groupName: string;
  itemName: string;
  extraPrice: number;
  quantity: number;
}

export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  options: OrderItemOption[];
}

export interface Order {
  id: number;
  items: OrderItem[];
  totalPrice: number;
  discountAmount: number;
  finalPrice: number;
  status: string;
  createdAt: string;
}

/** 주문 생성 요청. 항목마다 선택 옵션(optionItemId + 수량)을 함께 보낸다. */
export interface OrderCreateRequest {
  items: Array<{
    productId: number;
    quantity: number;
    options?: Array<{ optionItemId: number; quantity: number }>;
  }>;
}

// ── 장바구니 (프론트 전용, 서버에 없음) ─────────────────────────────
/** 사용자가 상세 화면에서 고른 옵션 한 항목. 표시(이름/가격)와 전송(id)에 모두 쓴다. */
export interface SelectedOption {
  optionItemId: number;
  groupName: string;
  itemName: string;
  extraPrice: number;
  quantity: number;
}

/** 장바구니 한 줄 = 상품 + 선택 옵션 + 수량. */
export interface CartLine {
  key: string; // 같은 상품이라도 옵션이 다르면 다른 줄로 취급하기 위한 서명
  productId: number;
  productName: string;
  imageUrl: string | null;
  quantity: number;
  unitPrice: number; // 상품 기본가
  options: SelectedOption[];
  /** 옵션 포함 단가 = unitPrice + Σ(extraPrice × 옵션수량). */
  unitTotal: number;
  /** 줄 합계 = unitTotal × quantity. */
  lineTotal: number;
}

/** API 호출 실패 시 던지는 에러(백엔드 ErrorCode 를 보존). */
export class ApiError extends Error {
  code: string;
  status: number;
  constructor(code: string, message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}
