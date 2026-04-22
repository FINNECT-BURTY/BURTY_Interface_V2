# 커밋 메시지와 GitHub 이슈 연결

## 규칙

- **첫 줄(제목)** 끝에 GitHub 이슈 번호를 붙입니다: `FEAT : 설명 (#3)`
- `Merge branch` / `Merge pull` 커밋은 그대로 두는 것이 일반적입니다.
- 이미 `#1` … `#7` 형태가 제목에 있으면 중복 추가하지 않습니다.

## 저장소 이슈

[hyperwiseLab/Almighty](https://github.com/hyperwiseLab/Almighty/issues) — `#1`~`#7` 주제별 이슈가 등록되어 있습니다.

## 자동 부여 스크립트

매핑된 커밋에만 제목 끝에 `(#N)`을 붙이려면:

```bash
FILTER_BRANCH_SQUELCH_WARNING=1 git filter-branch -f \
  --msg-filter "python3 $(pwd)/.github/scripts/msg_append_issue.py" \
  -- --branches
```

- 매핑은 [.github/scripts/msg_append_issue.py](scripts/msg_append_issue.py)의 `SHORT_TO_ISSUE`를 수정합니다.
- **모든 브랜치 히스토리가 바뀝니다.** 이미 실행한 뒤라면 다시 실행하지 마세요.
- 원격에 반영하려면 팀과 협의 후 `git push --force-with-lease`가 필요합니다.

## 수동으로 쓸 때

새 작업 시 제목에 이슈 번호를 포함하면 GitHub에서 자동으로 링크됩니다.

예: `fix: WebSocket 허용 오리진 검증 (#3)`
