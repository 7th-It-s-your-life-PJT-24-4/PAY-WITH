# FE 작업 지침

루트 `AGENTS.md`의 공통 지침과 FE 작업 규칙을 함께 따른다. 이 문서는 `fe` 하위의 역할별 페이지 구조를 정의한다.

## 역할별 페이지 구조

- 역할별 화면은 `apps/web/src/pages/<role>` 아래에 둔다. 현재 역할 폴더명은 `ward`, `guardian`을 사용한다.
- 역할 폴더 안에서는 `layout.vue`, `page.vue`처럼 동일한 파일명을 사용한다. Vue 컴포넌트 파일명은 전역적으로 고유할 필요가 없으며, import 시 경로와 로컬 식별자로 구분한다.
- 이 프로젝트는 Vue 3/Vite를 사용하므로 컴포넌트 파일 확장자는 `.tsx`가 아닌 `.vue`를 사용한다. 파일 기반 라우팅은 적용하지 않으므로 `apps/web/src/router/index.ts`에서 각 `page.vue`를 해당 URL에 명시적으로 연결한다.
- 역할 화면에서만 사용하는 하위 컴포넌트는 해당 역할 폴더의 `-components`에 둔다.
- 상세 화면은 `[id]/page.vue`에 두고 router에서는 `:id` 동적 세그먼트로 연결한다. 수정 화면은 `edit/page.vue`에 둔다.

```text
apps/web/src/pages/
├── ward/
│   ├── layout.vue
│   ├── page.vue
│   └── -components/
└── guardian/
    ├── layout.vue
    ├── page.vue
    ├── -components/
    ├── [id]/
    │   └── page.vue            # /guardian/:id
    └── edit/
        └── page.vue            # /guardian/edit
```
