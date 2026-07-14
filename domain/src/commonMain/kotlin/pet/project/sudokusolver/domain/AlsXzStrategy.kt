package pet.project.sudokusolver.domain

internal object AlsXzStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.AlsXz

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val almostLockedSets = AlmostLockedSetCatalog.find(state)
        val links = AlmostLockedSetRccGraph.build(state, almostLockedSets)

        for (link in links) {
            when (link.restrictedCandidates.size) {
                1 -> findSinglyLinkedStep(state, link)?.let { return it }
                2 -> findDoublyLinkedStep(state, link)?.let { return it }
            }
        }
        return null
    }

    private fun findSinglyLinkedStep(
        state: SudokuBoardState,
        link: AlmostLockedSetRccLink,
    ): SudokuSolutionStep? {
        val restrictedCandidate = link.restrictedCandidates.single()
        val sharedCandidates = link.first.candidates
            .intersect(link.second.candidates)
            .filter { it != restrictedCandidate }
            .sorted()
        val relatedIndexes = (link.first.indexes + link.second.indexes).distinct()

        for (candidate in sharedCandidates) {
            val occurrences = link.first.occurrences(candidate, state) +
                link.second.occurrences(candidate, state)
            val eliminations = collectEliminations(
                state = state,
                candidate = candidate,
                occurrences = occurrences,
                excludedIndexes = relatedIndexes.toSet(),
            )
            SudokuStepFactory.elimination(
                pattern = pattern,
                relatedIndexes = relatedIndexes,
                eliminations = eliminations,
            )?.let { return it }
        }
        return null
    }

    private fun findDoublyLinkedStep(
        state: SudokuBoardState,
        link: AlmostLockedSetRccLink,
    ): SudokuSolutionStep? {
        val relatedIndexes = (link.first.indexes + link.second.indexes).distinct()
        val removals = mutableMapOf<Int, MutableSet<Int>>()

        for (candidate in link.restrictedCandidates.sorted()) {
            collectRemovals(
                state = state,
                candidate = candidate,
                occurrences = link.first.occurrences(candidate, state) +
                    link.second.occurrences(candidate, state),
                excludedIndexes = relatedIndexes.toSet(),
                removals = removals,
            )
        }

        collectLockedSetRemovals(
            state = state,
            set = link.first,
            restrictedCandidates = link.restrictedCandidates,
            removals = removals,
        )
        collectLockedSetRemovals(
            state = state,
            set = link.second,
            restrictedCandidates = link.restrictedCandidates,
            removals = removals,
        )

        val eliminations = removals.entries
            .sortedBy { it.key }
            .map { (index, candidates) ->
                CandidateElimination(
                    row = index.row(),
                    column = index.column(),
                    values = candidates.sorted().toSet(),
                )
            }
        return SudokuStepFactory.elimination(
            pattern = pattern,
            relatedIndexes = relatedIndexes,
            eliminations = eliminations,
        )
    }

    private fun collectLockedSetRemovals(
        state: SudokuBoardState,
        set: AlmostLockedSet,
        restrictedCandidates: Set<Int>,
        removals: MutableMap<Int, MutableSet<Int>>,
    ) {
        for (candidate in (set.candidates - restrictedCandidates).sorted()) {
            collectRemovals(
                state = state,
                candidate = candidate,
                occurrences = set.occurrences(candidate, state),
                excludedIndexes = set.indexes.toSet(),
                removals = removals,
            )
        }
    }

    private fun collectEliminations(
        state: SudokuBoardState,
        candidate: Int,
        occurrences: List<Int>,
        excludedIndexes: Set<Int>,
    ): List<CandidateElimination> {
        val removals = mutableMapOf<Int, MutableSet<Int>>()
        collectRemovals(
            state = state,
            candidate = candidate,
            occurrences = occurrences,
            excludedIndexes = excludedIndexes,
            removals = removals,
        )
        return removals.entries
            .sortedBy { it.key }
            .map { (index, candidates) ->
                CandidateElimination(index.row(), index.column(), candidates.sorted().toSet())
            }
    }

    private fun collectRemovals(
        state: SudokuBoardState,
        candidate: Int,
        occurrences: List<Int>,
        excludedIndexes: Set<Int>,
        removals: MutableMap<Int, MutableSet<Int>>,
    ) {
        if (occurrences.isEmpty()) return
        for (index in state.board.indices) {
            if (
                index in excludedIndexes ||
                state.board[index] != 0 ||
                candidate !in state.candidates[index] ||
                occurrences.any { occurrence -> !cellsSeeEachOther(index, occurrence) }
            ) {
                continue
            }
            removals.getOrPut(index, ::mutableSetOf).add(candidate)
        }
    }
}
