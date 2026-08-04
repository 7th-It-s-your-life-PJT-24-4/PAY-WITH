---
name: feat-wt
description: "Create an isolated feature worktree, then implement the requested work there. Use when the user says `feat-wt` with a task, asks to start a feature in a worktree, or wants a latest develop/dev based feature branch prepared without disturbing the primary checkout."
---

# Feat Worktree

`feat-wt <작업 내용>` 요청을 독립 워크트리에서 수행한다. 기본 브랜치는 최신 원격 개발 브랜치이고, 기능 브랜치는 저장소 컨벤션을 따른다.

## Workflow

1. 원본 저장소 루트와 현재 상태를 확인한다.
   - `git rev-parse --show-toplevel`, `git status --short`, `git worktree list`를 확인한다.
   - 원본 워크트리에 사용자의 변경이 있어도 되돌리거나 stash하지 않는다.

2. 원격 참조를 최신화하고 기준 브랜치를 선택한다.
   - 먼저 `git fetch origin`을 실행한다.
   - 우선순위는 `origin/develop`, `origin/dev`, 로컬 `develop`, 로컬 `dev`다.
   - 원격 기준 브랜치가 있으면 새 브랜치를 그 ref에서 직접 만든다. 원본 워크트리의 `develop`을 checkout하거나 merge하지 않는다.
   - 기준 브랜치를 찾지 못하면 중단하고 사용자에게 기준 브랜치를 물어본다.

3. 작업 내용에서 브랜치 이름을 정한다.
   - 대상 경로 또는 요청 내용을 근거로 개발 파트를 `fe` 또는 `be`로 정한다. 판단할 근거가 없으면 먼저 확인한다.
   - 기능 작업은 `<part>/feat/<english-kebab-case-slug>` 형식으로 만든다. 예: `fe/feat/auth-api`.
   - 버그 수정·문서화처럼 기능이 아닌 작업이면 저장소 허용 접두어(`fix`, `docs`, `chore` 등)를 써서 `<part>/<prefix>/<slug>`로 만든다.
   - 같은 이름의 로컬 브랜치, 원격 브랜치 또는 워크트리가 이미 있으면 새로 만들거나 덮어쓰지 말고 사용자에게 재사용/새 이름을 확인한다.

4. 워크트리를 생성한다.
   - 기본 위치는 원본 저장소 밖의 `<repo-parent>/.worktrees/<repo-name>-<branch-with-slashes-replaced>`다.
   - 사용자가 위치를 지정했으면 그 위치를 사용한다.
   - 대상 디렉터리가 비어 있지 않으면 중단한다.
   - `git worktree add -b <branch> <worktree-path> <base-ref>`로 생성하고, 생성 직후 branch·HEAD·상태를 확인한다.

5. 이후 모든 구현·검증·커밋·PR 작업은 새 워크트리에서만 수행한다.
   - 새 워크트리의 `AGENTS.md`를 읽고 요청에 맞는 프로젝트 스킬을 적용한다.
   - 원본 워크트리에는 fetch 외의 변경을 만들지 않는다.
   - 작업 시작 시 사용자에게 기준 ref, 브랜치, 워크트리 경로를 알린다.

## Safety

- `git worktree remove --force`, `git reset --hard`, 사용자 변경의 stash/삭제를 실행하지 않는다.
- 원본 워크트리가 더럽다는 이유만으로 작업을 중단하지 않는다. 새 워크트리를 기준 ref에서 만들 수 있으면 계속한다.
- 로컬 `develop`/`dev`만 기준으로 쓸 수 있는 경우에는 원격 최신 여부를 확인할 수 없음을 사용자에게 알린다.
- 작업 완료 후 워크트리는 사용자의 명시 요청이 있기 전까지 제거하지 않는다.
