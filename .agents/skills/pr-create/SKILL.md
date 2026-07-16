---
name: pr-create
description: "Mandatory pull request creation workflow for this repository. Always use when the user asks to create/open a PR, prepare current branch changes for PR, commit changes, push a branch, generate a PR title/body, or turn local changes into a PR. Uses gh CLI by default and GitHub REST API only if gh cannot cover a required step. Do not use for addressing existing PR review comments; use comment. Do not use for initial code review or writing review feedback."
---

# pr-create

Use this skill to turn current branch changes into a GitHub PR. This skill is mandatory for commit, push, and new PR requests in this repository.

## Workflow

1. Read root `AGENTS.md` and follow branch and commit conventions.
2. Confirm Git and GitHub state:

```bash
git status --short
git branch --show-current
gh auth status
gh api user --jq .login
```

If the working directory is not a Git repository, stop and report that PR creation cannot proceed from the current directory.

3. Identify the PR type and follow the matching Git flow:
   - Feature PR: branch from `develop`, branch format `[개발 파트]접두어/기능명` (for example, `fe/feat/user-api-layer`), and create a PR to `develop`.
   - Release PR: create a `develop` to `main` release PR body only when requested. Do not merge it through GitHub; the user must complete the local rebase flow in root `AGENTS.md`.
   - If the current branch has a meaningful existing name but does not match the required format, confirm before creating or renaming it.
4. Inspect the diff. PR content must be based only on actual diff:

```bash
git diff --stat
git diff
```

5. Run relevant validation before committing:

```bash
cd fe
pnpm lint
pnpm build
pnpm test
```

Run `pnpm test:e2e` when UI flow, routing, or Playwright tests changed.
Run `cd be && mvn test && mvn -B clean package` when backend code changed.

6. Stage and commit with the project convention. The development part must be `fe` or `be`; the prefix must be one of `feat`, `refac`, `chore`, `docs`, `style`, `fix`, `hotfix`, `revert`, or `ai`:

```bash
git add <changed-files>
git commit -m "[fe]feat: 사용자 API 레이어 추가"
```

7. Push the branch:

```bash
git push -u origin "$(git branch --show-current)"
```

8. Create a feature PR with `gh pr create` using `develop` as its base branch and a title following `[개발 파트]접두어: 기능명`. Assign it to the authenticated GitHub user. Prefer `--assignee @me`; if that fails, resolve the login with `gh api user --jq .login` and retry with `--assignee "$LOGIN"`.

Feature PRs must use squash merge and receive at least one approval before merging. Do not merge PRs unless the user explicitly asks and the repository permissions permit it.

## PR body template

Write only facts visible in the diff or command output.

```md
## 📝 작업 내용 요약

- 

## ✅ 체크리스트

- [ ] `develop` 브랜치의 최신 코드를 `pull` 받았나요?
- [ ] 빌드가 통과했나요?

## 🤖 AI 리뷰

<!-- 발견 사항. 자동 생성 시 채워짐 -->

## 💬 기타 코멘트

<!-- 후속 작업·리뷰어에게 남길 메모 등 (없으면 비움) -->
```

Use checked boxes only for items that are true:
- Check the `develop` pull item only when `develop` exists and the branch was updated from it.
- Check the build item only when relevant build/validation commands actually ran and passed.
- When `develop` does not exist, leave the item unchecked and explain the actual base branch in `기타 코멘트`.
- Put AI-generated review findings or validation notes under `AI 리뷰`.

## Release PR body template

Use this only for a requested `develop` → `main` release PR. Replace the version fields only with values supplied by the user or confirmed from repository release metadata.

```md
## Release v0.0.0

> 배포 대상: `develop` → `main`
> 릴리스 일자: YYYY-MM-DD

## 📦 버전

- Version: `v0.0.0`
- 이전 버전: `v0.0.0`
- 변경 범위: <!-- major / minor / patch -->
```

The release merge is performed locally to preserve the intended linear history:

```bash
git checkout main
git fetch origin
git rebase origin/develop
git push origin main
```

## Command pattern

```bash
cat > /tmp/pr-body.md <<'EOF'
## 📝 작업 내용 요약

- 현재 diff 기반 요약

## ✅ 체크리스트

- [ ] `develop` 브랜치의 최신 코드를 `pull` 받았나요?
- [x] 빌드가 통과했나요?

## 🤖 AI 리뷰

- `cd fe && pnpm lint`, `pnpm build`, `pnpm test` 통과

## 💬 기타 코멘트

- `develop` 브랜치가 없어 base branch는 `main` 기준으로 생성
EOF

gh pr create \
  --base develop \
  --head "$(git branch --show-current)" \
  --title "[fe]feat: 사용자 API 레이어 추가" \
  --body-file /tmp/pr-body.md \
  --assignee @me
```

## Rules

- Do not invent business context, issue numbers, tests, screenshots, or reviewers.
- Do not include unrelated changes in the commit. If unrelated files are modified, ask before staging them.
- Do not rewrite user commits unless explicitly requested.
- If `gh auth status` fails, tell the user to authenticate and stop before staging/committing.
- Every newly created PR must be assigned to the authenticated GitHub user using `--assignee @me` or the login from `gh api user --jq .login`.
- If validation fails, fix the issue or report the failure; do not create a PR that claims passing tests.
- For documentation-only changes, skip FE/BE validation unless the changed docs describe executable examples that should be checked.
