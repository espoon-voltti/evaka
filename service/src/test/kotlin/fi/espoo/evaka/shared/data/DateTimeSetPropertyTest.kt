// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.dateTimeSet
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.helsinkiDateTimeRange
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.positiveInt
import java.time.Duration

class DateTimeSetPropertyTest :
    RangeBasedSetPropertyTest<HelsinkiDateTime, HelsinkiDateTimeRange, DateTimeSet>() {

    override fun emptySet(): DateTimeSet = DateTimeSet.empty()

    override fun arbitrarySet(): Arb<DateTimeSet> = Arb.dateTimeSet()

    override fun arbitraryRange(duration: Arb<Int>): Arb<HelsinkiDateTimeRange> =
        Arb.helsinkiDateTimeRange(duration = duration.map { Duration.ofNanos(it.toLong() * 1000L) })

    override fun defaultDuration(): Arb<Int> = Arb.positiveInt(max = 48 * 60 * 60)
}
