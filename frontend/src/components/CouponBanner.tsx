interface Props {
  onClick: () => void;
}

/**
 * 선착순 쿠폰 발급 배너.
 * Step 2 에서는 자리/모달만 잡아두고, Step 3~5 에서 실제 발급 API 를 연동한다.
 */
export default function CouponBanner({ onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className="group relative w-full overflow-hidden rounded-xl2 p-5 text-left text-white shadow-card
        transition active:scale-[0.99]"
      style={{ background: 'linear-gradient(120deg, #FF7A47 0%, #FF5A1F 55%, #F03E00 100%)' }}
    >
      {/* 장식 원 */}
      <span className="pointer-events-none absolute -right-8 -top-10 h-36 w-36 rounded-full bg-white/15" />
      <span className="pointer-events-none absolute -bottom-12 right-16 h-28 w-28 rounded-full bg-white/10" />

      <div className="relative flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-white/80">한정 수량 이벤트</p>
          <h2 className="mt-1 text-xl font-extrabold leading-tight">🔥 선착순 쿠폰 받기</h2>
          <p className="mt-1 text-[13px] text-white/90">지금 바로 할인 쿠폰을 선착순으로 받아가세요!</p>
        </div>
        <span
          className="shrink-0 rounded-full bg-white px-4 py-2 text-sm font-bold text-brand-600
            transition group-hover:bg-brand-50"
        >
          받기 →
        </span>
      </div>
    </button>
  );
}
