#!/usr/bin/env python3
"""
git filter-branch --msg-filter: enrich commit bodies (Korean, 무엇/왜 중심, 72자 wrap).
Reads old message from stdin; uses env GIT_COMMIT.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

WIDTH = 72
ROOT = Path(__file__).resolve().parent.parent


def wrap(s: str, width: int = WIDTH) -> list[str]:
    s = s.strip()
    if not s:
        return []
    out: list[str] = []
    while len(s) > width:
        cut = s.rfind(" ", 0, width + 1)
        if cut <= 0:
            cut = width
        out.append(s[:cut].rstrip())
        s = s[cut:].lstrip()
    if s:
        out.append(s)
    return out


def strip_cursor_trailer(text: str) -> str:
    lines = []
    for ln in text.splitlines():
        if ln.strip().startswith("Co-authored-by:"):
            continue
        lines.append(ln)
    return "\n".join(lines).strip()


def git_lines(cmd: list[str]) -> str:
    return subprocess.check_output(cmd, text=True, cwd=ROOT, errors="replace").strip()


def layer_hint(path: str) -> str:
    p = path.replace("\\", "/")
    rules: list[tuple[str, str]] = [
        ("src/main/java/com/berty/adapter/in/web/", "웹 API(인커밍 어댑터)"),
        ("src/main/java/com/berty/adapter/out/", "외부 연동(아웃고잉 어댑터)"),
        ("src/main/java/com/berty/application/service/", "애플리케이션 서비스"),
        ("src/main/java/com/berty/application/port/in/", "인커밍 포트(유스케이스)"),
        ("src/main/java/com/berty/application/port/out/", "아웃고잉 포트"),
        ("src/main/java/com/berty/domain/entity/", "영속 엔티티"),
        ("src/main/java/com/berty/domain/model/", "도메인 모델"),
        ("src/main/java/com/berty/domain/repository/", "저장소 인터페이스"),
        ("src/main/java/com/berty/config/", "Spring 설정"),
        ("src/main/java/com/berty/security/", "보안·인증"),
        ("src/main/java/com/berty/core/", "공통 인프라"),
        ("src/test/java/com/berty/", "Berty 단위·통합·E2E 테스트"),
        ("src/main/resources/", "실행 리소스·프로퍼티"),
        ("src/test/resources/", "테스트 전용 리소스"),
        ("berty-ERD/", "BERTY DB 스키마·SQL"),
        ("docs/", "요구·기능·화면 문서"),
        (".github/", "GitHub 협업·워크플로"),
        ("scripts/", "자동화·보조 스크립트"),
    ]
    for prefix, hint in rules:
        if p.startswith(prefix):
            return hint
    return "프로젝트 자산"


def status_word(st: str) -> str:
    if st.startswith("R"):
        return "이름 변경·이동"
    return {"A": "신규 추가", "M": "내용 수정", "D": "파일 삭제"}.get(st, st)


def build_body(subject: str, files: list[tuple[str, str]]) -> str:
    if not files:
        return ""

    # Squashed bulk delete (many nuri files)
    if len(files) > 30 and all(
        f[1].startswith("src/main/java/com/nuri/") or f[1].startswith("src/test/java/com/nuri/")
        for f in files
    ):
        mains = sum(1 for _, p in files if "/main/" in p)
        tests = sum(1 for _, p in files if "/test/" in p)
        paras = [
            f"무엇: com.nuri 패키지 기준 소스 {mains}건·테스트 {tests}건 일괄 삭제",
            "왜: Berty(com.berty) 이관 후 동일 도메인 이중 정의·클래스패스 혼선을 "
            "제거하고 유지보수 범위를 Berty 단일 트리로 고정",
            "범위: 어댑터·서비스·도메인·보안·테스트 등 Nuri 전용 트리 전부",
            "효과: 빌드·정적 분석·리뷰 시 참조 대상 단순화 및 배포 산출물 일관성 확보",
        ]
        all_lines: list[str] = []
        for p in paras:
            all_lines.extend(wrap(p))
        return "\n".join(all_lines)

    st, path = files[0]
    nm = Path(path).name
    layer = layer_hint(path)
    act = status_word(st)

    paras = [
        f"무엇: {layer}에 `{nm}` {act} (`{path}`)",
        f"왜: 변경 단위를 파일 단위로 남겨 리뷰·이슈·롤백 시 원인 추적과 "
        f"영향 범위(의존 모듈·API 계약) 파악을 쉽게 하기 위함",
    ]

    if len(files) > 1:
        extra = min(3, len(files) - 1)
        tail = ", ".join(f"`{Path(p).name}`" for _, p in files[1 : 1 + extra])
        more = len(files) - 1 - extra
        line = f"동일 커밋 포함: {tail}"
        if more > 0:
            line += f" 외 {more}건"
        paras.append(line)

    if "/test/" in path or nm.endswith("Tests.java"):
        paras.append(
            "확인: `./gradlew test` 또는 IDE에서 해당 스위트 실행으로 회귀 검증 가능"
        )

    if path.endswith("Controller.java") and "com/berty/" in path:
        paras.append(
            "참고: OpenAPI·클라이언트와의 계약은 DTO·응답 코드와 함께 검토 권장"
        )

    lines: list[str] = []
    for p in paras:
        lines.extend(wrap(p))
    return "\n".join(lines)


def main() -> None:
    commit = os.environ.get("GIT_COMMIT", "")
    old = sys.stdin.read()
    old = strip_cursor_trailer(old)
    lines = old.splitlines()
    subject = lines[0] if lines else "CHORE: 메시지 정비"

    try:
        parents = git_lines(["git", "rev-parse", f"{commit}^@"]).splitlines()
    except subprocess.CalledProcessError:
        parents = []

    if len(parents) > 1:
        sys.stdout.write(old + ("\n" if not old.endswith("\n") else ""))
        return

    raw = git_lines(["git", "diff-tree", "--no-commit-id", "--name-status", "-r", commit])
    files: list[tuple[str, str]] = []
    for ln in raw.splitlines():
        if not ln.strip():
            continue
        tabs = ln.split("\t")
        tag = tabs[0].split()[0] if tabs else ""
        if tag.startswith("R") and len(tabs) >= 3:
            files.append((tag[0], tabs[-1].strip()))
        elif len(tabs) >= 2:
            files.append((tag, tabs[1].strip()))

    new_body = build_body(subject, files)
    if not new_body:
        # fallback: keep non-empty old body lines (except subject)
        tail = "\n".join(lines[1:]).strip()
        if tail:
            print(subject + "\n\n" + tail + "\n")
        else:
            print(subject + "\n")
        return

    out = subject + "\n\n" + new_body + "\n"
    sys.stdout.write(out)


if __name__ == "__main__":
    main()
