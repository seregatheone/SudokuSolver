package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupedAicStrategyTest {
    @Test
    fun hodokuGroupedAicEliminatesAllCandidatesSeenByBothEndpoints() {
        val state = hodokuState()

        val step = GroupedAicStrategy.findStep(state)
        val proof = GroupedAicStrategy.findProof(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.GroupedAic, step.pattern)
        assertEquals(
            listOf(
                CandidateElimination(6, 3, setOf(5)),
                CandidateElimination(6, 4, setOf(5)),
                CandidateElimination(6, 5, setOf(5)),
                CandidateElimination(8, 0, setOf(5)),
            ),
            step.eliminations,
        )
        assertNotNull(proof)
        assertTrue(proof.path.nodes.any { node -> node.isGroup })
        assertTrue(
            proof.path.nodes.any { node ->
                node.value == 7 && node.memberIndexes == setOf(31, 40)
            },
        )
    }

    @Test
    fun hodokuProofLinksAlternateThroughTheExpectedGroup() {
        val graph = GroupedCandidateInferenceGraph.from(hodokuState())
        fun single(index: Int, value: Int) = graph.singleNodes.single { node ->
            node.value == value && node.memberIndexes == setOf(index)
        }
        val expectedNodes = listOf(
            single(55, 5),
            single(55, 6),
            single(46, 6),
            single(46, 3),
            single(48, 3),
            single(48, 7),
            graph.groupNodes.single { node ->
                node.value == 7 && node.memberIndexes == setOf(31, 40)
            },
            single(22, 7),
            single(22, 6),
            single(23, 6),
            single(77, 6),
            single(77, 5),
        )
        val expectedLinks = listOf(
            InferenceLinkType.Strong,
            InferenceLinkType.Weak,
            InferenceLinkType.Strong,
            InferenceLinkType.Weak,
            InferenceLinkType.Strong,
            InferenceLinkType.Weak,
            InferenceLinkType.Strong,
            InferenceLinkType.Weak,
            InferenceLinkType.Strong,
            InferenceLinkType.Weak,
            InferenceLinkType.Strong,
        )

        expectedLinks.forEachIndexed { index, link ->
            assertTrue(
                graph.areLinked(expectedNodes[index], expectedNodes[index + 1], link),
                "Missing $link link at proof edge $index",
            )
        }
    }

    @Test
    fun victimMustSeeEveryMemberOfAGroupEndpoint() {
        val state = candidateStateWithout(setOf(1)) {
            candidate(0, 1, 2, 3)
            candidate(1, 1, 4, 5)
            candidate(27, 1, 6, 7)
        }
        val graph = GroupedCandidateInferenceGraph.from(state)
        val group = graph.groupNodes.single { node ->
            node.value == 1 && node.memberIndexes == setOf(0, 1)
        }
        val partialVictim = graph.singleNodes.single { node ->
            node.value == 1 && node.memberIndexes == setOf(27)
        }

        assertFalse(graph.areUniversallyWeaklyLinked(partialVictim, group))
    }

    @Test
    fun rejectsAGroupThatCrossesBothRowsAndColumns() {
        val invalid = GroupedCandidateNode.groupOrNull(
            listOf(
                CandidateNode(index = 0, value = 4),
                CandidateNode(index = 10, value = 4),
            ),
        )

        assertNull(invalid)
    }

    @Test
    fun proofDoesNotUseOverlappingGroupNodes() {
        val proof = GroupedAicStrategy.findProof(hodokuState())

        assertNotNull(proof)
        val pathMembers = proof.path.nodes.flatMap { node -> node.members }.toSet()
        assertTrue(proof.eliminations.none { victim -> victim.members.single() in pathMembers })
        val groups = proof.path.nodes.filter { node -> node.isGroup }
        for (firstIndex in groups.indices) {
            for (secondIndex in firstIndex + 1 until groups.size) {
                assertTrue(
                    groups[firstIndex].memberIndexes
                        .intersect(groups[secondIndex].memberIndexes)
                        .isEmpty(),
                )
            }
        }
    }

    @Test
    fun groupCannotOverlapASingleCandidateAlreadyInThePath() {
        val group = checkNotNull(
            GroupedCandidateNode.groupOrNull(
                listOf(
                    CandidateNode(index = 0, value = 4),
                    CandidateNode(index = 1, value = 4),
                ),
            ),
        )
        val sameCandidate = GroupedCandidateNode.single(CandidateNode(index = 0, value = 4))
        val differentCandidateInSameCell = GroupedCandidateNode.single(CandidateNode(index = 0, value = 5))

        assertTrue(GroupedAicStrategy.overlapsPathMember(group, listOf(sameCandidate)))
        assertFalse(
            GroupedAicStrategy.overlapsPathMember(group, listOf(differentCandidateInSameCell)),
        )
    }

    @Test
    fun groupedAicHonorsDepthGuard() {
        assertNull(GroupedAicStrategy.findProof(hodokuState(), maxDepth = 3))
        assertNotNull(GroupedAicStrategy.findProof(hodokuState(), maxDepth = 11))
        assertNull(GroupedAicStrategy.findProof(hodokuState(), maximumStates = 1))
    }

    @Test
    fun groupedAicSearchIsDeterministic() {
        val results = List(10) { GroupedAicStrategy.findStep(hodokuState()) }

        assertEquals(1, results.distinct().size)
        assertNotNull(results.first())
    }

    private fun hodokuState(): SudokuBoardState = stateFrom(
        "3451289.." +
            "976...281" +
            "281...345" +
            ".......1." +
            "1..6...3." +
            "4.2.815.9" +
            "7.4...128" +
            "819.4.653" +
            ".2381.794",
    )

    private fun stateFrom(puzzle: String): SudokuBoardState {
        val grid = SudokuGrid(
            puzzle.map { character ->
                if (character == '.') {
                    SudokuCell()
                } else {
                    SudokuCell(value = character.digitToInt(), isGiven = true)
                }
            },
        )
        return checkNotNull(SudokuBoardState.from(grid))
    }

    private fun candidateStateWithout(
        excludedValues: Set<Int>,
        configure: CandidateFixture.() -> Unit,
    ): SudokuBoardState {
        val fixture = CandidateFixture((1..9).toSet() - excludedValues)
        fixture.configure()
        return checkNotNull(SudokuBoardState.from(SudokuGrid(fixture.cells)))
    }

    private class CandidateFixture(background: Set<Int>) {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = background) }

        fun candidate(index: Int, vararg values: Int) {
            cells[index] = SudokuCell(notes = values.toSet())
        }
    }
}
