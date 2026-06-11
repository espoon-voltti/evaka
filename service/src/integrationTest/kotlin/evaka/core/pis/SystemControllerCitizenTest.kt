// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.FullApplicationTest
import evaka.core.placement.PlacementType
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.*
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate
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
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.MESSAGING, PilotFeature.VASU_AND_PEDADOC),
        )
    private val citizenBasicDaycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.CITIZEN_BASIC_DOCUMENT),
        )
    private val otherDecisionDaycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.OTHER_DECISION))
    private val noDocumentDaycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val child = DevPerson()
    private val adult = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(area)
            it.insert(daycare)
            it.insert(citizenBasicDaycare)
            it.insert(otherDecisionDaycare)
            it.insert(noDocumentDaycare)
            it.insert(child, DevPersonType.CHILD)
            it.insert(adult, DevPersonType.ADULT)
            it.insert(DevGuardian(adult.id, child.id))
        }
    }

    private fun childDocumentationAccess(
        unitId: DaycareId,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Boolean {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unitId,
                    type = PlacementType.DAYCARE,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        return systemController
            .citizenUser(dbInstance(), user, clock, adult.id)
            .accessibleFeatures
            .childDocumentation
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

    @Test
    fun `pedagogical documentation access requires an active placement in a VASU_AND_PEDADOC unit`() {
        assertTrue(
            childDocumentationAccess(
                daycare.id,
                clock.today().minusDays(1),
                clock.today().plusDays(1),
            )
        )
    }

    @Test
    fun `no pedagogical documentation access when VASU_AND_PEDADOC placement is in the past`() {
        assertFalse(
            childDocumentationAccess(
                daycare.id,
                clock.today().minusMonths(2),
                clock.today().minusDays(1),
            )
        )
    }

    @Test
    fun `no pedagogical documentation access when VASU_AND_PEDADOC placement starts in the future`() {
        assertFalse(
            childDocumentationAccess(
                daycare.id,
                clock.today().plusDays(30),
                clock.today().plusMonths(4),
            )
        )
    }

    @Test
    fun `citizen basic documentation access starts the configured days before placement start`() {
        assertTrue(
            childDocumentationAccess(
                citizenBasicDaycare.id,
                clock.today().plusDays(60),
                clock.today().plusMonths(4),
            )
        )
    }

    @Test
    fun `no citizen basic documentation access when placement starts over 60 days in the future`() {
        assertFalse(
            childDocumentationAccess(
                citizenBasicDaycare.id,
                clock.today().plusDays(61),
                clock.today().plusMonths(4),
            )
        )
    }

    @Test
    fun `citizen basic documentation access when placement is active`() {
        assertTrue(
            childDocumentationAccess(
                citizenBasicDaycare.id,
                clock.today().minusDays(1),
                clock.today().plusDays(1),
            )
        )
    }

    @Test
    fun `no citizen basic documentation access when placement has ended`() {
        assertFalse(
            childDocumentationAccess(
                citizenBasicDaycare.id,
                clock.today().minusMonths(2),
                clock.today().minusDays(1),
            )
        )
    }

    @Test
    fun `other decision documentation access requires an active placement in an OTHER_DECISION unit`() {
        assertTrue(
            childDocumentationAccess(
                otherDecisionDaycare.id,
                clock.today().minusDays(1),
                clock.today().plusDays(1),
            )
        )
    }

    @Test
    fun `no other decision documentation access when placement starts in the future`() {
        assertFalse(
            childDocumentationAccess(
                otherDecisionDaycare.id,
                clock.today().plusDays(30),
                clock.today().plusMonths(4),
            )
        )
    }

    @Test
    fun `no documentation access when unit has none of the document pilot features`() {
        assertFalse(
            childDocumentationAccess(
                noDocumentDaycare.id,
                clock.today().minusDays(1),
                clock.today().plusDays(1),
            )
        )
    }
}
