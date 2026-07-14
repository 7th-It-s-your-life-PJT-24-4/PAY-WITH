---
name: pr-create
description: "Pull request creation workflow for this repository. Use when the user asks to create/open a PR, prepare current branch changes for PR, commit changes, push a branch, or generate a PR body from the current diff. Uses gh CLI by default and GitHub REST API only if gh cannot cover a required step. Do not use for addressing existing PR review comments; use comment. Do not use for initial code review or writing review feedback."
---

# pr-create

Use this skill to turn current branch changes into a GitHub PR.

## Workflow

1. Read root `AGENTS.md` and follow branch and commit conventions.
2. Confirm Git and GitHub state:

```bash
git status --short
git branch --show-current
gh auth status
```

If the working directory is not a Git repository, stop and report that PR creation cannot proceed from the current directory.

3. If not on a branch matching `<type>/<short-desc>`, create or rename only after confirming intent if the current branch has a meaningful existing name.
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

6. Stage and commit with Conventional Commits:

```bash
git add <changed-files>
git commit -m "feat: add user api layer"
```

7. Push the branch:

```bash
git push -u origin "$(git branch --show-current)"
```

8. Create PR with `gh pr create`.

## PR body template

Write only facts visible in the diff or command output.

```md
## 개요

- 

## 변경사항

- 

## 테스트

- [ ] `cd fe && pnpm lint`
- [ ] `cd fe && pnpm build`
- [ ] `cd fe && pnpm test`
- [ ] `cd fe && pnpm test:e2e`

## 참고

- 
```

Use checked boxes only for commands actually run and passed. If a command was not run, leave it unchecked and add a short reason.

## Command pattern

```bash
cat > /tmp/pr-body.md <<'EOF'
## 개요

- 현재 diff 기반 요약

## 변경사항

- 변경 파일과 동작 기준으로 작성

## 테스트

- [x] `cd fe && pnpm lint`
- [x] `cd fe && pnpm build`
- [x] `cd fe && pnpm test`
- [ ] `cd fe && pnpm test:e2e` - E2E 영향 없음

## 참고

- 없음
EOF

gh pr create \
  --base main \
  --head "$(git branch --show-current)" \
  --title "feat: add user api layer" \
  --body-file /tmp/pr-body.md
```

## Rules

- Do not invent business context, issue numbers, tests, screenshots, or reviewers.
- Do not include unrelated changes in the commit. If unrelated files are modified, ask before staging them.
- Do not rewrite user commits unless explicitly requested.
- If `gh auth status` fails, tell the user to authenticate and stop before staging/committing.
- If validation fails, fix the issue or report the failure; do not create a PR that claims passing tests.
- For documentation-only changes, skip FE/BE validation unless the changed docs describe executable examples that should be checked.
