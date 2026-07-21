# AGENTS.md

이 저장소에서 Claude/Codex 계열 에이전트가 가장 먼저 읽는 루트 지침이다. 모든 답변과 진행 상황은 한국어로 작성하고, 실제 작업은 파일과 명령 결과를 확인한 뒤 수행한다.

## 운영 구조

- AI 스킬과 서브에이전트는 반드시 `.agents` 아래에 원본을 추가한다.
- `.agents/skills`는 프로젝트 스킬의 원본 위치다.
- `.agents/agents`는 프로젝트 전용 서브에이전트 정의의 원본 위치다.
- `.claude/skills`는 `.agents/skills`의 각 스킬을 symlink로 참조한다.
- `.claude/agents`는 `.agents/agents`의 각 에이전트를 symlink로 참조한다.
- `.claude/skills`와 `.claude/agents`에는 원본 파일을 직접 작성하지 않는다. Claude 호환을 위해 필요한 항목만 `.agents` 원본을 가리키는 symlink로 둔다.
- 스킬을 추가하거나 수정할 때는 `.agents/skills/<name>/SKILL.md`를 수정하고, Claude에서 써야 하면 `.claude/skills/<name>` symlink만 추가한다.
- 서브에이전트를 추가하거나 수정할 때는 `.agents/agents/<name>.md`를 수정하고, Claude에서 써야 하면 `.claude/agents/<name>.md` symlink만 추가한다.
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
- Package/Monorepo: pnpm workspace, Turborepo, catalog 기반 버전 관리
- Styling: Tailwind CSS
- State/Data: Pinia, TanStack Query `@tanstack/vue-query`
- Validation/Test: Zod, Vitest, Playwright
- Backend: Spring Framework 5.x 기반 Legacy WAR, Java 17, MyBatis, Spring Security, JWT, MySQL
- Build: Gradle(Groovy DSL), Gradle Wrapper(`./gradlew`) 기준
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
│       ├── be-scaffold/
│       ├── be-test/
│       ├── pr-create/
│       └── comment/
├── .claude/
│   ├── agents/   # .agents/agents 참조
│   └── skills/   # .agents/skills 참조
├── fe/
│   ├── package.json
│   ├── pnpm-workspace.yaml
│   ├── turbo.json
│   ├── apps/
│   │   ├── web/
│   │   │   ├── package.json
│   │   │   └── src/{components,composables,stores,api,schemas,pages,router}
│   │   └── storybook/
│   │       ├── .storybook/
│   │       ├── src/stories/
│   │       └── package.json
│   └── packages/ui/
│       ├── src/{components,index.ts}
│       └── tests/unit/
└── be/
    ├── build.gradle
    ├── settings.gradle
    ├── gradlew
    └── src/main/
        ├── java/com/paywith/
        │   ├── common/, config/, exception/, security/   (도메인 공용 인프라)
        │   └── <domain>/{controller,service,mapper,domain,dto}   (예: user/, auth/)
        └── resources/mappers/<domain>/<Name>Mapper.xml
```

## 스킬 라우팅

| 요청 유형                                                                                 | 사용할 스킬                            | 한 줄 요약                                                 |
| ----------------------------------------------------------------------------------------- | -------------------------------------- | ---------------------------------------------------------- |
| Vue 3 `<script setup>` 컴포넌트, 페이지, route, Pinia store, FE 전용 Zod 스키마 생성/수정 | `.agents/skills/fe-scaffold/SKILL.md`  | UI/페이지 중심 FE 파일을 정해진 폴더 구조에 생성한다.      |
| REST endpoint 기반 API 클라이언트, Zod 응답 스키마, endpoint 함수, TanStack Query 훅 생성 | `.agents/skills/fe-api-layer/SKILL.md` | API 연동 세트를 한 번에 만든다.                            |
| Vitest 단위 테스트 또는 Playwright E2E 테스트 작성/수정                                   | `.agents/skills/fe-test/SKILL.md`      | 기존 컴포넌트/훅/사용자 플로우에 대한 테스트를 작성한다.   |
| Spring Legacy 리소스의 controller-service-mapper-domain/dto 풀스택 생성/수정             | `.agents/skills/be-scaffold/SKILL.md`  | `ApiResponse<T>` 계약을 따르는 BE 리소스 계층을 한 번에 만든다. |
| JUnit/Mockito 단위 테스트 작성/수정                                                       | `.agents/skills/be-test/SKILL.md`      | 기존 controller/service/mapper에 대한 BE 테스트를 작성한다. |
| 현재 브랜치 변경분으로 커밋 정리 후 새 PR 생성                                            | `.agents/skills/pr-create/SKILL.md`    | `gh` CLI로 인증 확인, 커밋, push, PR 생성까지 수행한다.    |
| 기존 PR 리뷰 코멘트 반영 및 답글 작성                                                     | `.agents/skills/comment/SKILL.md`      | `gh api`로 리뷰 코멘트를 읽고 수정, 답글, push를 수행한다. |

스킬이 겹치면 제외 조건을 우선한다. 예를 들어 API 훅과 테스트가 모두 필요하면 먼저 `fe-api-layer`로 프로덕션 코드를 만들고, 이어서 `fe-test`로 테스트를 작성한다. BE도 동일하게 신규 리소스와 테스트가 모두 필요하면 먼저 `be-scaffold`로 프로덕션 코드를 만들고, 이어서 `be-test`로 테스트를 작성한다. 커밋, push, PR 생성, PR 본문 작성, PR 제목 생성, 현재 변경분으로 PR 올리기 요청은 반드시 `pr-create`를 사용한다. 새 PR 생성과 기존 PR 리뷰 코멘트 대응은 섞지 않는다.

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
- 패키지 내부 소스와 테스트 import는 `src` 기준 alias `@/`를 사용한다. 파일 시스템 절대경로 import는 사용하지 않으며, 상대 import는 같은 파일의 스타일·에셋 등 별칭 적용이 불가능한 경우에만 쓴다.
- 워크스페이스 간 import는 공개 API인 `@pay-with/*` 패키지명만 사용한다. 다른 패키지의 `src`를 상대경로로 직접 import하지 않는다.
- Vite, TypeScript, Storybook 설정 파일의 파일 탐색 경로는 이동 가능한 설정을 위해 `fileURLToPath(new URL(..., import.meta.url))` 또는 설정 기준 상대경로를 사용한다. 개발 머신의 절대 파일 시스템 경로를 커밋하지 않는다.
- 외부 의존성 버전은 `fe/pnpm-workspace.yaml`의 `catalog`에서만 관리한다. 각 패키지는 실제 사용하는 의존성만 `catalog:`로 선언하고, 워크스페이스 내부 의존성은 `workspace:*`를 쓴다.
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

### UI 패키지와 Storybook

- 재사용 UI 컴포넌트는 `fe/packages/ui/src/components`에 작성하고 `src/index.ts`에서 공개한다.
- `fe/apps/storybook`의 스토리는 `@pay-with/ui` 공개 API만 import한다. 패키지 내부 경로를 직접 import하지 않는다.
- UI 컴포넌트는 Tailwind 유틸리티로 스타일링한다. Tailwind를 사용하는 앱과 Storybook의 CSS는 `@source`로 `packages/ui/src`를 스캔한다.
- Tooltip, Dialog, Select, Popover 등 접근성·포커스 관리가 필요한 컴포넌트는 Reka UI 프리미티브를 감싼다. 네이티브 요소로 충분한 컴포넌트에는 불필요한 프리미티브를 추가하지 않는다.
- UI 컴포넌트의 props, emits, disabled 상태 등 동작 계약은 `fe/packages/ui/tests/unit`의 Vitest로 검증한다. Storybook과 Chromatic은 시각 회귀 및 문서화에 사용한다.
- Storybook 명령은 `cd fe && pnpm --filter @pay-with/storybook dev`, `pnpm --filter @pay-with/storybook build`를 사용한다. Chromatic 토큰은 환경 변수 또는 CI secret으로만 전달하고 저장소에 기록하지 않는다.

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
- 패키지는 도메인 단위로 나누고(`com.paywith.<domain>`), 각 도메인 안에서 `controller -> service -> mapper -> domain/dto` 흐름을 유지한다. `common`, `config`, `exception`, `security`는 도메인 공용 인프라라 도메인 패키지로 옮기지 않는다.
- API 응답은 기존 `ApiResponse<T>` 패턴을 따른다.
- DB 스키마와 seed 데이터는 `be/src/main/resources/db`를 확인한다.
- Docker Compose 로컬 실행은 `be/docker-compose.yml`을 기준으로 한다.

```bash
cd be
./gradlew test
./gradlew clean build
docker compose up --build
```

## Git 컨벤션과 흐름

### 접두어

브랜치, 커밋, PR 제목에는 다음 접두어만 사용한다.

| 접두어 | 설명 |
| --- | --- |
| `feat` | 기능 개발 |
| `refac` | 리팩토링 |
| `chore` | 기타 환경 설정 |
| `docs` | 문서 |
| `style` | 스타일(FE) |
| `fix` | 버그 수정 |
| `hotfix` | 핫픽스 |
| `revert` | 리버트 |
| `ai` | AI 관련 설정(Claude, Codex 등) |

### 브랜치와 커밋

- 브랜치는 `develop`에서 생성하며 형식은 `[개발 파트]접두어/기능명`이다. 개발 파트는 `fe` 또는 `be`를 사용하고 기능명은 케밥 케이스로 작성한다.
- 예: `be/feat/transaction-form`, `fe/fix/date-filter`
- 커밋 메시지 형식은 `[개발 파트]접두어: 기능명`이다.
- 예: `[be]feat: 로그인 API 연동`, `[fe]fix: 날짜 필터 수정`
- PR 제목도 커밋과 같은 `[개발 파트]접두어: 기능명` 형식을 사용한다.

### Git 흐름

- `develop`과 `main`은 선형 히스토리로 관리한다.
- 기능 작업은 `develop`에서 기능 브랜치를 생성하고, 세부 기능 단위로 커밋·push한다.
- 기능 PR은 기능 브랜치에서 `develop`으로 생성하며 squash merge를 사용한다. 최소 1명의 리뷰어 승인을 받은 뒤 머지한다.
- 릴리스는 `develop`에서 `main`으로 반영하며 rebase merge를 사용한다. GitHub에서 머지하지 않고 로컬에서 다음 순서로 반영한다.

```bash
git checkout main
git fetch origin
git rebase origin/develop
git push origin main
```

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
./gradlew test
./gradlew clean build
```

- 문서/스킬/에이전트만 변경한 경우에는 관련 파일 링크 구조와 Markdown 내용을 확인하고, 코드 빌드는 생략해도 된다.
