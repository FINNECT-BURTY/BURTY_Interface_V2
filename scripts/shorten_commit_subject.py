#!/usr/bin/env python3
"""stdin: full commit message; stdout: first line capped at 50 chars, rest unchanged."""

import sys

text = sys.stdin.read()
lines = text.splitlines()
if not lines:
    sys.exit(0)
subj = lines[0]
if len(subj) > 50:
    if ": " in subj:
        prefix, rest = subj.split(": ", 1)
        room = 50 - len(prefix) - 2
        if room < 8:
            subj = (prefix + ": 항목 반영")[:50]
        else:
            subj = prefix + ": " + rest[:room].rstrip()
    else:
        subj = subj[:50].rstrip()
body = "\n".join(lines[1:])
sys.stdout.write(subj + ("\n" + body if body else "") + ("\n" if not text.endswith("\n") else ""))
