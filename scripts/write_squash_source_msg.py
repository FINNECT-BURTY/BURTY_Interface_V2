#!/usr/bin/env python3
"""GIT_EDITOR: write squash result message for combined 소스 제거 commit."""

import sys

MSG = """CHORE: 소스 제거

무엇: com.nuri 패키지 및 Nuri 전용 테스트 소스 일괄 제거
왜: Berty 이관 완료 후 레거시 경로·의존 혼선 방지 및 유지 범위 축소
"""

if __name__ == "__main__":
    path = sys.argv[1]
    existing = open(path, encoding="utf-8", errors="replace").read()
    if "combination of" in existing.lower() or "Please enter the commit message" in existing:
        open(path, "w", encoding="utf-8").write(MSG)
