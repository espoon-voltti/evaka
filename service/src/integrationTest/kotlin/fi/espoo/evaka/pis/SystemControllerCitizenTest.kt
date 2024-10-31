// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SystemControllerCitizenTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var systemController: SystemController

    private val user = AuthenticatedUser.SystemInternalUser
    private val clock = MockEvakaClock(2024, 10, 15, 10, 0)
    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val child = DevPerson()
    private val adult = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(area)
            it.insert(daycare)
            it.insert(child, DevPersonType.CHILD)
            it.insert(adult, DevPersonType.ADULT)
            it.insert(DevGuardian(adult.id, child.id))
        }
    }

    @Test
    fun `citizen has full message access when child active placement exists`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().minusDays(1),
                    endDate = clock.today().plusDays(1),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, adult.id)
        assertTrue(result.accessibleFeatures.messages)
        assertTrue(result.accessibleFeatures.composeNewMessage)
    }

    @Test
    fun `citizen can read but not send messages when child placement exists in the past`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().minusMonths(2),
                    endDate = clock.today().minusDays(1),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, adult.id)
        assertTrue(result.accessibleFeatures.messages)
        assertFalse(result.accessibleFeatures.composeNewMessage)
    }

    @Test
    fun `citizen can read but not send messages when child placement exists over 2 weeks in the future`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().plusWeeks(2).plusDays(1),
                    endDate = clock.today().plusMonths(2),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, adult.id)
        assertTrue(result.accessibleFeatures.messages)
        assertFalse(result.accessibleFeatures.composeNewMessage)
    }

    @Test
    fun `citizen has full message access when child placement exists less than 2 weeks in the future`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().plusWeeks(2),
                    endDate = clock.today().plusMonths(2),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, adult.id)
        assertTrue(result.accessibleFeatures.messages)
        assertTrue(result.accessibleFeatures.composeNewMessage)
    }
}
