package pet.project.sudokusolver.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponsiveLayoutTest {
    @Test
    fun boardLayoutSwitchesAtWideBreakpoint() {
        assertFalse(useWideBoardLayout(WideBoardBreakpointDp - 1f))
        assertTrue(useWideBoardLayout(WideBoardBreakpointDp))
    }

    @Test
    fun inputActionsStackOnlyBelowCompactBreakpoint() {
        assertTrue(stackInputActions(StackedInputBreakpointDp - 1f))
        assertFalse(stackInputActions(StackedInputBreakpointDp))
    }
}
