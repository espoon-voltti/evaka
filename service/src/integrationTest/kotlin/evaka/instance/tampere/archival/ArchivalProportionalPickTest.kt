// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import evaka.trevaka.archival.distributeProportionally
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ArchivalProportionalPickTest {

    @Test
    fun `a low deviation should receive fewest slots`() {
        val totalLimit = 2000
        val distribution = distributeProportionally(listOf(1000, 5, 1000, 1000, 1000), 2000)
        assert(distribution.sum() <= totalLimit)
        assertEquals(distribution.min(), distribution[1])
    }

    @Test
    fun `a high deviation should receive most slots`() {
        val totalLimit = 2000
        val distribution = distributeProportionally(listOf(1000, 5000, 1000, 1000, 1000), 2000)
        assert(distribution.sum() <= totalLimit)
        assertEquals(distribution.max(), distribution[1])
    }

    @Test
    fun `all slots go to the only present category`() {
        val totalLimit = 2000
        val categories = listOf(0, 1000, 0, 0, 0)
        val distribution = distributeProportionally(categories, totalLimit)
        assert(distribution.sum() <= totalLimit)
        assertEquals(categories[1], distribution[1])
    }

    @Test
    fun `realistic category distribution is exhausted in reasonable time`() {
        val maxRounds = 100
        val totalLimit = 2000
        var rounds = 0
        val batches = mutableListOf<Int>()

        var categories = listOf(16000, 6, 30000, 45000, 40000)

        while (categories.sum() > 0) {
            rounds++
            if (rounds > maxRounds) fail("Max limit of steps reached")
            val distribution = distributeProportionally(categories, totalLimit)
            categories = categories.zip(distribution) { a, b -> a - b }
            batches.add(distribution.sum())
        }

        assertEquals(0, categories.sum())
        assert(batches.max() <= totalLimit)
        assert(batches.min() > 0)
        assert(batches.average() > (0.95 * totalLimit))
    }
}
