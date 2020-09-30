// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.daycare.CareType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LocationControllerIT : AbstractIntegrationTest() {

    @Autowired
    lateinit var controller: LocationController

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
}
