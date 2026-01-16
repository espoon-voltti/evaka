// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import kotlin.test.Test
import kotlin.test.assertTrue

class StrictRateLimiterTest {
    @Test
    fun `enforces minimum interval between acquires`() {
        val rateLimiter = StrictRateLimiter(10.0)
        val timestamps = mutableListOf<Long>()

        timestamps.add(System.currentTimeMillis())
        rateLimiter.acquire()
        timestamps.add(System.currentTimeMillis())
        rateLimiter.acquire()
        timestamps.add(System.currentTimeMillis())
        rateLimiter.acquire()
        timestamps.add(System.currentTimeMillis())

        val interval1 = timestamps[1] - timestamps[0]
        val interval2 = timestamps[2] - timestamps[1]
        val interval3 = timestamps[3] - timestamps[2]

        assertTrue(interval1 < 10, "First acquire should be immediate")
        assertTrue(interval2 >= 100, "Second acquire should wait at least 100ms")
        assertTrue(interval3 >= 100, "Third acquire should wait at least 100ms")
    }
}
