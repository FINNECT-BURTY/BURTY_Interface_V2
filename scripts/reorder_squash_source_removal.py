#!/usr/bin/env python3
"""GIT_SEQUENCE_EDITOR: reorder picks so all 'CHORE: 소스 제거' are last, then squash them into one."""

import sys

SUBJECT_MARKER = "CHORE: 소스 제거"


def main() -> None:
    path = sys.argv[1]
    raw = open(path, encoding="utf-8").read().splitlines()
    comments: list[str] = []
    picks: list[str] = []
    for line in raw:
        if line.startswith("#") or not line.strip():
            comments.append(line)
            continue
        if line.startswith(("pick ", "reword ", "edit ", "squash ", "fixup ", "drop ", "break ", "label ", "reset ", "merge ")):
            picks.append(line)
        else:
            comments.append(line)

    def subject_of(pick_line: str) -> str:
        # "pick <hash> <subject...>"
        parts = pick_line.split(None, 2)
        return parts[2] if len(parts) > 2 else ""

    non_src = [ln for ln in picks if SUBJECT_MARKER not in subject_of(ln)]
    src = [ln for ln in picks if SUBJECT_MARKER in subject_of(ln)]
    if not src:
        return

    new_picks: list[str] = non_src[:]
    first = src[0].split(None, 2)
    # keep first as pick
    new_picks.append(src[0])
    for ln in src[1:]:
        rest = ln.split(None, 2)
        h = rest[1]
        subj = rest[2] if len(rest) > 2 else ""
        new_picks.append(f"squash {h} {subj}".rstrip())

    out = "\n".join(comments + new_picks) + "\n"
    open(path, "w", encoding="utf-8").write(out)


if __name__ == "__main__":
    main()
