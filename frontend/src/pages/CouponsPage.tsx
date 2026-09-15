import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { couponApi } from '../api/coupon';
import { ApiError, CouponSummary, MyCoupon } from '../types';
import { useAuth } from '../store/auth';
import Spinner from '../components/ui/Spinner';
import Modal from '../components/ui/Modal';
import Button from '../components/ui/Button';
import { cn } from '../lib/cn';

type IssueResult = { ok: boolean; title: string; message: string };

export default function CouponsPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [coupons, setCoupons] = useState<CouponSummary[]>([]);
  const [mine, setMine] = useState<MyCoupon[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [issuingId, setIssuingId] = useState<number | null>(null);
  const [result, setResult] = useState<IssueResult | null>(null);
  const [loginPrompt, setLoginPrompt] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [list, my] = await Promise.all([
        couponApi.list(),
        isAuthenticated ? couponApi.myCoupons() : Promise.resolve<MyCoupon[]>([]),
      ]);
      setCoupons(list);
      setMine(my);
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    load();
  }, [load]);

  const issuedIds = new Set(mine.map((m) => m.couponId));

  const issue = async (couponId: number) => {
    if (!isAuthenticated) {
      setLoginPrompt(true);
      return;
    }
    setIssuingId(couponId);
    try {
      await couponApi.issue(couponId);
      setResult({ ok: true, title: '발급 완료! 🎉', message: '쿠폰함에 담겼어요. 주문할 때 사용해보세요.' });
      await load();
    } catch (e) {
      const err = e as ApiError;
      const message =
        err.code === 'COUPON_409_SOLD_OUT'
          ? '아쉽게도 쿠폰이 모두 소진됐어요.'
          : err.code === 'COUPON_409_DUP'
            ? '이미 발급받은 쿠폰이에요.'
            : err.message;
      setResult({ ok: false, title: '발급 실패', message });
    } finally {
      setIssuingId(null);
    }
  };

  if (loading) return <Spinner label="쿠폰을 불러오는 중…" />;

  return (
    <div className="space-y-6">
      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>}

      {/* 발급 가능한 쿠폰 */}
      <section>
        <h1 className="mb-3 px-1 text-lg font-extrabold">🎟️ 선착순 쿠폰</h1>
        <div className="space-y-3">
          {coupons.map((c) => {
            const already = issuedIds.has(c.couponId);
            const disabled = c.soldOut || !c.open || already || issuingId === c.couponId;
            const pct = Math.round((c.stock / c.totalQuantity) * 100);
            return (
              <div key={c.couponId} className="card overflow-hidden">
                <div className="flex items-stretch">
                  <div className="flex w-24 shrink-0 flex-col items-center justify-center bg-brand text-white">
                    <span className="text-2xl font-extrabold leading-none">{c.discountRate}%</span>
                    <span className="mt-1 text-[10px] font-semibold opacity-90">
                      {c.applyScope === 'ORDER' ? '전체주문' : '상품한정'}
                    </span>
                  </div>
                  <div className="min-w-0 flex-1 p-4">
                    <h3 className="text-[15px] font-bold leading-tight">{c.name}</h3>
                    {c.productName && (
                      <p className="mt-0.5 text-[12px] text-ink-faint">대상: {c.productName}</p>
                    )}
                    {/* 남은 재고 게이지 */}
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-black/[0.06]">
                      <div className="h-full rounded-full bg-brand" style={{ width: `${pct}%` }} />
                    </div>
                    <div className="mt-1.5 flex items-center justify-between">
                      <span className="text-[11px] text-ink-faint">
                        {c.soldOut ? '품절' : `${c.stock.toLocaleString()} / ${c.totalQuantity.toLocaleString()}장 남음`}
                      </span>
                      <button
                        onClick={() => issue(c.couponId)}
                        disabled={disabled}
                        className={cn(
                          'h-8 rounded-lg px-3.5 text-sm font-semibold transition',
                          already
                            ? 'bg-black/[0.05] text-ink-faint'
                            : disabled
                              ? 'bg-black/[0.05] text-ink-faint'
                              : 'bg-brand-50 text-brand-600 hover:bg-brand-100',
                        )}
                      >
                        {already ? '보유중' : c.soldOut ? '품절' : issuingId === c.couponId ? '발급중…' : '받기'}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* 내 쿠폰함 */}
      {isAuthenticated && (
        <section>
          <h2 className="mb-3 px-1 text-lg font-extrabold">내 쿠폰함</h2>
          {mine.length === 0 ? (
            <p className="rounded-xl bg-black/[0.02] px-4 py-6 text-center text-sm text-ink-faint">
              아직 받은 쿠폰이 없어요. 위에서 선착순 쿠폰을 받아보세요!
            </p>
          ) : (
            <div className="space-y-2">
              {mine.map((m) => (
                <div
                  key={m.couponId}
                  className={cn(
                    'card flex items-center justify-between p-4',
                    m.used && 'opacity-60',
                  )}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-[15px] font-bold text-brand">{m.discountRate}%</span>
                      <span className="truncate text-sm font-semibold">{m.name}</span>
                    </div>
                    <span className="text-[11px] text-ink-faint">
                      {m.applyScope === 'ORDER' ? '전체 주문' : `대상: ${m.productName}`}
                    </span>
                  </div>
                  <span
                    className={cn(
                      'shrink-0 rounded-full px-2.5 py-1 text-xs font-bold',
                      m.used ? 'bg-black/[0.06] text-ink-faint' : 'bg-brand-50 text-brand-600',
                    )}
                  >
                    {m.used ? '사용완료' : '사용가능'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* 발급 결과 */}
      <Modal open={result !== null} onClose={() => setResult(null)}>
        <div className="text-center">
          <div className="mb-3 text-4xl">{result?.ok ? '🎟️' : '😢'}</div>
          <h3 className="text-lg font-extrabold">{result?.title}</h3>
          <p className="mt-2 text-sm text-ink-soft">{result?.message}</p>
          <Button className="mt-5" fullWidth onClick={() => setResult(null)}>
            확인
          </Button>
        </div>
      </Modal>

      {/* 로그인 유도 */}
      <Modal open={loginPrompt} onClose={() => setLoginPrompt(false)}>
        <div className="text-center">
          <div className="mb-3 text-4xl">🔒</div>
          <h3 className="text-lg font-extrabold">로그인이 필요해요</h3>
          <p className="mt-2 text-sm text-ink-soft">쿠폰을 받으려면 먼저 로그인해 주세요.</p>
          <Button className="mt-5" fullWidth onClick={() => navigate('/login')}>
            로그인하러 가기
          </Button>
        </div>
      </Modal>
    </div>
  );
}
