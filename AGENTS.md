# AGENTS.md

이 저장소에서 Claude/Codex 계열 에이전트가 가장 먼저 읽는 루트 지침이다. 모든 답변과 진행 상황은 한국어로 작성하고, 실제 작업은 파일과 명령 결과를 확인한 뒤 수행한다.

## 운영 구조

- `.agents/skills`는 프로젝트 스킬의 원본 위치다.
- `.claude/skills`는 `.agents/skills`의 각 스킬을 참조한다. 스킬을 추가하거나 수정할 때는 `.agents/skills/<name>/SKILL.md`를 원본으로 관리한다.
- `.agents/agents`는 프로젝트 전용 서브에이전트 정의의 원본 위치다.
- `.claude/agents`는 `.agents/agents`의 각 에이전트를 참조한다.
- 루트 문서와 스킬 문서가 충돌하면 더 구체적인 스킬 문서를 우선하되, 한국어 응답, 검증, 기존 구조 유지 원칙은 항상 지킨다.

## 기본 원칙

- 답변, 진행 상황, PR/리뷰 답글은 한국어로 작성한다.
- 코드, 변수명, 함수명, 클래스명, 파일명은 영어를 사용한다.
- 주석은 특별한 이유가 없으면 한국어로 작성한다.
- 구현 전 요구사항을 이해하고 작업 계획을 간단히 설명한다.
- 기존 코드 스타일과 아키텍처를 유지한다.
- 추측해서 구현하지 말고 파일, diff, 문서, 명령 결과로 근거를 확인한다.
- 서드파티 라이브러리, 프레임워크, SDK, CLI, 클라우드 서비스 사용법이 필요하면 Context7 MCP로 현재 문서를 먼저 확인한다.
- 사용자 변경분을 되돌리지 않는다. Git 상태가 없거나 불분명하면 실제 파일 내용을 기준으로 조심스럽게 작업한다.

## 프로젝트 기술 스택

- Frontend: Vue 3 Composition API, `<script setup>`, TypeScript, Vite
- Package/Monorepo: pnpm workspace, Turborepo
- Styling: Tailwind CSS
- State/Data: Pinia, TanStack Query `@tanstack/vue-query`
- Validation/Test: Zod, Vitest, Playwright
- Backend: Spring Framework 5.x 기반 Legacy WAR, Java 17, MyBatis, Spring Security, JWT, MySQL
- GitHub: `gh` CLI 기본 사용, 필요 시 GitHub REST API

## 저장소 구조

```text
.
├── AGENTS.md
├── .agents/
│   ├── agents/
│   └── skills/
│       ├── fe-scaffold/
│       ├── fe-api-layer/
│       ├── fe-test/
│       ├── pr-create/
│       └── comment/
├── .claude/
│   ├── agents/   # .agents/agents 참조
│   └── skills/   # .agents/skills 참조
├── fe/
│   ├── package.json
│   ├── pnpm-workspace.yaml
│   ├── turbo.json
│   └── apps/web/
│       ├── package.json
│       └── src/{components,composables,stores,api,schemas,pages,router}
└── be/
    ├── pom.xml
    └── src/main/{java,resources}
```

## 스킬 라우팅

| 요청 유형 | 사용할 스킬 | 한 줄 요약 |
| --- | --- | --- |
| Vue 3 `<script setup>` 컴포넌트, 페이지, route, Pinia store, FE 전용 Zod 스키마 생성/수정 | `.agents/skills/fe-scaffold/SKILL.md` | UI/페이지 중심 FE 파일을 정해진 폴더 구조에 생성한다. |
| REST endpoint 기반 API 클라이언트, Zod 응답 스키마, endpoint 함수, TanStack Query 훅 생성 | `.agents/skills/fe-api-layer/SKILL.md` | API 연동 세트를 한 번에 만든다. |
| Vitest 단위 테스트 또는 Playwright E2E 테스트 작성/수정 | `.agents/skills/fe-test/SKILL.md` | 기존 컴포넌트/훅/사용자 플로우에 대한 테스트를 작성한다. |
| 현재 브랜치 변경분으로 커밋 정리 후 새 PR 생성 | `.agents/skills/pr-create/SKILL.md` | `gh` CLI로 인증 확인, 커밋, push, PR 생성까지 수행한다. |
| 기존 PR 리뷰 코멘트 반영 및 답글 작성 | `.agents/skills/comment/SKILL.md` | `gh api`로 리뷰 코멘트를 읽고 수정, 답글, push를 수행한다. |

스킬이 겹치면 제외 조건을 우선한다. 예를 들어 API 훅과 테스트가 모두 필요하면 먼저 `fe-api-layer`로 프로덕션 코드를 만들고, 이어서 `fe-test`로 테스트를 작성한다. 새 PR 생성과 기존 PR 리뷰 코멘트 대응은 섞지 않는다.

## 서브에이전트 라우팅

`.agents/agents`에는 반복적으로 위임하기 좋은 조사/검증 역할을 둔다.

- `frontend-architect`: Vue 앱 구조, 라우팅, 상태, UI 변경 방향 조사
- `api-contract-analyst`: FE-BE API 계약, Zod 스키마, 응답 래퍼 정합성 조사
- `backend-legacy-spring`: Spring Legacy/MyBatis/JWT 백엔드 변경 범위 조사
- `quality-reviewer`: 변경 후 테스트, lint, 빌드, 회귀 위험 점검

서브에이전트는 판단 보조용이다. 실제 파일 수정, 커밋, push, PR 생성은 현재 작업자가 최종 확인 후 수행한다.

## FE 작업 규칙

- FE 루트 명령은 `fe`에서 실행한다.
- 기본 앱은 `fe/apps/web`이다.
- 앱 내부 import는 `src` 기준 alias `@`를 사용한다.
- Vue 컴포넌트는 `<script setup lang="ts">`를 사용한다.
- Tailwind 유틸리티를 기본으로 쓰되, 기존 컴포넌트 패턴을 우선한다.
- API 데이터가 필요한 UI는 API 클라이언트/Query 훅을 먼저 정리하고 페이지에서 훅을 사용한다.

```bash
cd fe
pnpm dev
pnpm lint
pnpm build
pnpm test
pnpm test:e2e
```

### FE 폴더와 네이밍

- Vue 컴포넌트: `fe/apps/web/src/components/PascalName.vue`
- 페이지 컴포넌트: `fe/apps/web/src/pages/PascalNamePage.vue`
- Composable 및 Query 훅: `fe/apps/web/src/composables/useThing.ts`, `useThingQuery.ts`, `useThingMutation.ts`
- Pinia store: `fe/apps/web/src/stores/thing.store.ts`, export는 `useThingStore`
- API 모듈: `fe/apps/web/src/api/<resources>.ts`
- Zod 스키마: `fe/apps/web/src/schemas/thing.schema.ts`, export는 `thingSchema`, `thingListSchema`, 타입은 `Thing`
- 라우터: `fe/apps/web/src/router/index.ts`
- 단위 테스트: `fe/apps/web/tests/unit/<target>.test.ts`
- E2E 테스트: `fe/apps/web/tests/e2e/<flow>.spec.ts`

## API 계약 규칙

- 백엔드는 `/api` prefix를 사용한다. 예: `/api/users`, `/api/auth/login`.
- FE 환경 변수는 `VITE_API_BASE_URL`을 사용한다. 예: `http://localhost:8080/api`.
- 백엔드 컨트롤러는 현재 `ApiResponse<T>` 형태로 `success`, `data`, `message`를 반환한다.
- FE Zod 스키마는 실제 응답 계약과 일치해야 한다. 백엔드 연동 작업 전에는 컨트롤러, DTO, `ApiResponse`를 확인한다.
- API wrapper가 `data`를 벗겨서 반환하는지, endpoint 함수가 래퍼 스키마를 직접 파싱하는지 한 방식으로 일관되게 맞춘다.
- 인증 토큰, DB 비밀번호, JWT secret 같은 값은 `.env.example`에는 예시만 두고 실제 비밀값은 커밋하지 않는다.

## BE 작업 규칙

- BE 루트는 `be`다.
- Java 17, Spring Framework 5.3.x, Spring Security 5.8.x, MyBatis, WAR 패키징을 기준으로 한다.
- 계층은 `controller -> service -> mapper -> domain/dto` 흐름을 유지한다.
- API 응답은 기존 `ApiResponse<T>` 패턴을 따른다.
- DB 스키마와 seed 데이터는 `be/src/main/resources/db`를 확인한다.
- Docker Compose 로컬 실행은 `be/docker-compose.yml`을 기준으로 한다.

```bash
cd be
mvn test
mvn -B clean package
docker compose up --build
```

## 브랜치와 커밋

- 브랜치 형식: `<type>/<short-desc>`
- 허용 type: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`
- 커밋 형식: Conventional Commits
- 예: `feat/user-api-layer`, `fix/login-validation`, `feat: add user query hooks`

## 검증

- FE 변경 후 기본 검증:

```bash
cd fe
pnpm lint
pnpm build
pnpm test
```

- 사용자 플로우, 라우팅, Playwright 테스트 변경 시:

```bash
cd fe
pnpm test:e2e
```

- BE 변경 후 기본 검증:

```bash
cd be
mvn test
mvn -B clean package
```

- 문서/스킬/에이전트만 변경한 경우에는 관련 파일 링크 구조와 Markdown 내용을 확인하고, 코드 빌드는 생략해도 된다.
