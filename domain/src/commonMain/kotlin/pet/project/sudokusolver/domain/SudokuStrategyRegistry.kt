package pet.project.sudokusolver.domain

internal object SudokuStrategyRegistry {
    val executablePatterns = listOf(
        SudokuSolvingPattern.NakedSingle,
        SudokuSolvingPattern.HiddenSingle,
        SudokuSolvingPattern.LockedCandidatesPointing,
        SudokuSolvingPattern.ClaimingBoxLineReduction,
        SudokuSolvingPattern.NakedPair,
        SudokuSolvingPattern.NakedTriple,
        SudokuSolvingPattern.NakedQuad,
        SudokuSolvingPattern.HiddenPair,
        SudokuSolvingPattern.HiddenTriple,
        SudokuSolvingPattern.HiddenQuad,
        SudokuSolvingPattern.XWing,
        SudokuSolvingPattern.Swordfish,
        SudokuSolvingPattern.Jellyfish,
        SudokuSolvingPattern.XYWing,
        SudokuSolvingPattern.XYZWing,
        SudokuSolvingPattern.WWing,
        SudokuSolvingPattern.Skyscraper,
        SudokuSolvingPattern.TwoStringKite,
        SudokuSolvingPattern.EmptyRectangle,
        SudokuSolvingPattern.UniqueRectangle,
        SudokuSolvingPattern.SimpleColoring,
        SudokuSolvingPattern.MultiColoring,
        SudokuSolvingPattern.RemotePair,
        SudokuSolvingPattern.XChain,
        SudokuSolvingPattern.XYChain,
        SudokuSolvingPattern.AlternatingInferenceChain,
        SudokuSolvingPattern.ForcingChain,
        SudokuSolvingPattern.NiceLoop,
        SudokuSolvingPattern.ContinuousLoop,
        SudokuSolvingPattern.DiscontinuousLoop,
        SudokuSolvingPattern.GroupedAic,
        SudokuSolvingPattern.AlsXz,
        SudokuSolvingPattern.AlsXyWing,
        SudokuSolvingPattern.DeathBlossom,
        SudokuSolvingPattern.FinnedXWing,
        SudokuSolvingPattern.SashimiXWing,
        SudokuSolvingPattern.FinnedSwordfish,
        SudokuSolvingPattern.KrakenFish,
        SudokuSolvingPattern.SueDeCoq,
        SudokuSolvingPattern.Exocet,
        SudokuSolvingPattern.ThreeDMedusa,
        SudokuSolvingPattern.BowmansBingo,
        SudokuSolvingPattern.Nishio,
    )

    private val manualOnlyPatterns = setOf(
        SudokuSolvingPattern.ForcingChain,
        SudokuSolvingPattern.NiceLoop,
        SudokuSolvingPattern.BowmansBingo,
        SudokuSolvingPattern.Nishio,
    )

    // Assumption searches remain available through hintForPattern(), but are too expensive for every hint pass.
    val automaticSearchOrder = executablePatterns.filterNot { pattern -> pattern in manualOnlyPatterns }

    private val strategiesByPattern: Map<SudokuSolvingPattern, SudokuStrategy> = listOf(
        NakedSingleStrategy,
        HiddenSingleStrategy,
        LockedCandidatesPointingStrategy,
        ClaimingBoxLineReductionStrategy,
        NakedSubsetStrategy(size = 2, pattern = SudokuSolvingPattern.NakedPair),
        NakedSubsetStrategy(size = 3, pattern = SudokuSolvingPattern.NakedTriple),
        NakedSubsetStrategy(size = 4, pattern = SudokuSolvingPattern.NakedQuad),
        HiddenSubsetStrategy(size = 2, pattern = SudokuSolvingPattern.HiddenPair),
        HiddenSubsetStrategy(size = 3, pattern = SudokuSolvingPattern.HiddenTriple),
        HiddenSubsetStrategy(size = 4, pattern = SudokuSolvingPattern.HiddenQuad),
        FishStrategy(size = 2, pattern = SudokuSolvingPattern.XWing),
        FishStrategy(size = 3, pattern = SudokuSolvingPattern.Swordfish),
        FishStrategy(size = 4, pattern = SudokuSolvingPattern.Jellyfish),
        XYWingStrategy,
        XYZWingStrategy,
        WWingStrategy,
        SkyscraperStrategy,
        TwoStringKiteStrategy,
        EmptyRectangleStrategy,
        XChainStrategy,
        XYChainStrategy,
        AlternatingInferenceChainStrategy,
        ForcingChainStrategy,
        NiceLoopStrategy,
        ContinuousLoopStrategy,
        DiscontinuousLoopStrategy,
        GroupedAicStrategy,
        AlsXzStrategy,
        AlsXyWingStrategy,
        DeathBlossomStrategy,
        UniqueRectangleStrategy,
        SimpleColoringStrategy,
        MultiColoringStrategy,
        RemotePairStrategy,
        SueDeCoqStrategy,
        ExocetStrategy,
        FinnedFishStrategy(size = 2, pattern = SudokuSolvingPattern.FinnedXWing),
        FinnedFishStrategy(size = 2, pattern = SudokuSolvingPattern.SashimiXWing, requireSashimi = true),
        FinnedFishStrategy(size = 3, pattern = SudokuSolvingPattern.FinnedSwordfish),
        KrakenFishStrategy,
        ThreeDMedusaStrategy,
        BowmansBingoStrategy,
        NishioStrategy,
    ).associateBy { it.pattern }

    init {
        require(executablePatterns.size == executablePatterns.distinct().size)
        require(executablePatterns.toSet() == strategiesByPattern.keys)
        require(automaticSearchOrder.all { pattern -> pattern in executablePatterns })
    }

    fun strategyFor(pattern: SudokuSolvingPattern): SudokuStrategy? = strategiesByPattern[pattern]

    fun findNextStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (pattern in automaticSearchOrder) {
            strategyFor(pattern)?.findStep(state)?.let { return it }
        }
        return null
    }
}
