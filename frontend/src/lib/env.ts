/**
 * 데모 모드 판별.
 * - VITE_DEMO_MODE=true 이거나
 * - 프로덕션 빌드인데 API base URL 이 없으면 (예: 백엔드 없이 Vercel 배포)
 * → 백엔드 대신 목(mock) 데이터로 동작한다.
 */
export const IS_DEMO =
  import.meta.env.VITE_DEMO_MODE === 'true' ||
  (import.meta.env.PROD && !import.meta.env.VITE_API_BASE_URL);
