// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LocationControllerIT : FullApplicationTest() {
    @Autowired
    lateinit var controller: LocationController

    @BeforeEach
    fun beforeEach() {
        val legacyDataSql = this.javaClass.getResource("/legacy_db_data.sql").readText()
        jdbi.handle { h ->
            resetDatabase(h)
            h.execute(legacyDataSql)
        }
    }

    @Test
    fun `enduser only sees locations that can be applied to`() {
        val response = controller.getEnduserUnitsByArea()

        with(response.body!!) {
            assertTrue(
                this.all {
                    it.daycares.all {
                        it.canApplyClub || it.canApplyDaycare || it.canApplyPreschool
                    }
                }
            )
        }
    }

    @Test
    fun `clubs are included in results although they do not offer daycare nor preschool`() {
        val response = controller.getEnduserUnitsByArea()

        with(response.body!!) {
            assertTrue(this.any { it.daycares.any { it.type.contains(CareType.CLUB) } })
        }
    }

    @Test
    fun `areas endpoint works without login`() {
        val (_, response, _) = http.get("/enduser/areas").responseString()
        assertEquals(200, response.statusCode)
    }
}
