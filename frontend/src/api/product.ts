import { http } from './client';
import { Product, ProductDetail } from '../types';
import { IS_DEMO } from '../lib/env';
import { mockApi } from './mock';

export const productApi = {
  list: () => (IS_DEMO ? mockApi.products() : http.get<Product[]>('/products')),
  /** 상품 상세 = 옵션 그룹 포함. */
  get: (id: number) =>
    IS_DEMO ? mockApi.product(id) : http.get<ProductDetail>(`/products/${id}`),
};
