/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 백엔드 API base URL. 미설정 시 개발은 vite proxy(/api) 사용. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
