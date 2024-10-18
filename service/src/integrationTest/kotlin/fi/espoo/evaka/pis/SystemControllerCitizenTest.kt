// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.*
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SystemControllerCitizenTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var systemController: SystemController

    val user = AuthenticatedUser.SystemInternalUser
    val clock = MockEvakaClock(2024, 10, 15, 10, 0)
    val guardian = DevGuardian(testAdult_1.id, testChild_1.id)

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(testArea)
            it.insert(testDaycare)
            it.insert(testChild_1, DevPersonType.CHILD)
            it.insert(testAdult_1, DevPersonType.ADULT)
            it.insert(guardian)
        }
    }

    @Test
    fun `citizen has full message access when child active placement exists`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().minusDays(1),
                    endDate = clock.today().plusDays(1),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, testAdult_1.id)
        assertTrue(result.accessibleFeatures.messages)
        assertTrue(result.accessibleFeatures.composeNewMessage)
    }

    @Test
    fun `citizen can read but not send messages when child placement exists in the past`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().minusMonths(2),
                    endDate = clock.today().minusDays(1),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, testAdult_1.id)
        assertTrue(result.accessibleFeatures.messages)
        assertFalse(result.accessibleFeatures.composeNewMessage)
    }

    @Test
    fun `citizen can read but not send messages when child placement exists over 2 weeks in the future`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().plusWeeks(2).plusDays(1),
                    endDate = clock.today().plusMonths(2),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, testAdult_1.id)
        assertTrue(result.accessibleFeatures.messages)
        assertFalse(result.accessibleFeatures.composeNewMessage)
    }

    @Test
    fun `citizen has full message access when child placement exists less than 2 weeks in the future`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = clock.today().plusWeeks(2),
                    endDate = clock.today().plusMonths(2),
                )
            )
        }
        val result = systemController.citizenUser(dbInstance(), user, clock, testAdult_1.id)
        assertTrue(result.accessibleFeatures.messages)
        assertTrue(result.accessibleFeatures.composeNewMessage)
    }
}
