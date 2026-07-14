package pet.project.sudokusolver.domain

internal object AlsXyWingStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.AlsXyWing

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val sets = AlmostLockedSetCatalog.find(state)
        val setIndexes = sets.withIndex().associate { (index, set) -> set to index }
        val adjacency = Array(sets.size) { mutableListOf<RccNeighbor>() }

        for (link in AlmostLockedSetRccGraph.build(state, sets)) {
            if (link.restrictedCandidates.size != 1) continue
            val firstIndex = setIndexes.getValue(link.first)
            val secondIndex = setIndexes.getValue(link.second)
            val restrictedCandidate = link.restrictedCandidates.single()
            adjacency[firstIndex] += RccNeighbor(secondIndex, restrictedCandidate)
            adjacency[secondIndex] += RccNeighbor(firstIndex, restrictedCandidate)
        }

        for (hubIndex in sets.indices) {
            val neighbors = adjacency[hubIndex].sortedWith(
                compareBy<RccNeighbor> { it.setIndex }.thenBy { it.restrictedCandidate },
            )
            for (firstPosition in 0 until neighbors.lastIndex) {
                val firstLink = neighbors[firstPosition]
                for (secondPosition in firstPosition + 1 until neighbors.size) {
                    val secondLink = neighbors[secondPosition]
                    if (
                        firstLink.setIndex == secondLink.setIndex ||
                        firstLink.restrictedCandidate == secondLink.restrictedCandidate
                    ) {
                        continue
                    }

                    val first = sets[firstLink.setIndex]
                    val hub = sets[hubIndex]
                    val second = sets[secondLink.setIndex]
                    if (first.indexes.any { it in second.indexes }) continue

                    val sharedCandidates = first.candidates
                        .intersect(second.candidates)
                        .filter { candidate ->
                            candidate != firstLink.restrictedCandidate &&
                                candidate != secondLink.restrictedCandidate
                        }
                        .sorted()
                    val relatedIndexes = (first.indexes + hub.indexes + second.indexes).distinct()
                    val excludedIndexes = relatedIndexes.toSet()

                    for (candidate in sharedCandidates) {
                        val occurrences = first.occurrences(candidate, state) +
                            second.occurrences(candidate, state)
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
                }
            }
        }
        return null
    }

    private data class RccNeighbor(
        val setIndex: Int,
        val restrictedCandidate: Int,
    )
}
