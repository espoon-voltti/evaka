// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import java.time.Duration
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocalTimeRangeTest {

    @Test
    fun `start is inclusive`() {
        assertThat(LocalTimeRange(LocalTime.of(8, 5), LocalTime.of(8, 5)))
            .containsExactly(LocalTime.of(8, 5))
    }

    @Test
    fun `end is inclusive`() {
        assertThat(LocalTimeRange(LocalTime.of(8, 5), LocalTime.of(8, 6)))
            .containsExactly(LocalTime.of(8, 5), LocalTime.of(8, 6))
    }

    @Test
    fun `works with 30 minutes duration`() {
        assertThat(LocalTimeRange(LocalTime.of(8, 5), LocalTime.of(9, 22), Duration.ofMinutes(30)))
            .containsExactly(
                LocalTime.of(8, 5),
                LocalTime.of(8, 35),
                LocalTime.of(9, 5),
            )
    }
}
