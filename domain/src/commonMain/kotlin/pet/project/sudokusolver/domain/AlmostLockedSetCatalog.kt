package pet.project.sudokusolver.domain

internal data class AlmostLockedSet(
    val indexes: List<Int>,
    val candidateMask: Int,
) {
    val candidates: Set<Int> = (1..9)
        .filterTo(linkedSetOf()) { value -> candidateMask containsCandidate value }

    fun occurrences(value: Int, state: SudokuBoardState): List<Int> = indexes.filter { index ->
        value in state.candidates[index]
    }
}

internal object AlmostLockedSetCatalog {
    fun find(state: SudokuBoardState): List<AlmostLockedSet> {
        val resultByIndexes = linkedMapOf<List<Int>, AlmostLockedSet>()

        for (unit in sudokuUnits()) {
            val eligibleIndexes = unit.filter { index ->
                state.board[index] == 0 && state.candidates[index].isNotEmpty()
            }
            for (size in 1..minOf(MaximumCellCount, eligibleIndexes.size)) {
                collectInUnit(
                    state = state,
                    eligibleIndexes = eligibleIndexes,
                    targetSize = size,
                    resultByIndexes = resultByIndexes,
                )
            }
        }

        return resultByIndexes.values.sortedWith(
            Comparator { first, second ->
                val sizeComparison = first.indexes.size.compareTo(second.indexes.size)
                if (sizeComparison != 0) {
                    sizeComparison
                } else {
                    compareIndexes(first.indexes, second.indexes)
                }
            },
        )
    }

    private fun collectInUnit(
        state: SudokuBoardState,
        eligibleIndexes: List<Int>,
        targetSize: Int,
        resultByIndexes: MutableMap<List<Int>, AlmostLockedSet>,
    ) {
        val selectedIndexes = ArrayList<Int>(targetSize)

        fun collect(start: Int, candidateMask: Int) {
            if (selectedIndexes.size == targetSize) {
                if (candidateMask.candidateCount() == targetSize + 1) {
                    val indexes = selectedIndexes.toList()
                    if (indexes !in resultByIndexes) {
                        resultByIndexes[indexes] = AlmostLockedSet(
                            indexes = indexes,
                            candidateMask = candidateMask,
                        )
                    }
                }
                return
            }

            val remainingCellCount = targetSize - selectedIndexes.size
            val lastStart = eligibleIndexes.size - remainingCellCount
            for (position in start..lastStart) {
                val index = eligibleIndexes[position]
                val nextMask = candidateMask or state.candidates[index].toCandidateMask()
                if (nextMask.candidateCount() > targetSize + 1) continue

                selectedIndexes += index
                collect(position + 1, nextMask)
                selectedIndexes.removeAt(selectedIndexes.lastIndex)
            }
        }

        collect(start = 0, candidateMask = 0)
    }

    private fun compareIndexes(first: List<Int>, second: List<Int>): Int {
        for (index in first.indices) {
            val comparison = first[index].compareTo(second[index])
            if (comparison != 0) return comparison
        }
        return 0
    }

    private const val MaximumCellCount = SudokuGrid.Size - 1
}

internal data class AlmostLockedSetRccLink(
    val first: AlmostLockedSet,
    val second: AlmostLockedSet,
    val restrictedCandidates: Set<Int>,
)

internal object AlmostLockedSetRccGraph {
    fun build(
        state: SudokuBoardState,
        sets: List<AlmostLockedSet>,
    ): List<AlmostLockedSetRccLink> {
        val setIndexesByCandidate = Array(10) { mutableListOf<Int>() }
        sets.forEachIndexed { setIndex, set ->
            set.candidates.forEach { value -> setIndexesByCandidate[value] += setIndex }
        }

        val restrictedCandidatesByPair = linkedMapOf<Long, MutableSet<Int>>()
        for (value in 1..9) {
            val setIndexes = setIndexesByCandidate[value]
            for (firstPosition in 0 until setIndexes.lastIndex) {
                val firstIndex = setIndexes[firstPosition]
                val first = sets[firstIndex]
                for (secondPosition in firstPosition + 1 until setIndexes.size) {
                    val secondIndex = setIndexes[secondPosition]
                    val second = sets[secondIndex]
                    if (first.indexes.any { it in second.indexes }) continue
                    if (!isRestrictedCommonCandidate(state, first, second, value)) continue

                    val pairKey = pairKey(firstIndex, secondIndex)
                    restrictedCandidatesByPair
                        .getOrPut(pairKey, ::linkedSetOf)
                        .add(value)
                }
            }
        }

        return restrictedCandidatesByPair.entries.sortedBy { it.key }.map { (pairKey, candidates) ->
            AlmostLockedSetRccLink(
                first = sets[(pairKey ushr 32).toInt()],
                second = sets[pairKey.toInt()],
                restrictedCandidates = candidates.sorted().toSet(),
            )
        }
    }

    private fun isRestrictedCommonCandidate(
        state: SudokuBoardState,
        first: AlmostLockedSet,
        second: AlmostLockedSet,
        value: Int,
    ): Boolean {
        val firstOccurrences = first.occurrences(value, state)
        val secondOccurrences = second.occurrences(value, state)
        return firstOccurrences.isNotEmpty() &&
            secondOccurrences.isNotEmpty() &&
            firstOccurrences.all { firstIndex ->
                secondOccurrences.all { secondIndex -> cellsSeeEachOther(firstIndex, secondIndex) }
            }
    }

    private fun pairKey(firstIndex: Int, secondIndex: Int): Long =
        (firstIndex.toLong() shl 32) or (secondIndex.toLong() and 0xffffffffL)
}

internal infix fun Int.containsCandidate(value: Int): Boolean = this and (1 shl value) != 0

internal fun Set<Int>.toCandidateMask(): Int = fold(0) { mask, value -> mask or (1 shl value) }

internal fun cellsSeeEachOther(firstIndex: Int, secondIndex: Int): Boolean =
    firstIndex != secondIndex &&
        (
            firstIndex.row() == secondIndex.row() ||
                firstIndex.column() == secondIndex.column() ||
                firstIndex.boxIndex() == secondIndex.boxIndex()
            )

private fun Int.candidateCount(): Int {
    var value = this
    var count = 0
    while (value != 0) {
        value = value and (value - 1)
        count += 1
    }
    return count
}

private fun Int.boxIndex(): Int = row() / 3 * 3 + column() / 3
