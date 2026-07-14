package pet.project.sudokusolver.ui

internal const val WideBoardBreakpointDp = 840f
internal const val LandscapeBoardBreakpointDp = 540f
internal const val ShortBoardHeightDp = 600f
internal const val DenseBoardControlsHeightDp = 700f
internal const val StackedInputBreakpointDp = 520f
private const val BoardLabelHeightReserveDp = 16f
private const val MaximumBoardWidthDp = 680f

internal fun useWideBoardLayout(widthDp: Float, heightDp: Float): Boolean =
    widthDp >= WideBoardBreakpointDp ||
        (widthDp >= LandscapeBoardBreakpointDp && heightDp < ShortBoardHeightDp)

internal fun useDenseBoardControls(heightDp: Float): Boolean = heightDp < DenseBoardControlsHeightDp

internal fun boardWidthForViewport(widthDp: Float, heightDp: Float): Float = minOf(
    MaximumBoardWidthDp,
    widthDp,
    (heightDp - BoardLabelHeightReserveDp).coerceAtLeast(0f),
)

internal fun stackInputActions(widthDp: Float): Boolean = widthDp < StackedInputBreakpointDp
