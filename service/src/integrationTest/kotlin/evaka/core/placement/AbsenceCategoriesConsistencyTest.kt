// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.placement

import evaka.core.PureJdbiTest
import evaka.core.absence.AbsenceCategory
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AbsenceCategoriesConsistencyTest : PureJdbiTest(resetDbBeforeEach = false) {
    @Test
    fun `SQL absence_categories matches PlacementType absenceCategories for every placement type`() {
        val fromDatabase = db.read { tx ->
            PlacementType.entries.associateWith { placementType ->
                tx.createQuery { sql("SELECT absence_categories(${bind(placementType)})") }
                    .exactlyOne<Set<AbsenceCategory>>()
            }
        }
        val fromKotlin = PlacementType.entries.associateWith { it.absenceCategories() }

        assertEquals(fromKotlin, fromDatabase)
    }
}
