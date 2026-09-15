import { http } from './client';
import { CouponSummary, IssueStrategy, MyCoupon } from '../types';
import { IS_DEMO } from '../lib/env';
import { mockApi } from './mock';

export const couponApi = {
  /** 발급 가능한 쿠폰 목록. */
  list: () => (IS_DEMO ? mockApi.coupons() : http.get<CouponSummary[]>('/coupons')),

  /** 선착순 발급. strategy 로 동시성 제어 방식을 고른다(기본은 서버 설정값). */
  issue: (couponId: number, strategy?: IssueStrategy) => {
    if (IS_DEMO) return mockApi.issueCoupon(couponId);
    const q = strategy ? `?strategy=${strategy}` : '';
    return http.post<void>(`/coupons/${couponId}/issue${q}`);
  },

  /** 내 쿠폰함. */
  myCoupons: () => (IS_DEMO ? mockApi.myCoupons() : http.get<MyCoupon[]>('/coupons/me')),
};
