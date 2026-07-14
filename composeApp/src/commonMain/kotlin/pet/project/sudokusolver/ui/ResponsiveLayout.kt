package pet.project.sudokusolver.ui

internal const val WideBoardBreakpointDp = 840f
internal const val StackedInputBreakpointDp = 520f

internal fun useWideBoardLayout(widthDp: Float): Boolean = widthDp >= WideBoardBreakpointDp

internal fun stackInputActions(widthDp: Float): Boolean = widthDp < StackedInputBreakpointDp
