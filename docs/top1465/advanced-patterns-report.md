# Advanced-pattern catalog report

Full pinned-corpus run after adding the advanced inference, coloring, ALS,
assumption, fish, and Exocet strategies.

## Run

- Date: 2026-07-14
- Source SHA-256: `32837f38ece94e75678deadbe256aeafda704c8d4c2f8b5095a630ce2d0114d3`
- Processed: 1,465 puzzles
- Elapsed: 265,177 ms (limit: 10 minutes)

```shell
TOP1465_RUN=1 \
TOP1465_OUTPUT_DIR="$PWD/domain/build/reports/top1465-issue-1110" \
./gradlew :domain:testDebugUnitTest \
  --tests '*Top1465CorpusRunnerTest.runTop1465CorpusWhenEnabled' \
  --rerun-tasks --console=plain
```

## Delta from the committed baseline

| Metric | Baseline | Advanced catalog | Delta |
| --- | ---: | ---: | ---: |
| Logically solved | 369 | 936 | +567 |
| Logically stalled | 1,096 | 529 | -567 |
| Unique after logical pipeline | 1,460 | 1,465 | +5 |
| Unsolvable after logical pipeline | 5 | 0 | -5 |
| Incorrect logical placements | 5 | 0 | -5 |
| Incorrect candidate eliminations | 4 | 0 | -4 |
| Incorrect candidate losses | 5 | 0 | -5 |

All 529 still-stalled lines already have a GitHub issue with their givens and
rendered puzzle image; no new failure issue was required by this run.

## Catalog contract

- `catalogPatterns()` exposes conceptual entries, including structural
  `AlmostLockedSet`.
- `supportedPatterns()` exposes only patterns backed by an executable strategy.
- `automaticPatterns()` excludes the explicit `NiceLoop` facade and the
  exhaustive assumption searches `ForcingChain`, `BowmansBingo`, and `Nishio`.
  They remain available through `hintForPattern()` and have dedicated tests.
