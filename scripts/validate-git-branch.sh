#!/usr/bin/env sh

set -eu

branch="$(git branch --show-current)"

if [ -z "$branch" ] || [ "$branch" = 'develop' ] || [ "$branch" = 'main' ]; then
  exit 0
fi

pattern='^(fe|be)/(feat|refac|chore|docs|style|fix|hotfix|revert|ai)/[a-z0-9]+(-[a-z0-9]+)*$'

if ! printf '%s\n' "$branch" | grep -Eq "$pattern"; then
  printf '%s\n' '브랜치명은 개발-파트/접두어/케밥-기능명 형식이어야 합니다.' >&2
  printf '%s\n' '예: fe/feat/payment-history, be/fix/date-filter' >&2
  exit 1
fi
