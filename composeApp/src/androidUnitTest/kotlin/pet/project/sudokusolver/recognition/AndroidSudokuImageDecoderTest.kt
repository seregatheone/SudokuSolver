package pet.project.sudokusolver.recognition

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidSudokuImageDecoderTest {
    @Test
    fun leavesAlreadyBoundedImageAtFullResolution() {
        assertEquals(
            1,
            calculateImageSampleSize(
                width = 1920,
                height = 1080,
                maxDimension = 4096,
                maxPixels = 8_388_608,
            ),
        )
    }

    @Test
    fun usesPowerOfTwoSampleForLargeImage() {
        assertEquals(
            4,
            calculateImageSampleSize(
                width = 12_000,
                height = 9_000,
                maxDimension = 4096,
                maxPixels = 8_388_608,
            ),
        )
    }

    @Test
    fun pixelBudgetCanRequireMoreSamplingThanDimensionBudget() {
        assertEquals(
            2,
            calculateImageSampleSize(
                width = 4000,
                height = 4000,
                maxDimension = 4096,
                maxPixels = 8_388_608,
            ),
        )
    }
}
