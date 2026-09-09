# 배포 가이드

## 현재 단계: 프론트엔드만 Vercel 배포 (데모 모드)

백엔드가 아직 배포되지 않았으므로, 프론트는 **데모 모드**로 배포합니다.
백엔드 주소(`VITE_API_BASE_URL`)가 없으면 프론트가 자동으로 목(mock) 데이터로 동작해
**메뉴 조회 · 로그인/회원가입 · 주문 · 내 주문**이 모두 인터랙티브하게 돌아갑니다.
(유저/주문은 브라우저 localStorage 에 저장됩니다.)

### Vercel 대시보드로 배포 (권장, 가장 쉬움)

1. https://vercel.com → **Add New… → Project**
2. GitHub 저장소 `D5-wq/burger-coupon-rush` 를 **Import**
3. **Root Directory** 를 `frontend` 로 지정 ⚠️ (모노레포이므로 필수)
4. Framework Preset 은 자동으로 **Vite** 로 인식됨
   - Build Command: `npm run build`
   - Output Directory: `dist`
5. (선택) Environment Variables — 아무것도 안 넣어도 자동 데모 모드.
   명시하고 싶으면 `VITE_DEMO_MODE = true`
6. **Deploy** 클릭 → 몇 초 뒤 `https://<프로젝트>.vercel.app` 발급

이후 `main` 브랜치에 푸시될 때마다 자동 재배포됩니다.
(`vercel.json` 에 SPA 라우팅 rewrite 가 있어 새로고침/딥링크도 정상 동작)

### Vercel CLI 로 배포 (대안)

```bash
npm i -g vercel
cd frontend
vercel            # 최초: 프로젝트 연결 (Root=현재 폴더)
vercel --prod     # 프로덕션 배포
```

---

## 다음 단계: 백엔드 배포 (예정)

백엔드(Spring Boot + MySQL + Redis)를 배포하면, Vercel 환경변수에
`VITE_API_BASE_URL=https://<백엔드주소>/api` 를 넣는 것만으로 실제 API 로 전환됩니다.
(코드 변경 불필요 — 데모 모드가 자동으로 꺼짐)

후보 플랫폼:
- **Railway / Render**: Spring Boot + MySQL + Redis 를 한 프로젝트에 간단히
- **AWS**: EC2/Beanstalk + RDS + ElastiCache (이력서용, 설정 복잡)

백엔드 배포 시 준비물(추가 예정): `backend/Dockerfile`, prod 프로필, CI/CD 워크플로.
