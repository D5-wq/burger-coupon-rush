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

export interface Product {
  id: number;
  name: string;
  price: number;
  imageUrl: string | null;
  description: string | null;
}

export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
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

export interface OrderCreateRequest {
  items: Array<{ productId: number; quantity: number }>;
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
