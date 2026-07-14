package pet.project.sudokusolver.domain

class SudokuCell(
    value: Int? = null,
    notes: Set<Int> = emptySet(),
    val isGiven: Boolean = false,
    val confidence: Float? = null,
    val source: SudokuCellSource = SudokuCellSource.Unknown,
) {
    val value: Int? = value
    val notes: Set<Int> = notes.toSet()

    init {
        require(value == null || value in 1..9) { "Sudoku cell value must be 1..9." }
        require(notes.all { it in 1..9 }) { "Sudoku notes must be 1..9." }
        require(confidence == null || confidence.isFinite() && confidence in 0f..1f) {
            "Sudoku cell confidence must be finite and between 0 and 1."
        }
    }

    fun copy(
        value: Int? = this.value,
        notes: Set<Int> = this.notes,
        isGiven: Boolean = this.isGiven,
        confidence: Float? = this.confidence,
        source: SudokuCellSource = this.source,
    ): SudokuCell = SudokuCell(
        value = value,
        notes = notes,
        isGiven = isGiven,
        confidence = confidence,
        source = source,
    )

    override fun equals(other: Any?): Boolean = other is SudokuCell &&
        value == other.value &&
        notes == other.notes &&
        isGiven == other.isGiven &&
        confidence == other.confidence &&
        source == other.source

    override fun hashCode(): Int {
        var result = value ?: 0
        result = 31 * result + notes.hashCode()
        result = 31 * result + isGiven.hashCode()
        result = 31 * result + (confidence?.hashCode() ?: 0)
        result = 31 * result + source.hashCode()
        return result
    }

    override fun toString(): String =
        "SudokuCell(value=$value, notes=$notes, isGiven=$isGiven, confidence=$confidence, source=$source)"
}

enum class SudokuCellSource {
    Unknown,
    Manual,
    Ocr,
}

data class CellPosition(
    val row: Int,
    val column: Int,
) {
    init {
        require(row in 0 until SudokuGrid.Size) { "Sudoku row must be 0..8." }
        require(column in 0 until SudokuGrid.Size) { "Sudoku column must be 0..8." }
    }

    val index: Int = row * SudokuGrid.Size + column
}

sealed interface SudokuEditAction {
    val position: CellPosition

    data class SetValue(
        override val position: CellPosition,
        val value: Int,
        val isGiven: Boolean = false,
    ) : SudokuEditAction {
        init {
            require(value in 1..9) { "Sudoku cell value must be 1..9." }
        }
    }

    data class ClearCell(
        override val position: CellPosition,
    ) : SudokuEditAction

    data class ToggleNote(
        override val position: CellPosition,
        val value: Int,
    ) : SudokuEditAction {
        init {
            require(value in 1..9) { "Sudoku note value must be 1..9." }
        }
    }

    data class RemoveNotes(
        override val position: CellPosition,
        val values: Set<Int>,
    ) : SudokuEditAction {
        init {
            require(values.all { it in 1..9 }) { "Sudoku notes must be 1..9." }
        }
    }
}

class SudokuGrid(cells: List<SudokuCell>) {
    val cells: List<SudokuCell> = cells.toList()

    init {
        require(cells.size == CellCount) { "Sudoku grid must contain exactly $CellCount cells." }
    }

    fun valueAt(row: Int, column: Int): Int? = cells[row * Size + column].value

    fun cellAt(row: Int, column: Int): SudokuCell = cells[row * Size + column]

    fun edit(action: SudokuEditAction): SudokuGrid = when (action) {
        is SudokuEditAction.SetValue -> setValue(
            row = action.position.row,
            column = action.position.column,
            value = action.value,
            isGiven = action.isGiven,
        )

        is SudokuEditAction.ClearCell -> clearCell(
            row = action.position.row,
            column = action.position.column,
        )

        is SudokuEditAction.ToggleNote -> toggleNote(
            row = action.position.row,
            column = action.position.column,
            value = action.value,
        )

        is SudokuEditAction.RemoveNotes -> removeNotes(
            row = action.position.row,
            column = action.position.column,
            values = action.values,
        )
    }

    fun setValue(row: Int, column: Int, value: Int?, isGiven: Boolean = false): SudokuGrid {
        require(value == null || value in 1..9) { "Sudoku cell value must be 1..9." }
        val index = row * Size + column
        val affectedPeerIndexes: Set<Int> = if (value == null) emptySet() else peerIndexes(row, column)
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                when {
                    currentIndex == index -> {
                        cell.copy(
                            value = value,
                            notes = if (value == null) cell.notes else emptySet(),
                            isGiven = isGiven,
                            source = if (cell.source == SudokuCellSource.Unknown && value != null) {
                                SudokuCellSource.Manual
                            } else {
                                cell.source
                            },
                        )
                    }
                    currentIndex in affectedPeerIndexes -> {
                        cell.copy(notes = cell.notes - requireNotNull(value))
                    }
                    else -> cell
                }
            },
        )
    }

    fun toggleNote(row: Int, column: Int, value: Int): SudokuGrid {
        require(value in 1..9) { "Sudoku note value must be 1..9." }
        val index = row * Size + column
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                if (currentIndex == index && cell.value == null) {
                    val notes = if (value in cell.notes) cell.notes - value else cell.notes + value
                    cell.copy(notes = notes)
                } else {
                    cell
                }
            },
        )
    }

    fun clearCell(row: Int, column: Int): SudokuGrid {
        val index = row * Size + column
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                if (currentIndex == index) {
                    cell.copy(value = null, notes = emptySet(), isGiven = false)
                } else {
                    cell
                }
            },
        )
    }

    fun removeNotes(row: Int, column: Int, values: Set<Int>): SudokuGrid {
        require(values.all { it in 1..9 }) { "Sudoku notes must be 1..9." }
        val index = row * Size + column
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                if (currentIndex == index && cell.value == null) {
                    cell.copy(notes = cell.notes - values)
                } else {
                    cell
                }
            },
        )
    }

    fun withCandidateNotes(): SudokuGrid = copy(
        cells = cells.mapIndexed { index, cell ->
            if (cell.value == null) {
                val row = index / Size
                val column = index % Size
                cell.copy(notes = allowedValuesAt(row, column))
            } else {
                cell.copy(notes = emptySet())
            }
        },
    )

    fun canSetValue(row: Int, column: Int, value: Int): Boolean {
        require(value in 1..9) { "Sudoku cell value must be 1..9." }
        return conflictFor(row, column, value) == null
    }

    fun allowedValuesAt(row: Int, column: Int): Set<Int> = (1..9)
        .filter { value -> canSetValue(row, column, value) }
        .toSet()

    fun conflictFor(row: Int, column: Int, value: Int): SudokuConflict? {
        require(value in 1..9) { "Sudoku cell value must be 1..9." }
        val sourceIndex = row * Size + column

        for (currentColumn in 0 until Size) {
            val index = row * Size + currentColumn
            if (index != sourceIndex && cells[index].value == value) return SudokuConflict.Row
        }

        for (currentRow in 0 until Size) {
            val index = currentRow * Size + column
            if (index != sourceIndex && cells[index].value == value) return SudokuConflict.Column
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (currentRow in boxRow until boxRow + 3) {
            for (currentColumn in boxColumn until boxColumn + 3) {
                val index = currentRow * Size + currentColumn
                if (index != sourceIndex && cells[index].value == value) return SudokuConflict.Box
            }
        }

        return null
    }

    private fun peerIndexes(row: Int, column: Int): Set<Int> {
        val indexes = mutableSetOf<Int>()

        for (i in 0 until Size) {
            indexes += row * Size + i
            indexes += i * Size + column
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (currentRow in boxRow until boxRow + 3) {
            for (currentColumn in boxColumn until boxColumn + 3) {
                indexes += currentRow * Size + currentColumn
            }
        }

        indexes -= row * Size + column
        return indexes
    }

    fun hasAnyValue(): Boolean = cells.any { it.value != null }

    fun values(): List<Int?> = cells.map { it.value }

    fun copy(cells: List<SudokuCell> = this.cells): SudokuGrid = SudokuGrid(cells)

    override fun equals(other: Any?): Boolean = other is SudokuGrid && cells == other.cells

    override fun hashCode(): Int = cells.hashCode()

    override fun toString(): String = "SudokuGrid(cells=$cells)"

    companion object {
        const val Size = 9
        const val CellCount = Size * Size

        val Empty = SudokuGrid(List(CellCount) { SudokuCell() })

        fun fromRows(
            rows: List<List<Int?>>,
            markAsGiven: Boolean = true,
            source: SudokuCellSource = SudokuCellSource.Manual,
        ): SudokuGrid {
            require(rows.size == Size) { "Sudoku must contain 9 rows." }
            require(rows.all { it.size == Size }) { "Each sudoku row must contain 9 values." }

            return SudokuGrid(
                rows.flatten().map { value ->
                    SudokuCell(
                        value = value,
                        isGiven = markAsGiven && value != null,
                        source = if (value == null) SudokuCellSource.Unknown else source,
                    )
                },
            )
        }
    }
}

enum class SudokuConflict {
    Row,
    Column,
    Box,
}

enum class SolutionMode {
    Fast,
    StepByStep,
    SelfPractice,
}

enum class SudokuSolvingPattern {
    NakedSingle,
    HiddenSingle,
    LockedCandidatesPointing,
    ClaimingBoxLineReduction,
    NakedPair,
    NakedTriple,
    NakedQuad,
    HiddenPair,
    HiddenTriple,
    HiddenQuad,
    XWing,
    Swordfish,
    Jellyfish,
    XYWing,
    XYZWing,
    WWing,
    Skyscraper,
    TwoStringKite,
    EmptyRectangle,
    UniqueRectangle,
    SimpleColoring,
    MultiColoring,
    RemotePair,
    XChain,
    XYChain,
    AlternatingInferenceChain,
    ForcingChain,
    NiceLoop,
    ContinuousLoop,
    DiscontinuousLoop,
    GroupedAic,
    AlmostLockedSet,
    AlsXz,
    AlsXyWing,
    DeathBlossom,
    FinnedXWing,
    SashimiXWing,
    FinnedSwordfish,
    KrakenFish,
    SueDeCoq,
    Exocet,
    ThreeDMedusa,
    BowmansBingo,
    Nishio,
}

data class CandidateElimination(
    val row: Int,
    val column: Int,
    val values: Set<Int>,
)

data class SudokuSolutionStep(
    val row: Int,
    val column: Int,
    val value: Int,
    val pattern: SudokuSolvingPattern,
    val relatedCells: List<CellPosition> = emptyList(),
    val eliminations: List<CandidateElimination> = emptyList(),
) {
    val isPlacement: Boolean = eliminations.isEmpty()
}

sealed interface SudokuSolveResult {
    data class Invalid(
        val validation: SudokuValidation,
    ) : SudokuSolveResult

    data object Unsolvable : SudokuSolveResult

    class Multiple(
        solutions: List<SudokuGrid>,
        steps: List<SudokuSolutionStep>,
    ) : SudokuSolveResult {
        val solutions: List<SudokuGrid> = solutions.toList()
        val steps: List<SudokuSolutionStep> = steps.toList()

        init {
            require(this.solutions.size >= 2) { "Multiple result must contain at least two solutions." }
        }

        override fun equals(other: Any?): Boolean = other is Multiple &&
            solutions == other.solutions &&
            steps == other.steps

        override fun hashCode(): Int = 31 * solutions.hashCode() + steps.hashCode()

        override fun toString(): String = "Multiple(solutions=$solutions, steps=$steps)"
    }

    class Unique(
        val solvedGrid: SudokuGrid,
        steps: List<SudokuSolutionStep>,
    ) : SudokuSolveResult {
        val steps: List<SudokuSolutionStep> = steps.toList()

        override fun equals(other: Any?): Boolean = other is Unique &&
            solvedGrid == other.solvedGrid &&
            steps == other.steps

        override fun hashCode(): Int = 31 * solvedGrid.hashCode() + steps.hashCode()

        override fun toString(): String = "Unique(solvedGrid=$solvedGrid, steps=$steps)"
    }
}
