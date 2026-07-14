package pet.project.sudokusolver.domain

internal object DeathBlossomStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.DeathBlossom

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val stemIndexes = state.board.indices.filter { index ->
            state.board[index] == 0 && state.candidates[index].size in MinimumStemSize..MaximumStemSize
        }
        if (stemIndexes.isEmpty()) return null

        val sets = AlmostLockedSetCatalog.find(state)
        for (stemIndex in stemIndexes) {
            findForStem(state, sets, stemIndex)?.let { return it }
        }
        return null
    }

    private fun findForStem(
        state: SudokuBoardState,
        sets: List<AlmostLockedSet>,
        stemIndex: Int,
    ): SudokuSolutionStep? {
        val stemCandidates = state.candidates[stemIndex].sorted().toSet()
        val optionsByCandidate = stemCandidates.map { linkCandidate ->
            val options = sets.filter { set ->
                stemIndex !in set.indexes &&
                    linkCandidate in set.candidates &&
                    (set.candidates - stemCandidates).isNotEmpty() &&
                    set.occurrences(linkCandidate, state).all { occurrence ->
                        cellsSeeEachOther(stemIndex, occurrence)
                    }
            }
            PetalOptions(linkCandidate = linkCandidate, sets = options)
        }
        if (optionsByCandidate.any { it.sets.isEmpty() }) return null

        val searchOrder = optionsByCandidate.sortedWith(
            compareBy<PetalOptions> { it.sets.size }.thenBy { it.linkCandidate },
        )
        val selectedPetals = mutableListOf<SelectedPetal>()
        val occupiedIndexes = mutableSetOf<Int>()
        var visitedNodes = 0

        fun search(depth: Int, commonCandidates: Set<Int>): SudokuSolutionStep? {
            if (visitedNodes >= MaximumSearchNodeCount) return null
            visitedNodes += 1

            if (depth == searchOrder.size) {
                val orderedPetals = selectedPetals.sortedBy { it.linkCandidate }
                val relatedIndexes = listOf(stemIndex) + orderedPetals.flatMap { it.set.indexes }
                val excludedIndexes = relatedIndexes.toSet()
                for (candidate in commonCandidates.sorted()) {
                    val occurrences = orderedPetals.flatMap { petal ->
                        petal.set.occurrences(candidate, state)
                    }
                    val eliminations = collectAlsEliminations(
                        state = state,
                        candidate = candidate,
                        occurrences = occurrences,
                        excludedIndexes = excludedIndexes,
                    )
                    SudokuStepFactory.elimination(
                        pattern = pattern,
                        relatedIndexes = relatedIndexes,
                        eliminations = eliminations,
                    )?.let { return it }
                }
                return null
            }

            val option = searchOrder[depth]
            for (set in option.sets) {
                if (set.indexes.any { it in occupiedIndexes }) continue
                val nextCommonCandidates = commonCandidates
                    .intersect(set.candidates)
                    .filterTo(linkedSetOf()) { it !in stemCandidates }
                if (nextCommonCandidates.isEmpty()) continue

                selectedPetals += SelectedPetal(option.linkCandidate, set)
                occupiedIndexes.addAll(set.indexes)
                search(depth + 1, nextCommonCandidates)?.let { return it }
                set.indexes.forEach(occupiedIndexes::remove)
                selectedPetals.removeAt(selectedPetals.lastIndex)
            }
            return null
        }

        return search(depth = 0, commonCandidates = (1..9).toSet() - stemCandidates)
    }

    private data class PetalOptions(
        val linkCandidate: Int,
        val sets: List<AlmostLockedSet>,
    )

    private data class SelectedPetal(
        val linkCandidate: Int,
        val set: AlmostLockedSet,
    )

    private const val MinimumStemSize = 2
    private const val MaximumStemSize = 3
    private const val MaximumSearchNodeCount = 20_000
}
