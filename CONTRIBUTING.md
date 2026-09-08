# 기여 & 개발 컨벤션

혼자 개발하지만 협업하듯이 컨벤션을 지킵니다.

## 브랜치 전략 (git-flow lite)

```
main  ← 최종/릴리스 (항상 배포 가능한 상태)
 └─ dev  ← 통합 브랜치 (모든 feature가 여기로 머지)
     └─ feat/*   ← 기능 개발
     └─ fix/*    ← 버그 수정
     └─ chore/*  ← 설정/빌드/문서 외 잡일
     └─ docs/*   ← 문서
```

- 모든 작업은 `dev`에서 브랜치를 따서 진행하고, PR로 `dev`에 머지한다.
- 마일스톤이 마무리되면 `dev` → `main` PR로 릴리스한다.
- 브랜치 이름: `feat/step-1-project-setup` 처럼 `<type>/step-<n>-<slug>`.

## 커밋 컨벤션 (Conventional Commits)

```
<type>(<scope>): <subject>
```

| type | 설명 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 (동작 변화 없음) |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드/설정/의존성 등 잡무 |
| `docs` | 문서 |
| `perf` | 성능 개선 |
| `style` | 포맷/세미콜론 등 (로직 변화 없음) |

- scope 예: `backend`, `frontend`, `coupon`, `infra`, `loadtest`, `docs`
- subject는 명령형·현재형으로. 예: `feat(coupon): add pessimistic lock issuer`

## PR 규칙

- 하나의 PR = 하나의 논리적 단위(가능하면 하나의 이슈).
- PR 본문에 `Closes #N`으로 이슈 연결.
- 셀프 리뷰: 주요 결정/트레이드오프를 PR 코멘트로 남긴다.
- 프론트 변경은 스크린샷, 부하테스트는 측정 수치를 첨부한다.

## 코드 스타일

- **Backend**: 계층 분리(Controller → Service → Repository), 엔티티 ↔ DTO 변환은
  경계에서만. 도메인 로직은 엔티티/도메인 서비스에.
- **Frontend**: 함수형 컴포넌트 + 훅, 타입 명시, Tailwind 유틸리티 우선.
