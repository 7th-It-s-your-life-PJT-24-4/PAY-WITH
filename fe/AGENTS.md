# FE 작업 지침

루트 `AGENTS.md`의 공통 지침과 FE 작업 규칙을 함께 따른다. 이 문서는 `fe` 하위의 역할별 페이지 구조를 정의한다.

## 역할별 페이지 구조

- 역할별 화면은 `apps/web/src/pages/<role>` 아래에 둔다. 현재 역할 폴더명은 `ward`, `guard`를 사용한다.
- 각 라우트 세그먼트는 디렉터리로 표현하고, 진입 페이지는 `page.vue`를 사용한다. Vue 컴포넌트 파일명은 전역적으로 고유할 필요가 없으며, import 시 경로와 로컬 식별자로 구분한다.
- 이 프로젝트는 Vue 3/Vite를 사용하므로 컴포넌트 파일 확장자는 `.tsx`가 아닌 `.vue`를 사용한다. 파일 기반 라우팅은 적용하지 않으므로 `apps/web/src/router/index.ts`에서 각 `page.vue`를 해당 URL에 명시적으로 연결한다.
- 역할 화면에서만 사용하는 하위 컴포넌트는 해당 역할 폴더의 `-components`에 둔다.
- 상세 화면의 동적 세그먼트는 숫자 전용 `:id(\\d+)`를 사용하고 디렉터리는 `[id]`로 표현한다. 단계형 플로우처럼 ID의 의미가 상태 복구에 필요한 경우에만 `:transactionId` 등 구체적인 이름을 유지한다.
- 라우트 전용 하위 컴포넌트와 유틸리티는 해당 라우트 디렉터리의 `-components`, `-utils`에 둔다.

```text
apps/web/src/pages/
├── ward/
│   ├── layout.vue
│   ├── page.vue
│   └── -components/
└── guard/
    ├── layout.vue
    ├── page.vue
    ├── -components/
    └── charge/
        ├── page.vue            # /guard/charge
        └── [id]/
            └── page.vue        # /guard/charge/:id(\d+)
```

여러 단계로 구성된 라우트도 URL 계층을 그대로 디렉터리에 반영한다.

```text
ward/transfer/
├── page.vue                     # /ward/transfer
├── account/
│   └── page.vue                 # /ward/transfer/account
└── [transactionId]/
    └── complete/
        └── page.vue             # /ward/transfer/:transactionId/complete
```
