import { http } from './client';
import { Product } from '../types';

export const productApi = {
  list: () => http.get<Product[]>('/products'),
  get: (id: number) => http.get<Product>(`/products/${id}`),
};
