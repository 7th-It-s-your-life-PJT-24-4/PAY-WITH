---
name: comment
description: "PR review comment response workflow for this repository. Use when the user asks to handle, address, fix, or reply to existing GitHub PR review comments. Reads line-level review comments with gh api, groups unresolved threads, inspects file and line context, applies code changes when justified, replies to comments through GitHub review comment replies endpoints, commits and pushes updates. Do not use to create a new PR; use pr-create. Do not use to perform an initial code review or leave first-pass review comments."
---

# comment

Use this skill to address existing PR review comments.

## Workflow

1. Read root `AGENTS.md` and follow branch and commit conventions.
2. Confirm this is a Git repository and identify the PR:

```bash
git status --short
gh pr view --json number,headRefName,baseRefName,url
```

If there is no current PR, ask the user for the PR number.

3. Fetch line-level review comments:

```bash
PR_NUMBER="$(gh pr view --json number --jq .number)"
OWNER_REPO="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
gh api "repos/${OWNER_REPO}/pulls/${PR_NUMBER}/comments" \
  --paginate \
  --jq '.[] | {id, path, line, original_line, side, body, user: .user.login, in_reply_to_id, position, commit_id, diff_hunk}'
```

4. Group comments by thread:
   - Root comment: comment with no `in_reply_to_id`.
   - Replies: comments whose `in_reply_to_id` points to the root.
   - Treat a thread as unresolved unless later replies clearly show it was fixed or dismissed.
5. For each unresolved thread, inspect the file and nearby lines:

```bash
sed -n '<start>,<end>p' <path>
```

6. Decide:
   - Apply the requested code change when it is correct and consistent with the codebase.
   - Do not blindly apply suggestions that weaken type safety, break conventions, or conflict with requirements.
   - If judgment is needed, explain the tradeoff in the reply and propose an alternative.
7. Run relevant validation:

```bash
cd fe
pnpm lint
pnpm build
pnpm test
```

Run `pnpm test:e2e` when user flows changed.
Run `cd be && ./gradlew test && ./gradlew clean build` when backend code changed.

8. Commit and push fixes:

```bash
git status --short
git add <changed-files>
git commit -m "fix: address pr review comments"
git push
```

9. Reply to each handled root comment using the replies endpoint:

```bash
OWNER_REPO="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
COMMENT_ID="<root-review-comment-id>"

gh api \
  --method POST \
  "repos/${OWNER_REPO}/pulls/comments/${COMMENT_ID}/replies" \
  -f body='반영했습니다. 구체적으로 <파일/함수>에서 <수정 내용>을 변경했고, `pnpm lint`, `pnpm build`, `pnpm test` 통과를 확인했습니다.'
```

## Reply rules

- Reply in Korean unless the PR discussion is clearly in English.
- State exactly what changed and where.
- Mention validation commands that were actually run.
- If not applying a suggestion, state the reason and propose a concrete alternative.
- Do not mark work as done unless the code was changed or a justified decision was posted.

## REST API notes

Use `gh api` for review comments because line-level replies require the Pull Request Review Comments API.

- List review comments: `GET /repos/{owner}/{repo}/pulls/{pull_number}/comments`
- Reply to a review comment: `POST /repos/{owner}/{repo}/pulls/comments/{comment_id}/replies`

## Safety rules

- Do not force-push unless the user explicitly asks.
- Do not resolve by deleting unrelated code.
- Do not respond to stale comments without checking current file context.
- Do not create a new PR from this workflow.
- If the working directory is not a Git repository, report that review comment handling cannot proceed from the current directory.
