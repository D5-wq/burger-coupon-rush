/** 원화 포맷: 6500 → "6,500원" */
export function formatWon(price: number): string {
  return `${price.toLocaleString('ko-KR')}원`;
}

/** 상품명 기반으로 안정적인 브랜드 계열 그라디언트를 만들어 이미지 폴백에 사용. */
export function gradientFor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash);
  }
  const hue = Math.abs(hash) % 18; // 따뜻한 주황~빨강 계열로 제한
  const h1 = 8 + hue; // 8~26
  const h2 = 20 + hue; // 20~38
  return `linear-gradient(135deg, hsl(${h1} 92% 60%), hsl(${h2} 96% 50%))`;
}
