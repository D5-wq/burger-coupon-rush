import { http } from './client';
import { Product } from '../types';
import { IS_DEMO } from '../lib/env';
import { mockApi } from './mock';

export const productApi = {
  list: () => (IS_DEMO ? mockApi.products() : http.get<Product[]>('/products')),
  get: (id: number) => (IS_DEMO ? mockApi.product(id) : http.get<Product>(`/products/${id}`)),
};
