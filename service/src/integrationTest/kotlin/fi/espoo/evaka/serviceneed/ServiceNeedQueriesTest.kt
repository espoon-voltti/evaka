// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.snPreparatoryDaycare50
import fi.espoo.evaka.snPreschoolDaycare45
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServiceNeedQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun getServiceNeedOptionPublicInfos() {
        assertThat(
            db.read { tx ->
                tx.getServiceNeedOptionPublicInfos(PlacementType.values().toList())
            }
        ).isEmpty()
    }

    @Test
    fun getOnlyShownServiceNeedOptionPublicInfos() {
        db.transaction { tx -> tx.insertServiceNeedOptions() }
        db.transaction { tx -> tx.updateShowForCitizen() }
        val queriedOptions =
            db.read { tx -> tx.getServiceNeedOptionPublicInfos(PlacementType.values().toList()) }
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
