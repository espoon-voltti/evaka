// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed

import evaka.core.PureJdbiTest
import evaka.core.insertServiceNeedOptions
import evaka.core.placement.PlacementType
import evaka.core.shared.db.Database
import evaka.core.snPreparatoryDaycare50
import evaka.core.snPreschoolDaycare45
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServiceNeedQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun getServiceNeedOptionPublicInfos() {
        assertThat(db.read { tx -> tx.getServiceNeedOptionPublicInfos(PlacementType.entries) })
            .isEmpty()
    }

    @Test
    fun getOnlyShownServiceNeedOptionPublicInfos() {
        db.transaction { tx -> tx.insertServiceNeedOptions() }
        db.transaction { tx -> tx.updateShowForCitizen() }
        val queriedOptions = db.read { tx ->
            tx.getServiceNeedOptionPublicInfos(PlacementType.entries)
        }
        // I couln't get any of the fancy asssertj list assertions to work in Kotlin, so test the
        // stupid way
        queriedOptions.forEach {
            assertThat(it.id).isNotEqualTo(snPreschoolDaycare45.id)
            assertThat(it.id).isNotEqualTo(snPreparatoryDaycare50.id)
        }
    }

    fun Database.Transaction.updateShowForCitizen() {
        execute {
            sql(
                """
UPDATE service_need_option SET show_for_citizen=FALSE
WHERE id=${bind(snPreschoolDaycare45.id)} OR id=${bind(snPreparatoryDaycare50.id)}
"""
            )
        }
    }
}
