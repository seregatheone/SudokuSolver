package pet.project.sudokusolver.feature.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import pet.project.sudokusolver.domain.CellPosition
import pet.project.sudokusolver.domain.SolutionMode
import pet.project.sudokusolver.domain.SudokuConflict
import pet.project.sudokusolver.domain.SudokuGrid
import pet.project.sudokusolver.domain.SudokuSolutionStep
import pet.project.sudokusolver.domain.SudokuSolver

data class BoardState(
    val grid: SudokuGrid,
    val selectedCell: CellPosition? = null,
    val highlightedValue: Int? = null,
    val selectedMode: SolutionMode? = null,
    val isPencilMode: Boolean = false,
    val steps: List<SudokuSolutionStep> = emptyList(),
    val stepHistory: List<SudokuGrid> = emptyList(),
    val nextStepIndex: Int = 0,
    val status: BoardStatus = BoardStatus.Initial,
) {
    val allowedValues: Set<Int> = selectedCell
        ?.let { cell -> grid.allowedValuesAt(cell.row, cell.column) }
        .orEmpty()
}

sealed interface BoardIntent {
    data object ClearClicked : BoardIntent
    data class CellSelected(val cell: CellPosition) : BoardIntent
    data object PencilModeToggled : BoardIntent
    data class ValueSelected(val value: Int?) : BoardIntent
    data class SolutionModeSelected(val mode: SolutionMode) : BoardIntent
    data object PreviousStepClicked : BoardIntent
    data object NextStepClicked : BoardIntent
    data object HintClicked : BoardIntent
}

sealed interface BoardStatus {
    data object Initial : BoardStatus
    data object GridCleared : BoardStatus
    data object DigitRemoved : BoardStatus
    data class DigitEntered(val value: Int) : BoardStatus
    data class NoteToggled(val value: Int) : BoardStatus
    data object NotesCleared : BoardStatus
    data class Conflict(val value: Int, val conflict: SudokuConflict) : BoardStatus
    data object SolveFailed : BoardStatus
    data object FastSolved : BoardStatus
    data object NoStepsAvailable : BoardStatus
    data object StepByStepReady : BoardStatus
    data object SelfPracticeReady : BoardStatus
    data object HintUnavailable : BoardStatus
    data class StepApplied(val step: SudokuSolutionStep) : BoardStatus
}

class BoardViewModel(
    initialGrid: SudokuGrid,
    private val solver: SudokuSolver = SudokuSolver(),
) : ViewModel() {
    var state by mutableStateOf(BoardState(grid = initialGrid))
        private set

    fun onIntent(intent: BoardIntent) {
        when (intent) {
            BoardIntent.ClearClicked -> {
                state = BoardState(
                    grid = SudokuGrid.Empty,
                    status = BoardStatus.GridCleared,
                )
            }

            is BoardIntent.CellSelected -> {
                state = state.copy(
                    selectedCell = if (state.selectedCell == intent.cell) null else intent.cell,
                    highlightedValue = null,
                )
            }

            BoardIntent.PencilModeToggled -> {
                state = state.copy(isPencilMode = !state.isPencilMode)
            }

            is BoardIntent.ValueSelected -> onValueSelected(intent.value)
            is BoardIntent.SolutionModeSelected -> onSolutionModeSelected(intent.mode)
            BoardIntent.PreviousStepClicked -> onPreviousStepClicked()
            BoardIntent.NextStepClicked -> onNextStepClicked()
            BoardIntent.HintClicked -> onHintClicked()
        }
    }

    private fun onValueSelected(value: Int?) {
        val cell = state.selectedCell
        if (cell == null) {
            if (value != null) {
                state = state.copy(
                    highlightedValue = if (state.highlightedValue == value) null else value,
                )
            }
            return
        }

        if (value == null) {
            state = state.copy(
                grid = state.grid.clearCell(cell.row, cell.column),
                highlightedValue = null,
                selectedMode = null,
                steps = emptyList(),
                stepHistory = emptyList(),
                nextStepIndex = 0,
                status = if (state.isPencilMode) BoardStatus.NotesCleared else BoardStatus.DigitRemoved,
            )
            return
        }

        val conflict = state.grid.conflictFor(cell.row, cell.column, value)
        if (conflict != null) {
            state = state.copy(status = BoardStatus.Conflict(value = value, conflict = conflict))
            return
        }

        if (state.isPencilMode) {
            state = state.copy(
                grid = state.grid.toggleNote(cell.row, cell.column, value),
                highlightedValue = null,
                status = BoardStatus.NoteToggled(value),
            )
            return
        }

        state = state.copy(
            grid = state.grid.setValue(cell.row, cell.column, value),
            highlightedValue = null,
            selectedMode = null,
            steps = emptyList(),
            stepHistory = emptyList(),
            nextStepIndex = 0,
            status = BoardStatus.DigitEntered(value),
        )
    }

    private fun onSolutionModeSelected(mode: SolutionMode) {
        state = state.copy(
            grid = state.grid.withCandidateNotes(),
            highlightedValue = null,
            selectedMode = mode,
            steps = emptyList(),
            stepHistory = emptyList(),
            nextStepIndex = 0,
        )

        when (mode) {
            SolutionMode.Fast -> {
                val result = solver.solve(state.grid)
                state = if (result == null) {
                    state.copy(status = BoardStatus.SolveFailed)
                } else {
                    state.copy(grid = result.solvedGrid, status = BoardStatus.FastSolved)
                }
            }

            SolutionMode.StepByStep -> {
                val result = solver.solve(state.grid)
                state = if (result == null || result.steps.isEmpty()) {
                    state.copy(status = BoardStatus.NoStepsAvailable)
                } else {
                    state.copy(steps = result.steps, status = BoardStatus.StepByStepReady)
                }
            }

            SolutionMode.SelfPractice -> {
                state = state.copy(status = BoardStatus.SelfPracticeReady)
            }
        }
    }

    private fun onPreviousStepClicked() {
        val previousGrid = state.stepHistory.lastOrNull() ?: return
        val previousStep = state.steps.getOrNull(state.nextStepIndex - 2)
        state = state.copy(
            grid = previousGrid,
            stepHistory = state.stepHistory.dropLast(1),
            nextStepIndex = (state.nextStepIndex - 1).coerceAtLeast(0),
            highlightedValue = null,
            status = previousStep?.let(BoardStatus::StepApplied) ?: BoardStatus.StepByStepReady,
        )
    }

    private fun onNextStepClicked() {
        val step = state.steps.getOrNull(state.nextStepIndex) ?: return
        state = state.copy(
            grid = state.grid.applyStep(step),
            stepHistory = state.stepHistory + state.grid,
            nextStepIndex = state.nextStepIndex + 1,
            highlightedValue = null,
            status = BoardStatus.StepApplied(step),
        )
    }

    private fun onHintClicked() {
        val hint = solver.hint(state.grid)
        state = if (hint == null) {
            state.copy(status = BoardStatus.HintUnavailable)
        } else {
            state.copy(status = BoardStatus.StepApplied(hint))
        }
    }

    private fun SudokuGrid.applyStep(step: SudokuSolutionStep): SudokuGrid {
        if (step.isPlacement) return setValue(step.row, step.column, step.value)

        var updated = this
        step.eliminations.forEach { elimination ->
            updated = updated.removeNotes(elimination.row, elimination.column, elimination.values)
        }
        return updated
    }
}
