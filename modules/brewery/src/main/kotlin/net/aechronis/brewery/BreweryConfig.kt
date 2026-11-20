package net.aechronis.brewery

/** Wall-clock durations in milliseconds. [TEST] makes every timer take seconds for in-game checks. */
data class BreweryConfig(
    val growthStageMs: Long = 20 * 60_000L,
    val fermentMs: Long = 60 * 60_000L,
    val agingStepMs: Long = 24 * 60 * 60_000L,
    val nearbyRange: Double = 6.0,
) {
    companion object {
        val TEST = BreweryConfig(growthStageMs = 5_000L, fermentMs = 10_000L, agingStepMs = 10_000L)

        @Volatile
        var current = BreweryConfig()
    }
}
