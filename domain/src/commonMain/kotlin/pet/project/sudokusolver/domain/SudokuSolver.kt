package pet.project.sudokusolver.domain

class SudokuSolver {
    fun solve(grid: SudokuGrid): SudokuSolveResult? = solveInternal(grid)

    fun hint(grid: SudokuGrid): SudokuSolutionStep? = solveInternal(grid)?.steps?.firstOrNull()

    private fun solveInternal(grid: SudokuGrid): SudokuSolveResult? {
        val board = grid.values().map { it ?: 0 }.toIntArray()
        if (!isValidBoard(board)) return null

        val candidates = initialCandidates(grid, board)
        val steps = mutableListOf<SudokuSolutionStep>()
        while (true) {
            val step = findLogicalStep(board, candidates) ?: break
            applyStep(board, candidates, step)
            steps += step
        }

        val solvedBoard = board.copyOf()
        if (!solveBoard(solvedBoard)) return null

        val solvedGrid = SudokuGrid(
            solvedBoard.mapIndexed { index, value ->
                val source = grid.cells[index]
                SudokuCell(value = value, isGiven = source.isGiven)
            },
        )

        return SudokuSolveResult(solvedGrid = solvedGrid, steps = steps)
    }

    private fun initialCandidates(grid: SudokuGrid, board: IntArray): Array<MutableSet<Int>> = Array(SudokuGrid.CellCount) { index ->
        if (board[index] != 0) {
            mutableSetOf()
        } else {
            val legalCandidates = candidatesFor(board, index).toSet()
            val notes = grid.cells[index].notes
            if (notes.isEmpty()) legalCandidates.toMutableSet() else notes.intersect(legalCandidates).toMutableSet()
        }
    }

    private fun findLogicalStep(board: IntArray, candidates: Array<MutableSet<Int>>): SudokuSolutionStep? {
        findNakedSingle(board, candidates)?.let { return it }
        findHiddenSingles(board, candidates)?.let { return it }
        findNakedSubset(board, candidates, size = 2)?.let { return it }
        findNakedSubset(board, candidates, size = 3)?.let { return it }
        findNakedSubset(board, candidates, size = 4)?.let { return it }
        findHiddenSubset(board, candidates, size = 2)?.let { return it }
        findHiddenSubset(board, candidates, size = 3)?.let { return it }
        findHiddenSubset(board, candidates, size = 4)?.let { return it }
        findPointingSet(board, candidates)?.let { return it }
        return findBoxLineReduction(board, candidates)
    }

    private fun applyStep(board: IntArray, candidates: Array<MutableSet<Int>>, step: SudokuSolutionStep) {
        if (step.isPlacement) {
            val index = step.row * SudokuGrid.Size + step.column
            board[index] = step.value
            candidates[index].clear()
            peerIndexes(index).forEach { peer -> candidates[peer].remove(step.value) }
            return
        }

        step.eliminations.forEach { elimination ->
            val index = elimination.row * SudokuGrid.Size + elimination.column
            candidates[index].removeAll(elimination.values)
        }
    }

    private fun findNakedSingle(board: IntArray, candidates: Array<MutableSet<Int>>): SudokuSolutionStep? {
        for (index in board.indices) {
            if (board[index] != 0) continue
            if (candidates[index].size == 1) {
                return placementStep(
                    index = index,
                    value = candidates[index].first(),
                    pattern = SudokuSolvingPattern.NakedSingle,
                    relatedIndexes = filledPeers(board, index).map { it.index },
                )
            }
        }
        return null
    }

    private fun findHiddenSingles(board: IntArray, candidates: Array<MutableSet<Int>>): SudokuSolutionStep? {
        rows().forEach { indexes ->
            hiddenSingleInUnit(indexes, board, candidates, SudokuSolvingPattern.HiddenSingleRow)?.let { return it }
        }
        columns().forEach { indexes ->
            hiddenSingleInUnit(indexes, board, candidates, SudokuSolvingPattern.HiddenSingleColumn)?.let { return it }
        }
        boxes().forEach { indexes ->
            hiddenSingleInUnit(indexes, board, candidates, SudokuSolvingPattern.HiddenSingleBox)?.let { return it }
        }
        return null
    }

    private fun hiddenSingleInUnit(
        indexes: List<Int>,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
        pattern: SudokuSolvingPattern,
    ): SudokuSolutionStep? {
        for (value in 1..9) {
            val positions = indexes.filter { index -> board[index] == 0 && value in candidates[index] }
            if (positions.size == 1) {
                val index = positions.first()
                return placementStep(
                    index = index,
                    value = value,
                    pattern = pattern,
                    relatedIndexes = indexes.filter { it != index },
                )
            }
        }
        return null
    }

    private fun findNakedSubset(
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
        size: Int,
    ): SudokuSolutionStep? {
        val pattern = when (size) {
            2 -> SudokuSolvingPattern.NakedPair
            3 -> SudokuSolvingPattern.NakedTriple
            else -> SudokuSolvingPattern.NakedQuad
        }

        for (unit in units()) {
            val candidateIndexes = unit.filter { index -> board[index] == 0 && candidates[index].size in 2..size }
            for (subset in candidateIndexes.combinations(size)) {
                val subsetValues = subset.flatMap { index -> candidates[index] }.toSet()
                if (subsetValues.size != size) continue

                val eliminations = unit
                    .filter { index -> index !in subset && board[index] == 0 }
                    .mapNotNull { index ->
                        val removed = candidates[index].intersect(subsetValues).sorted().toSet()
                        if (removed.isEmpty()) null else CandidateElimination(index.row(), index.column(), removed)
                    }

                eliminationStep(pattern, subset, eliminations)?.let { return it }
            }
        }
        return null
    }

    private fun findHiddenSubset(
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
        size: Int,
    ): SudokuSolutionStep? {
        val pattern = when (size) {
            2 -> SudokuSolvingPattern.HiddenPair
            3 -> SudokuSolvingPattern.HiddenTriple
            else -> SudokuSolvingPattern.HiddenQuad
        }

        for (unit in units()) {
            for (values in (1..9).toList().combinations(size)) {
                val positionsByValue = values.map { value ->
                    unit.filter { index -> board[index] == 0 && value in candidates[index] }
                }
                if (positionsByValue.any { it.isEmpty() }) continue

                val subset = positionsByValue.flatten().distinct()
                if (subset.size != size) continue

                val allowedValues = values.toSet()
                val eliminations = subset.mapNotNull { index ->
                    val removed = (candidates[index] - allowedValues).sorted().toSet()
                    if (removed.isEmpty()) null else CandidateElimination(index.row(), index.column(), removed)
                }

                eliminationStep(pattern, subset, eliminations)?.let { return it }
            }
        }
        return null
    }

    private fun findPointingSet(board: IntArray, candidates: Array<MutableSet<Int>>): SudokuSolutionStep? {
        for (box in boxes()) {
            for (value in 1..9) {
                val positions = box.filter { index -> board[index] == 0 && value in candidates[index] }
                if (positions.size !in 2..3) continue

                val sameRow = positions.map { it.row() }.distinct().singleOrNull()
                if (sameRow != null) {
                    val eliminations = rows()[sameRow]
                        .filter { index -> index !in box && board[index] == 0 && value in candidates[index] }
                        .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
                    eliminationStep(pointingPattern(positions.size), positions, eliminations)?.let { return it }
                }

                val sameColumn = positions.map { it.column() }.distinct().singleOrNull()
                if (sameColumn != null) {
                    val eliminations = columns()[sameColumn]
                        .filter { index -> index !in box && board[index] == 0 && value in candidates[index] }
                        .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
                    eliminationStep(pointingPattern(positions.size), positions, eliminations)?.let { return it }
                }
            }
        }
        return null
    }

    private fun findBoxLineReduction(board: IntArray, candidates: Array<MutableSet<Int>>): SudokuSolutionStep? {
        for (row in rows()) {
            boxLineReductionInUnit(row, boxes(), board, candidates)?.let { return it }
        }
        for (column in columns()) {
            boxLineReductionInUnit(column, boxes(), board, candidates)?.let { return it }
        }
        return null
    }

    private fun boxLineReductionInUnit(
        unit: List<Int>,
        boxes: List<List<Int>>,
        board: IntArray,
        candidates: Array<MutableSet<Int>>,
    ): SudokuSolutionStep? {
        for (value in 1..9) {
            val positions = unit.filter { index -> board[index] == 0 && value in candidates[index] }
            if (positions.size < 2) continue

            val box = boxes.singleOrNull { currentBox -> positions.all { it in currentBox } } ?: continue
            val eliminations = box
                .filter { index -> index !in unit && board[index] == 0 && value in candidates[index] }
                .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
            eliminationStep(SudokuSolvingPattern.BoxLineReduction, positions, eliminations)?.let { return it }
        }
        return null
    }

    private fun pointingPattern(size: Int): SudokuSolvingPattern =
        if (size == 2) SudokuSolvingPattern.PointingPair else SudokuSolvingPattern.PointingTriple

    private fun placementStep(
        index: Int,
        value: Int,
        pattern: SudokuSolvingPattern,
        relatedIndexes: List<Int>,
    ) = SudokuSolutionStep(
        row = index.row(),
        column = index.column(),
        value = value,
        pattern = pattern,
        relatedCells = relatedIndexes.distinct().filter { it != index }.map { it.toCellPosition() },
    )

    private fun eliminationStep(
        pattern: SudokuSolvingPattern,
        relatedIndexes: List<Int>,
        eliminations: List<CandidateElimination>,
    ): SudokuSolutionStep? {
        val first = eliminations.firstOrNull() ?: return null
        return SudokuSolutionStep(
            row = first.row,
            column = first.column,
            value = first.values.minOrNull() ?: return null,
            pattern = pattern,
            relatedCells = relatedIndexes.distinct().map { it.toCellPosition() },
            eliminations = eliminations,
        )
    }

    private fun solveBoard(board: IntArray): Boolean {
        val emptyIndex = findEmptyCellWithFewestCandidates(board) ?: return true
        for (candidate in candidatesFor(board, emptyIndex)) {
            board[emptyIndex] = candidate
            if (solveBoard(board)) return true
            board[emptyIndex] = 0
        }
        return false
    }

    private fun findEmptyCellWithFewestCandidates(board: IntArray): Int? {
        var bestIndex: Int? = null
        var bestCandidateCount = Int.MAX_VALUE

        for (index in board.indices) {
            if (board[index] != 0) continue
            val candidateCount = candidatesFor(board, index).size
            if (candidateCount == 0) return index
            if (candidateCount < bestCandidateCount) {
                bestCandidateCount = candidateCount
                bestIndex = index
            }
        }

        return bestIndex
    }

    private fun candidatesFor(board: IntArray, index: Int): List<Int> {
        val row = index.row()
        val column = index.column()
        return (1..9).filter { value -> canPlace(board, row, column, value) }
    }

    private fun canPlace(board: IntArray, row: Int, column: Int, value: Int): Boolean {
        for (i in 0 until SudokuGrid.Size) {
            if (board[row * SudokuGrid.Size + i] == value) return false
            if (board[i * SudokuGrid.Size + column] == value) return false
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxColumn until boxColumn + 3) {
                if (board[r * SudokuGrid.Size + c] == value) return false
            }
        }

        return true
    }

    private fun isValidBoard(board: IntArray): Boolean {
        for (index in board.indices) {
            val value = board[index]
            if (value == 0) continue
            board[index] = 0
            val isValid = canPlace(board, index.row(), index.column(), value)
            board[index] = value
            if (!isValid) return false
        }
        return true
    }

    private fun filledPeers(board: IntArray, index: Int): List<CellPosition> = peerIndexes(index)
        .filter { board[it] != 0 }
        .map { it.toCellPosition() }

    private fun peerIndexes(index: Int): Set<Int> {
        val row = index.row()
        val column = index.column()
        val peers = mutableSetOf<Int>()

        for (i in 0 until SudokuGrid.Size) {
            peers += row * SudokuGrid.Size + i
            peers += i * SudokuGrid.Size + column
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxColumn until boxColumn + 3) {
                peers += r * SudokuGrid.Size + c
            }
        }

        peers -= index
        return peers
    }

    private fun units(): List<List<Int>> = rows() + columns() + boxes()

    private fun rows(): List<List<Int>> = (0 until SudokuGrid.Size).map { row ->
        (0 until SudokuGrid.Size).map { column -> row * SudokuGrid.Size + column }
    }

    private fun columns(): List<List<Int>> = (0 until SudokuGrid.Size).map { column ->
        (0 until SudokuGrid.Size).map { row -> row * SudokuGrid.Size + column }
    }

    private fun boxes(): List<List<Int>> = buildList {
        for (boxRow in 0 until SudokuGrid.Size step 3) {
            for (boxColumn in 0 until SudokuGrid.Size step 3) {
                add(
                    buildList {
                        for (row in boxRow until boxRow + 3) {
                            for (column in boxColumn until boxColumn + 3) {
                                add(row * SudokuGrid.Size + column)
                            }
                        }
                    },
                )
            }
        }
    }

    private fun <T> List<T>.combinations(size: Int): List<List<T>> {
        if (size == 0) return listOf(emptyList())
        if (size > this.size) return emptyList()

        val result = mutableListOf<List<T>>()
        fun collect(start: Int, current: List<T>) {
            if (current.size == size) {
                result += current
                return
            }
            for (index in start until this.size) {
                collect(index + 1, current + this[index])
            }
        }
        collect(start = 0, current = emptyList())
        return result
    }

    private fun Int.row(): Int = this / SudokuGrid.Size

    private fun Int.column(): Int = this % SudokuGrid.Size

    private fun Int.toCellPosition() = CellPosition(row = row(), column = column())
}
