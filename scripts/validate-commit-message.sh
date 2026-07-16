#!/usr/bin/env sh

set -eu

message_file="${1:?커밋 메시지 파일 경로가 필요합니다.}"
subject="$(sed -n '1p' "$message_file")"
pattern='^(\[(fe|be)\](feat|refac|chore|docs|style|fix|hotfix|revert|ai)|chore): .+$'

if ! printf '%s\n' "$subject" | grep -Eq "$pattern"; then
  printf '%s\n' '커밋 메시지는 [fe|be]접두어: 기능명 또는 프로젝트 공통 chore: 기능명 형식이어야 합니다.' >&2
  printf '%s\n' '예: [fe]feat: 결제 내역 화면 추가, chore: Lefthook 컨벤션 추가' >&2
  exit 1
fi
