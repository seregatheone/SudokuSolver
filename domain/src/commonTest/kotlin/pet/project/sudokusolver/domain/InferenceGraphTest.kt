package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InferenceGraphTest {
    @Test
    fun createsStrongLinkForBivalueCellOnly() {
        val graph = graphOf(
            0 to setOf(1, 2),
            10 to setOf(3, 4, 5),
        )

        assertTrue(graph.areStronglyLinked(node(0, 1), node(0, 2)))
        assertFalse(graph.areStronglyLinked(node(10, 3), node(10, 4)))
        assertTrue(graph.areWeaklyLinked(node(10, 3), node(10, 4)))
    }

    @Test
    fun createsStrongLinksForConjugatePairsInEveryUnitType() {
        val rowGraph = graphOf(0 to setOf(1, 4, 5), 4 to setOf(1, 6, 7))
        val columnGraph = graphOf(0 to setOf(2, 4, 5), 36 to setOf(2, 6, 7))
        val boxGraph = graphOf(0 to setOf(3, 4, 5), 10 to setOf(3, 6, 7))

        assertTrue(rowGraph.areStronglyLinked(node(0, 1), node(4, 1)))
        assertTrue(columnGraph.areStronglyLinked(node(0, 2), node(36, 2)))
        assertTrue(boxGraph.areStronglyLinked(node(0, 3), node(10, 3)))
    }

    @Test
    fun rejectsUnitStrongLinksWhenCandidateHasAThirdPosition() {
        val rowGraph = graphOf(
            0 to setOf(1, 4, 5),
            4 to setOf(1, 6, 7),
            8 to setOf(1, 2, 3),
        )
        val columnGraph = graphOf(
            0 to setOf(2, 4, 5),
            36 to setOf(2, 6, 7),
            72 to setOf(2, 3, 8),
        )
        val boxGraph = graphOf(
            0 to setOf(3, 4, 5),
            10 to setOf(3, 6, 7),
            20 to setOf(2, 3, 8),
        )

        assertFalse(rowGraph.areStronglyLinked(node(0, 1), node(4, 1)))
        assertFalse(columnGraph.areStronglyLinked(node(0, 2), node(36, 2)))
        assertFalse(boxGraph.areStronglyLinked(node(0, 3), node(10, 3)))
        assertTrue(rowGraph.areWeaklyLinked(node(0, 1), node(4, 1)))
        assertTrue(columnGraph.areWeaklyLinked(node(0, 2), node(36, 2)))
        assertTrue(boxGraph.areWeaklyLinked(node(0, 3), node(10, 3)))
    }

    @Test
    fun weakLinksRequireSameCellOrSameValueInPeerCells() {
        val graph = graphOf(
            0 to setOf(1, 2, 3),
            4 to setOf(1, 4, 5),
            40 to setOf(1, 6, 7),
        )

        assertTrue(graph.areWeaklyLinked(node(0, 1), node(0, 2)))
        assertTrue(graph.areWeaklyLinked(node(0, 1), node(4, 1)))
        assertFalse(graph.areWeaklyLinked(node(0, 1), node(4, 4)))
        assertFalse(graph.areWeaklyLinked(node(0, 1), node(40, 1)))
    }

    @Test
    fun strongLinkCanAlsoBeTraversedAsAWeakInference() {
        val graph = graphOf(0 to setOf(1, 2))

        assertEquals(listOf(node(0, 2)), graph.neighbors(node(0, 1), InferenceLinkType.Strong))
        assertEquals(listOf(node(0, 2)), graph.neighbors(node(0, 1), InferenceLinkType.Weak))
    }

    @Test
    fun findsShortestAlternatingPathWithExplicitEndpointLinkTypes() {
        val graph = alternatingFixture()

        val path = findAlternatingPath(
            graph = graph,
            start = node(0, 1),
            end = node(79, 1),
            firstLink = InferenceLinkType.Strong,
            lastLink = InferenceLinkType.Strong,
            maxDepth = 7,
        )

        assertNotNull(path)
        assertEquals(
            listOf(
                node(0, 1),
                node(0, 2),
                node(4, 2),
                node(40, 2),
                node(40, 3),
                node(43, 3),
                node(79, 3),
                node(79, 1),
            ),
            path.nodes,
        )
        assertEquals(
            listOf(
                InferenceLinkType.Strong,
                InferenceLinkType.Weak,
                InferenceLinkType.Strong,
                InferenceLinkType.Weak,
                InferenceLinkType.Strong,
                InferenceLinkType.Weak,
                InferenceLinkType.Strong,
            ),
            path.links,
        )
    }

    @Test
    fun alternatingPathHonorsDepthCapAndBrokenGraph() {
        val graph = alternatingFixture()
        val tooShort = findAlternatingPath(
            graph = graph,
            start = node(0, 1),
            end = node(79, 1),
            firstLink = InferenceLinkType.Strong,
            lastLink = InferenceLinkType.Strong,
            maxDepth = 6,
        )
        val brokenGraph = graphOf(
            0 to setOf(1, 2),
            4 to setOf(4, 5, 6),
            8 to setOf(2, 7, 8),
            40 to setOf(2, 3, 6),
            43 to setOf(1, 3, 8),
            79 to setOf(1, 3),
            25 to setOf(3, 4, 5),
            5 to setOf(1, 4, 5),
            7 to setOf(1, 4, 9),
        )
        val broken = findAlternatingPath(
            graph = brokenGraph,
            start = node(0, 1),
            end = node(79, 1),
            firstLink = InferenceLinkType.Strong,
            lastLink = InferenceLinkType.Strong,
            maxDepth = 7,
        )

        assertNull(tooShort)
        assertNull(broken)
    }

    @Test
    fun alternatingPathHonorsSearchStateCap() {
        val path = findAlternatingPath(
            graph = alternatingFixture(),
            start = node(0, 1),
            end = node(79, 1),
            firstLink = InferenceLinkType.Strong,
            lastLink = InferenceLinkType.Strong,
            maxDepth = 7,
            maximumStates = 1,
        )

        assertNull(path)
    }

    @Test
    fun repeatedAlternatingSearchIsDeterministic() {
        val graph = alternatingFixture()

        val results = List(20) {
            findAlternatingPath(
                graph = graph,
                start = node(0, 1),
                end = node(79, 1),
                firstLink = InferenceLinkType.Strong,
                lastLink = InferenceLinkType.Strong,
                maxDepth = 7,
            )
        }

        assertEquals(1, results.distinct().size)
        assertNotNull(results.first())
    }

    @Test
    fun singleDigitViewDelegatesToSharedGraph() {
        val graph = graphOf(
            0 to setOf(1, 2),
            4 to setOf(1, 3),
        )
        val digitGraph = graph.forDigit(1)

        assertEquals(listOf(node(0, 1), node(4, 1)), digitGraph.nodes)
        assertTrue(digitGraph.areLinked(node(0, 1), node(4, 1), InferenceLinkType.Strong))
        assertFalse(digitGraph.areLinked(node(0, 2), node(4, 1), InferenceLinkType.Weak))
    }

    private fun alternatingFixture(): CandidateInferenceGraph = graphOf(
        0 to setOf(1, 2),
        4 to setOf(2, 4, 5),
        8 to setOf(2, 7, 8),
        40 to setOf(2, 3, 6),
        43 to setOf(1, 3, 8),
        79 to setOf(1, 3),
        25 to setOf(3, 4, 5),
        5 to setOf(1, 4, 5),
        7 to setOf(1, 4, 9),
    )

    private fun graphOf(vararg candidates: Pair<Int, Set<Int>>): CandidateInferenceGraph {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = setOf(9)) }
        candidates.forEach { (index, values) -> cells[index] = SudokuCell(notes = values) }
        return CandidateInferenceGraph.from(checkNotNull(SudokuBoardState.from(SudokuGrid(cells))))
    }

    private fun node(index: Int, value: Int) = CandidateNode(index = index, value = value)
}
