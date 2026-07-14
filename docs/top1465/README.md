# top1465 corpus

This directory contains the reproducible logical-solver baseline for the
[`top1465`](http://magictour.free.fr/top1465) Sudoku corpus. Each non-empty
source line is one 81-cell puzzle where `.` denotes an empty cell.

## Pinned input

- Source: `http://magictour.free.fr/top1465`
- Puzzle count: `1465`
- SHA-256: `32837f38ece94e75678deadbe256aeafda704c8d4c2f8b5095a630ce2d0114d3`
- Maximum runner time: 10 minutes

The test rejects a changed checksum, malformed line, wrong puzzle count, or a
run that exceeds the deadline. Normal unit-test runs do not access the network.

## Run the corpus

```shell
TOP1465_RUN=1 \
TOP1465_OUTPUT_DIR="$PWD/domain/build/reports/top1465" \
./gradlew :domain:testDebugUnitTest \
  --tests '*Top1465CorpusRunnerTest.runTop1465CorpusWhenEnabled' \
  --rerun-tasks --console=plain
```

For an offline run, point `TOP1465_SOURCE` at a local copy with the pinned
checksum:

```shell
TOP1465_RUN=1 \
TOP1465_SOURCE=/absolute/path/to/top1465 \
TOP1465_OUTPUT_DIR="$PWD/domain/build/reports/top1465" \
./gradlew :domain:testDebugUnitTest \
  --tests '*Top1465CorpusRunnerTest.runTop1465CorpusWhenEnabled'
```

The output contains:

- `summary.txt` — aggregate classifications and timing;
- `report.json` — complete machine-readable report;
- `puzzles.tsv` — compact spreadsheet-friendly inventory;
- `issue-drafts.jsonl` — one JSON object per logical failure;
- `failures/*.png` — givens rendered for each logical failure.

`baseline/` is the committed result used by GitHub issues. A puzzle is
`logicallySolved` only when registered logical strategies fill every cell and
the resulting board has one solution. Backtracking is used only to classify the
original and post-logic boards; it is never counted as a logical step.

The diagnostic fields `firstIncorrectStep`, `firstIncorrectElimination`, and
`firstIncorrectCandidateLoss` identify contradictions against the unique
backtracking solution. They distinguish a missing strategy from an unsound
existing strategy.

The latest advanced-pattern catalog comparison is recorded in
[`advanced-patterns-report.md`](advanced-patterns-report.md).

## File issues in resumable batches

Preview the next batch:

```shell
./tools/top1465/file_issues.py --limit 25
```

Create it after the baseline is available on the configured Git branch:

```shell
./tools/top1465/file_issues.py --apply --limit 25
```

The script loads all existing issue titles before starting, skips exact title
matches, and adds newly created titles to the in-memory index. Re-running the
same command is therefore idempotent. Use `--start-line N` to resume from a
specific corpus line and `--limit 0` only when intentionally processing every
remaining puzzle in one invocation.
