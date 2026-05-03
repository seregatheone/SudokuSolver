package pet.project.sudokusolver.feature.board

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.domain.CellPosition
import pet.project.sudokusolver.domain.SudokuConflict
import pet.project.sudokusolver.domain.SudokuSolutionStep
import pet.project.sudokusolver.domain.SudokuSolvingPattern
import sudokusolver.composeapp.generated.resources.Res
import sudokusolver.composeapp.generated.resources.*

@Composable
internal fun statusText(status: BoardStatus): String = when (status) {
    BoardStatus.Initial -> stringResource(Res.string.board_status_initial)
    BoardStatus.GridCleared -> stringResource(Res.string.board_status_cleared)
    BoardStatus.DigitRemoved -> stringResource(Res.string.board_status_digit_removed)
    BoardStatus.NotesCleared -> stringResource(Res.string.board_status_notes_cleared)
    is BoardStatus.NoteToggled -> stringResource(Res.string.board_status_note_toggled, status.value)
    is BoardStatus.DigitEntered -> stringResource(Res.string.board_status_digit_entered, status.value)
    BoardStatus.SolveFailed -> stringResource(Res.string.board_status_solve_failed)
    BoardStatus.FastSolved -> stringResource(Res.string.board_status_fast_solved)
    BoardStatus.NoStepsAvailable -> stringResource(Res.string.board_status_no_steps)
    BoardStatus.StepByStepReady -> stringResource(Res.string.board_status_step_by_step_ready)
    BoardStatus.SelfPracticeReady -> stringResource(Res.string.board_status_self_practice_ready)
    BoardStatus.HintUnavailable -> stringResource(Res.string.board_status_hint_unavailable)
    is BoardStatus.StepApplied -> stepText(status.step)
    is BoardStatus.Conflict -> conflictText(status.conflict, status.value)
}

@Composable
private fun stepText(step: SudokuSolutionStep): String {
    val pattern = patternText(step.pattern)
    val related = relatedCellsText(step.relatedCells)
    return if (step.isPlacement) {
        stringResource(
            Res.string.board_status_step_place,
            coordinateText(step.row, step.column),
            step.value,
            pattern,
            related,
        )
    } else {
        stringResource(
            Res.string.board_status_step_eliminate,
            eliminationsText(step),
            pattern,
            related,
        )
    }
}

@Composable
private fun relatedCellsText(cells: List<CellPosition>): String {
    if (cells.isEmpty()) return stringResource(Res.string.board_status_step_no_related_cells)
    return cells
        .take(6)
        .joinToString { cell -> coordinateText(cell.row, cell.column) }
}

private fun eliminationsText(step: SudokuSolutionStep): String = step.eliminations
    .take(6)
    .joinToString { elimination ->
        val values = elimination.values.sorted().joinToString("/")
        "${coordinateText(elimination.row, elimination.column)}=$values"
    }

internal fun coordinateText(row: Int, column: Int): String = "${columnLabel(column)}${row + 1}"

internal fun columnLabel(column: Int): Char = ('A'.code + column).toChar()

@Composable
private fun patternText(pattern: SudokuSolvingPattern): String = when (pattern) {
    SudokuSolvingPattern.NakedSingle -> stringResource(Res.string.pattern_naked_single)
    SudokuSolvingPattern.HiddenSingle -> stringResource(Res.string.pattern_hidden_single)
    SudokuSolvingPattern.LockedCandidatesPointing -> stringResource(Res.string.pattern_locked_candidates_pointing)
    SudokuSolvingPattern.ClaimingBoxLineReduction -> stringResource(Res.string.pattern_claiming_box_line_reduction)
    SudokuSolvingPattern.NakedPair -> stringResource(Res.string.pattern_naked_pair)
    SudokuSolvingPattern.NakedTriple -> stringResource(Res.string.pattern_naked_triple)
    SudokuSolvingPattern.NakedQuad -> stringResource(Res.string.pattern_naked_quad)
    SudokuSolvingPattern.HiddenPair -> stringResource(Res.string.pattern_hidden_pair)
    SudokuSolvingPattern.HiddenTriple -> stringResource(Res.string.pattern_hidden_triple)
    SudokuSolvingPattern.HiddenQuad -> stringResource(Res.string.pattern_hidden_quad)
    SudokuSolvingPattern.XWing -> stringResource(Res.string.pattern_x_wing)
    SudokuSolvingPattern.Swordfish -> stringResource(Res.string.pattern_swordfish)
    SudokuSolvingPattern.Jellyfish -> stringResource(Res.string.pattern_jellyfish)
    SudokuSolvingPattern.XYWing -> stringResource(Res.string.pattern_xy_wing)
    SudokuSolvingPattern.XYZWing -> stringResource(Res.string.pattern_xyz_wing)
    SudokuSolvingPattern.WWing -> stringResource(Res.string.pattern_w_wing)
    SudokuSolvingPattern.Skyscraper -> stringResource(Res.string.pattern_skyscraper)
    SudokuSolvingPattern.TwoStringKite -> stringResource(Res.string.pattern_two_string_kite)
    SudokuSolvingPattern.EmptyRectangle -> stringResource(Res.string.pattern_empty_rectangle)
    SudokuSolvingPattern.UniqueRectangle -> stringResource(Res.string.pattern_unique_rectangle)
    SudokuSolvingPattern.SimpleColoring -> stringResource(Res.string.pattern_simple_coloring)
    SudokuSolvingPattern.MultiColoring -> stringResource(Res.string.pattern_multi_coloring)
    SudokuSolvingPattern.XChain -> stringResource(Res.string.pattern_x_chain)
    SudokuSolvingPattern.XYChain -> stringResource(Res.string.pattern_xy_chain)
    SudokuSolvingPattern.AlternatingInferenceChain -> stringResource(Res.string.pattern_aic)
    SudokuSolvingPattern.ForcingChain -> stringResource(Res.string.pattern_forcing_chain)
    SudokuSolvingPattern.NiceLoop -> stringResource(Res.string.pattern_nice_loop)
    SudokuSolvingPattern.ContinuousLoop -> stringResource(Res.string.pattern_continuous_loop)
    SudokuSolvingPattern.DiscontinuousLoop -> stringResource(Res.string.pattern_discontinuous_loop)
    SudokuSolvingPattern.GroupedAic -> stringResource(Res.string.pattern_grouped_aic)
    SudokuSolvingPattern.AlmostLockedSet -> stringResource(Res.string.pattern_als)
    SudokuSolvingPattern.AlsXz -> stringResource(Res.string.pattern_als_xz)
    SudokuSolvingPattern.AlsXyWing -> stringResource(Res.string.pattern_als_xy_wing)
    SudokuSolvingPattern.DeathBlossom -> stringResource(Res.string.pattern_death_blossom)
    SudokuSolvingPattern.FinnedXWing -> stringResource(Res.string.pattern_finned_x_wing)
    SudokuSolvingPattern.SashimiXWing -> stringResource(Res.string.pattern_sashimi_x_wing)
    SudokuSolvingPattern.FinnedSwordfish -> stringResource(Res.string.pattern_finned_swordfish)
    SudokuSolvingPattern.KrakenFish -> stringResource(Res.string.pattern_kraken_fish)
    SudokuSolvingPattern.SueDeCoq -> stringResource(Res.string.pattern_sue_de_coq)
    SudokuSolvingPattern.Exocet -> stringResource(Res.string.pattern_exocet)
    SudokuSolvingPattern.ThreeDMedusa -> stringResource(Res.string.pattern_3d_medusa)
    SudokuSolvingPattern.BowmansBingo -> stringResource(Res.string.pattern_bowmans_bingo)
    SudokuSolvingPattern.Nishio -> stringResource(Res.string.pattern_nishio)
}

@Composable
private fun conflictText(conflict: SudokuConflict, value: Int): String = when (conflict) {
    SudokuConflict.Row -> stringResource(Res.string.board_conflict_row, value)
    SudokuConflict.Column -> stringResource(Res.string.board_conflict_column, value)
    SudokuConflict.Box -> stringResource(Res.string.board_conflict_box, value)
}
