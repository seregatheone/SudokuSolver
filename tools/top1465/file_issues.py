#!/usr/bin/env python3
"""Create one resumable GitHub issue per unsolved top1465 puzzle."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
from typing import Any


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_REPORT = PROJECT_ROOT / "docs/top1465/baseline/report.json"
DEFAULT_HELPER = Path.home() / ".codex/skills/issue-creator/scripts/create_issue_checked.sh"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument("--repo", default="seregatheone/SudokuSolver")
    parser.add_argument("--branch", default="dev")
    parser.add_argument("--epic", type=int, default=14)
    parser.add_argument("--discovered-by", type=int, default=16)
    parser.add_argument("--start-line", type=int, default=1)
    parser.add_argument(
        "--limit",
        type=int,
        default=25,
        help="Maximum issues to create or preview; 0 means all remaining puzzles.",
    )
    parser.add_argument("--pause-seconds", type=float, default=2.0)
    parser.add_argument("--apply", action="store_true", help="Create issues instead of previewing them.")
    parser.add_argument("--helper", type=Path, default=DEFAULT_HELPER)
    return parser.parse_args()


def run(command: list[str]) -> str:
    completed = subprocess.run(command, check=True, text=True, capture_output=True)
    return completed.stdout.strip()


def existing_issues(repo: str) -> dict[str, dict[str, Any]]:
    pages = json.loads(
        run(
            [
                "gh",
                "api",
                "--paginate",
                "--slurp",
                f"repos/{repo}/issues?state=all&per_page=100",
            ],
        ),
    )
    issues: dict[str, dict[str, Any]] = {}
    for page in pages:
        for issue in page:
            if "pull_request" not in issue:
                issues[issue["title"]] = issue
    return issues


def issue_title(puzzle: dict[str, Any]) -> str:
    line = puzzle["lineNumber"]
    if puzzle.get("firstIncorrectStep") or puzzle.get("firstIncorrectCandidateLoss"):
        outcome = "логический решатель повреждает решение"
    else:
        outcome = "логический решатель останавливается"
    return f"top1465 #{line:04d}: {outcome}"


def diagnostic_text(puzzle: dict[str, Any]) -> str:
    parts = []
    for key in (
        "firstIncorrectCandidateLoss",
        "firstIncorrectElimination",
        "firstIncorrectStep",
    ):
        value = puzzle.get(key)
        if value:
            parts.append(f"- `{key}`: `{json.dumps(value, ensure_ascii=False, sort_keys=True)}`")
    return "\n".join(parts) if parts else "- Противоречий с уникальным решением до остановки не обнаружено."


def issue_body(
    puzzle: dict[str, Any],
    repo: str,
    branch: str,
    epic: int,
    discovered_by: int,
) -> str:
    line = puzzle["lineNumber"]
    image = puzzle["image"]
    raw_image = (
        f"https://raw.githubusercontent.com/{repo}/{branch}/"
        f"docs/top1465/baseline/{image}"
    )
    patterns = ", ".join(f"`{pattern}`" for pattern in puzzle["patterns"]) or "—"
    return f"""## Summary

Логический pipeline не решает строку {line} из закреплённого корпуса top1465.

## Evidence

- Corpus line: `{line}`
- Baseline classification: `{puzzle['baselineClassification']}`
- Current classification: `{puzzle['classification']}`
- Remaining cells: `{puzzle['remainingCells']}`
- Logical steps: `{puzzle['stepCount']}`
- Used patterns: {patterns}

{diagnostic_text(puzzle)}

![top1465 #{line:04d}]({raw_image})

## Steps to reproduce

1. Запустить corpus runner командой из `docs/top1465/README.md`.
2. Открыть строку `{line}` в `report.json`.
3. Применять только зарегистрированные логические стратегии до остановки.

## Expected

Решатель завершает уникальную головоломку без backtracking и не удаляет значение из её уникального решения.

## Actual

После логического pipeline остаётся `{puzzle['remainingCells']}` пустых клеток; классификация — `{puzzle['classification']}`.

## Input

Givens:

```text
{puzzle['givens']}
```

Grid after logical steps:

```text
{puzzle['stalledGrid']}
```

## Acceptance criteria

- [ ] Головоломка решается зарегистрированными логическими стратегиями без backtracking.
- [ ] Все шаги сохраняют уникальное решение исходной головоломки.
- [ ] Добавлен регрессионный тест для строки `{line}`.
- [ ] Повторный полный прогон top1465 не ухудшает остальные результаты.

## Relations

- Part of #{epic}
- Discovered by #{discovered_by}
"""


def main() -> int:
    args = parse_args()
    if args.limit < 0:
        raise SystemExit("--limit must be zero or positive")
    report = json.loads(args.report.read_text(encoding="utf-8"))
    puzzles = [
        puzzle
        for puzzle in report["puzzles"]
        if not puzzle["logicallySolved"] and puzzle["lineNumber"] >= args.start_line
    ]
    known = existing_issues(args.repo)
    pending = [puzzle for puzzle in puzzles if issue_title(puzzle) not in known]
    selected = pending if args.limit == 0 else pending[: args.limit]

    print(
        json.dumps(
            {
                "mode": "apply" if args.apply else "preview",
                "unsolvedFromStart": len(puzzles),
                "alreadyFiled": len(puzzles) - len(pending),
                "selected": len(selected),
                "remainingAfterBatch": len(pending) - len(selected),
            },
            ensure_ascii=False,
        ),
    )

    for index, puzzle in enumerate(selected, start=1):
        title = issue_title(puzzle)
        if not args.apply:
            print(f"PREVIEW {puzzle['lineNumber']:04d} {title}")
            continue
        if not args.helper.is_file():
            raise SystemExit(f"issue helper not found: {args.helper}")
        body = issue_body(puzzle, args.repo, args.branch, args.epic, args.discovered_by)
        with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".md", delete=False) as body_file:
            body_file.write(body)
            body_path = Path(body_file.name)
        try:
            result = json.loads(
                run(
                    [
                        os.fspath(args.helper),
                        "-R",
                        args.repo,
                        "--title",
                        title,
                        "--body-file",
                        os.fspath(body_path),
                        "--search",
                        f'"{title}" in:title',
                        "--label",
                        "bug",
                        "--force",
                    ],
                ),
            )
        finally:
            body_path.unlink(missing_ok=True)
        known[title] = {"title": title, "url": result["url"]}
        print(f"CREATED {index}/{len(selected)} {result['url']}")
        if index != len(selected) and args.pause_seconds > 0:
            time.sleep(args.pause_seconds)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
