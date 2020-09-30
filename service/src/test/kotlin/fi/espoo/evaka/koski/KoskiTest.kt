// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KoskiTest {
    @Test
    fun `calculateStudyRightRanges works correctly`() {
        //       123456789
        // input --
        //         ---
        //             -
        //              --
        // output
        //       -----
        //             ---
        val output = calculateStudyRightRanges(
            sequenceOf(
                ClosedPeriod(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 2)),
                ClosedPeriod(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 5)),
                ClosedPeriod(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 7)),
                ClosedPeriod(LocalDate.of(2019, 1, 8), LocalDate.of(2019, 1, 9))
            )
        )
        assertEquals(
            listOf(
                ClosedPeriod(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)),
                ClosedPeriod(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 9))
            ),
            output
        )
    }

    @Test
    fun `calculateStudyRightRanges clamps ranges correctly`() {
        //       123456789
        // input --
        //         ---
        //             ---
        // clamp    =====
        // output
        //          --
        //             --
        val output = calculateStudyRightRanges(
            sequenceOf(
                ClosedPeriod(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 2)),
                ClosedPeriod(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 5)),
                ClosedPeriod(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 9))
            ),
            clampRange = ClosedPeriod(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 8))
        )
        assertEquals(
            listOf(
                ClosedPeriod(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5)),
                ClosedPeriod(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 8))
            ),
            output
        )
    }
}
