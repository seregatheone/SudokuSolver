# SudokuSolver Architecture

## Current Stack

- Kotlin Multiplatform for shared Android/iOS domain and UI code.
- Compose Multiplatform + Material 3 for screens.
- AndroidX Activity Result Photo Picker for gallery input on Android.
- Lifecycle ViewModel/Runtime dependencies are already in the project and are the next natural place to move screen state when flows become larger.

## Suggested Feature Modules

```text
domain/src/commonMain/kotlin/pet/project/sudokusolver/domain
  SudokuGrid, validation rules, solver, solution steps

data/src/commonMain/kotlin/pet/project/sudokusolver/data
  recognition contracts and CV result types

composeApp/src/commonMain/kotlin/pet/project/sudokusolver
  feature/input          input method selection screen
  feature/board          9x9 grid screen and solution modes

composeApp/src/androidMain/kotlin/pet/project/sudokusolver
  recognition            Android Photo Picker and Android CV adapter

composeApp/src/iosMain/kotlin/pet/project/sudokusolver
  recognition            iOS PHPicker/Vision adapter, to be added
```

## CV Model Options

Preferred production path:

1. Detect the sudoku board with OpenCV-style preprocessing: grayscale, threshold, contour detection, perspective transform.
2. Split the normalized board into 81 cells.
3. Classify each non-empty cell with an on-device digit model.
4. Show the recognized grid for user correction before solving.

Recommended model/runtime choices:

- Android easy start: [Google ML Kit Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android). It is quick to integrate and can extract text boxes from images, but single sudoku digits may still need cell-aware post-processing.
- Android production/offline: [LiteRT](https://ai.google.dev/edge/litert/android) with a small custom digit classifier trained on MNIST plus synthetic sudoku fonts/screenshots. This gives low latency, privacy, and stable behavior offline.
- iOS easy start: [Apple Vision text recognition](https://developer.apple.com/documentation/vision/recognizing-text-in-images) for OCR, then map recognized text boxes into the 9x9 grid.
- Cross-platform high accuracy: deterministic grid detection plus a custom digit classifier exported to TFLite/Core ML. Keep the domain result as `SudokuGrid` so platform models remain replaceable.

## Solver Implementation

The current solver is local and deterministic:

- validates duplicate conflicts in rows, columns, and 3x3 blocks;
- solves with backtracking;
- chooses the empty cell with the fewest candidates first;
- returns either a completed grid for fast mode or a list of fill steps for step-by-step and hint modes.

For richer human-like explanations, add a second solver layer that emits named techniques before falling back to backtracking:

- naked singles;
- hidden singles;
- locked candidates;
- naked pairs/triples;
- X-Wing and similar advanced techniques.

The UI already separates three flows:

- fast solution: writes all digits at once;
- step-by-step solution: applies one generated step at a time;
- self practice: user edits the board and can request a hint.
