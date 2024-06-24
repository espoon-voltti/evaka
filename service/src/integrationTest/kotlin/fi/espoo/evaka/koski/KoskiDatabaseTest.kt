// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.placement.PlacementType
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

// We don't have a separate enum for this and reuse the raw Koski HTTP API type
typealias KoskiStudyRightType = OpiskeluoikeudenTyyppiKoodi

class KoskiDatabaseTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `all placement types relevant to Koski can be cast to a Koski study right type`() {
        PlacementType.entries
            .filter { it.isRelevantToKoski() }
            .forEach { placementType ->
                assertNotNull(
                    db.read { tx ->
                        tx
                            .createQuery {
                                sql(
                                    """
SELECT ${bind(placementType)}::koski_study_right_type
"""
                                )
                            }.exactlyOne<KoskiStudyRightType>()
                    }
                )
            }
    }
}
