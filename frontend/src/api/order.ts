import { http } from './client';
import { Order, OrderCreateRequest } from '../types';

export const orderApi = {
  create: (body: OrderCreateRequest) => http.post<Order>('/orders', body),
  myOrders: () => http.get<Order[]>('/orders'),
  get: (id: number) => http.get<Order>(`/orders/${id}`),
};
