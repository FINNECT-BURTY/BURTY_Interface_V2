#!/usr/bin/env python3
"""Stage exactly one path per commit with Korean messages (50-char title, 72-char body)."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def wrap_line(s: str, width: int = 72) -> list[str]:
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


ROOT = Path(__file__).resolve().parents[1]


def normalize_path(path: str) -> str:
    path = path.strip()
    if len(path) >= 2 and path[0] == '"' and path[-1] == '"':
        path = path[1:-1]
    return path


def classify(path: str, xy: str) -> tuple[str, str, str]:
    """Return (type_prefix, 무엇, 왜) Korean phrases for body."""
    p = path.replace("\\", "/")
    is_del = len(xy) >= 2 and (xy[0] == "D" or xy[1] == "D")
    is_untracked = xy == "??"
    is_new = is_untracked or (len(xy) >= 2 and (xy[0] == "A" or xy[1] == "A"))

    if p == "build.gradle" or p == "settings.gradle":
        return (
            "CHORE",
            f"무엇: Gradle 설정 파일 `{Path(p).name}` 반영",
            "왜: Berty 모듈 기준 빌드·의존성 정렬 및 재현성 확보",
        )
    if p.startswith(".github/"):
        return (
            "CHORE",
            f"무엇: GitHub 협업 자산 `{p}` 반영",
            "왜: 이슈·PR·규칙 문서로 협업 품질 및 추적성 확보",
        )
    if p.startswith("docs/"):
        if is_del:
            return (
                "CHORE",
                f"무엇: 문서 자산 `{p}` 제거",
                "왜: 레거시 문서 정리 및 저장소 범위·출처 일원화",
            )
        return (
            "CHORE",
            f"무엇: 문서 자산 `{p}` 반영",
            "왜: 요구·설계 근거 공유 및 온보딩 효율 확보",
        )
    if p.startswith("nuri-ERD/") or p.startswith("berty-ERD/"):
        if is_del:
            return (
                "CHORE",
                f"무엇: ERD·SQL 자산 `{p}` 제거",
                "왜: 저장소 주제 정리 및 중복 스키마 혼선 방지",
            )
        return (
            "CHORE",
            f"무엇: ERD·SQL 자산 `{p}` 반영",
            "왜: 데이터 모델·쿼리 기준 공유 및 개발 연계성 확보",
        )
    if p.endswith(".pdf"):
        if is_del:
            return (
                "CHORE",
                f"무엇: PDF 산출물 `{Path(p).name}` 제거",
                "왜: 바이너리 용량·저작권 관리 및 저장소 범위 정리",
            )
        return (
            "CHORE",
            f"무엇: PDF 산출물 `{Path(p).name}` 반영",
            "왜: 기획·요구 근거 문서 보존 및 의사결정 기준 확보",
        )
    if p.startswith("src/test/"):
        return (
            "CHORE",
            f"무엇: 테스트 코드 `{p}` 반영",
            "왜: 회귀 검증·품질 안전망 및 스펙 고정 효과 확보",
        )
    if p.startswith("src/main/resources/"):
        return (
            "CHORE",
            f"무엇: 실행 리소스 `{p}` 반영",
            "왜: 환경별 설정 일관성 및 배포·로컬 동작 안정성 확보",
        )
    if p.startswith("src/main/java/com/berty/"):
        if is_del:
            return (
                "CHORE",
                f"무엇: Berty 소스 `{p}` 제거",
                "왜: 불필요·중복 코드 정리 및 유지보수 범위 축소",
            )
        return (
            "FEAT",
            f"무엇: Berty 애플리케이션 코드 `{p}` 반영",
            "왜: 도메인 기능·어댑터 계층 확장 및 API 계약 충족",
        )
    if p.startswith("src/main/java/com/nuri/"):
        if is_del:
            return (
                "CHORE",
                f"무엇: Nuri 레거시 소스 `{p}` 제거",
                "왜: 패키지 이관 후 중복·혼선 방지",
            )
        return (
            "FEAT",
            f"무엇: Nuri 소스 `{p}` 반영",
            "왜: 기존 모듈 호환·이관 구간 유지",
        )
    if is_del:
        return (
            "CHORE",
            f"무엇: 저장소 자산 `{p}` 제거",
            "왜: 불필요 파일 정리 및 협업 범위 명확화",
        )
    if is_new or "M" in xy:
        return (
            "CHORE",
            f"무엇: 프로젝트 자산 `{p}` 반영",
            "왜: 저장소 구성 일관성 및 추적 가능한 변경 단위 확보",
        )
    return (
        "CHORE",
        f"무엇: 자산 `{p}` 반영",
        "왜: 변경 사항 분리 기록 및 리뷰·롤백 용이성 확보",
    )


def verb_for_path(path: str, xy: str) -> str:
    p = path.replace("\\", "/")
    is_del = len(xy) >= 2 and (xy[0] == "D" or xy[1] == "D")
    if is_del:
        if p.startswith("nuri-ERD/") or p.startswith("berty-ERD/"):
            return "ERD SQL 제거"
        if p.startswith("docs/"):
            return "문서 제거"
        if p.endswith(".java"):
            return "소스 제거"
        return "자산 제거"
    if p in ("build.gradle", "settings.gradle"):
        return "Gradle 설정 반영"
    if p.startswith(".github/"):
        return "GitHub 설정 반영"
    if p.startswith("berty-ERD/") or p.startswith("nuri-ERD/"):
        return "ERD SQL 반영"
    if p.startswith("src/main/java/com/berty/"):
        nm = Path(p).name
        if nm.endswith("Controller.java"):
            return f"{nm[:-14]} API 반영"
        if nm.endswith("Service.java"):
            return f"{nm[:-11]} 서비스 반영"
        if nm.endswith("Entity.java"):
            return f"{nm[:-10]} 엔티티 반영"
        if nm.endswith("Repository.java"):
            return f"{nm[:-14]} 저장소 반영"
        if nm.endswith("UseCase.java"):
            return f"{nm[:-8]} 유스케이스 반영"
        if nm.endswith("Port.java"):
            return f"{nm[:-8]} 포트 반영"
        if nm.endswith("Adapter.java"):
            return f"{nm[:-10]} 어댑터 반영"
        if nm.endswith("Request.java") or nm.endswith("Response.java"):
            return f"{Path(nm).stem} DTO 반영"
        return f"{nm} 반영"
    if p.startswith("src/test/"):
        return f"{Path(p).name} 테스트 반영"
    if p.startswith("src/main/resources/"):
        return f"{Path(p).name} 리소스 반영"
    return f"{Path(p).name} 반영"


def build_title(prefix: str, path: str, xy: str) -> str:
    """Subject: PREFIX: imperative Korean, <= 50 chars, no trailing period."""
    verb = verb_for_path(path, xy)
    head = f"{prefix}: {verb}"
    if len(head) <= 50:
        return head
    # Shorten verb part
    max_verb = 50 - len(prefix) - 2  # ": "
    if max_verb < 8:
        max_verb = 8
    verb = verb[:max_verb].rstrip()
    head = f"{prefix}: {verb}"
    if len(head) > 50:
        head = head[:50].rstrip()
    return head


def parse_status() -> list[tuple[str, str]]:
    """Return list of (xy, path) for every path that needs a commit."""
    out = subprocess.check_output(
        ["git", "-c", "core.quotePath=false", "status", "--porcelain=v1"],
        cwd=ROOT,
        text=True,
        errors="replace",
    )
    entries: list[tuple[str, str]] = []
    for line in out.splitlines():
        if len(line) < 4:
            continue
        xy = line[:2]
        rest = line[3:]
        if " -> " in rest:
            path = rest.split(" -> ", 1)[1].strip()
        else:
            path = rest.strip()
        if not path:
            continue
        entries.append((xy, normalize_path(path)))
    # Deterministic order: deletions of legacy dirs first, then gradle, then github, then rest alpha
    def sort_key(item: tuple[str, str]) -> tuple[int, str]:
        xy, path = item
        pri = 5
        if path.startswith("nuri-ERD/") and "D" in xy:
            pri = 0
        elif path.startswith("docs/") and "D" in xy:
            pri = 1
        elif path.endswith(".pdf") and "D" in xy:
            pri = 2
        elif path in ("build.gradle", "settings.gradle"):
            pri = 3
        elif path.startswith(".github/"):
            pri = 4
        return (pri, path)

    entries.sort(key=sort_key)
    return expand_untracked_dirs(entries)


def expand_untracked_dirs(entries: list[tuple[str, str]]) -> list[tuple[str, str]]:
    """Turn `?? dir/` into one entry per file under dir (per-file commits)."""
    out: list[tuple[str, str]] = []
    for xy, path in entries:
        rel = path.rstrip("/")
        full = ROOT / rel
        if xy == "??" and full.is_dir():
            files = sorted(
                p.relative_to(ROOT).as_posix()
                for p in full.rglob("*")
                if p.is_file()
            )
            if not files:
                continue
            for f in files:
                out.append((xy, f))
        else:
            out.append((xy, rel))
    return out


def main() -> int:
    subprocess.check_call(["git", "reset", "HEAD"], cwd=ROOT)
    skip_paths = {".env"}
    pending = [p for p in parse_status() if p[1] not in skip_paths]
    if not pending:
        print("Nothing to commit.")
        return 0

    for xy, path in pending:
        prefix, body1, body2 = classify(path, xy)
        title = build_title(prefix, path, xy)
        # Ensure first character of title line is uppercase (English prefix already is)
        if title and title[0].isalpha() and title[0].islower():
            title = title[0].upper() + title[1:]

        body_lines = wrap_line(body1) + wrap_line(body2)
        for ln in body_lines:
            if len(ln) > 72:
                print(f"WARN line >72: {ln}", file=sys.stderr)

        msg = title + "\n\n" + "\n".join(body_lines) + "\n"
        subprocess.check_call(["git", "add", "--", path], cwd=ROOT)
        subprocess.check_call(["git", "commit", "-m", msg], cwd=ROOT)
        print(f"OK {xy} {path}")

    print(f"Done {len(pending)} commits.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
