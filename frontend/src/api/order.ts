import { http } from './client';
import { ApiError, Order, OrderCreateRequest } from '../types';
import { IS_DEMO } from '../lib/env';
import { mockApi } from './mock';

export const orderApi = {
  create: (body: OrderCreateRequest) =>
    IS_DEMO ? mockApi.createOrder(body) : http.post<Order>('/orders', body),

  myOrders: () => (IS_DEMO ? mockApi.myOrders() : http.get<Order[]>('/orders')),

  get: async (id: number): Promise<Order> => {
    if (!IS_DEMO) return http.get<Order>(`/orders/${id}`);
    const found = (await mockApi.myOrders()).find((o) => o.id === id);
    if (!found) throw new ApiError('ORDER_404', '주문을 찾을 수 없습니다.', 404);
    return found;
  },
};
