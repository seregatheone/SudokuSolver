package pet.project.sudokusolver.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponsiveLayoutTest {
    @Test
    fun boardLayoutSwitchesAtWideBreakpoint() {
        assertFalse(useWideBoardLayout(WideBoardBreakpointDp - 1f, 800f))
        assertTrue(useWideBoardLayout(WideBoardBreakpointDp, 800f))
    }

    @Test
    fun shortLandscapeUsesSideBySideBoardLayout() {
        assertTrue(useWideBoardLayout(LandscapeBoardBreakpointDp, ShortBoardHeightDp - 1f))
        assertFalse(useWideBoardLayout(LandscapeBoardBreakpointDp - 1f, ShortBoardHeightDp - 1f))
    }

    @Test
    fun boardWidthIsBoundedByBothViewportAxes() {
        assertEquals(320f, boardWidthForViewport(widthDp = 320f, heightDp = 600f))
        assertEquals(244f, boardWidthForViewport(widthDp = 600f, heightDp = 260f))
        assertEquals(680f, boardWidthForViewport(widthDp = 900f, heightDp = 900f))
    }

    @Test
    fun denseControlsAreSelectedForShortScreens() {
        assertTrue(useDenseBoardControls(DenseBoardControlsHeightDp - 1f))
        assertFalse(useDenseBoardControls(DenseBoardControlsHeightDp))
    }

    @Test
    fun inputActionsStackOnlyBelowCompactBreakpoint() {
        assertTrue(stackInputActions(StackedInputBreakpointDp - 1f))
        assertFalse(stackInputActions(StackedInputBreakpointDp))
    }
}
