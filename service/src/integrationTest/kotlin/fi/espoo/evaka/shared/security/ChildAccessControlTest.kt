// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import fi.espoo.evaka.shared.security.actionrule.IsMobile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChildAccessControlTest : AccessControlTest() {
    private lateinit var childId: ChildId

    @BeforeEach
    private fun beforeEach() {
        childId = db.transaction { tx ->
            tx.insertTestPerson(DevPerson()).also { tx.insertTestChild(DevChild(id = it)) }
        }
    }

    @Test
    fun `IsCitizen guardianOfChild`() {
        val action = Action.Child.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).guardianOfChild())
        val guardianCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        db.transaction { tx ->
            tx.insertGuardian(guardianCitizen.id, childId)
        }
        val otherCitizen = createTestCitizen(CitizenAuthLevel.STRONG)

        assertTrue(accessControl.hasPermissionFor(guardianCitizen, action, childId))
        assertFalse(accessControl.hasPermissionFor(otherCitizen, action, childId))
        assertFalse(accessControl.hasPermissionFor(guardianCitizen.copy(authLevel = CitizenAuthLevel.WEAK), action, childId))
    }

    @Test
    fun `HasUnitRole inPlacementUnitOfChild`() {
        val action = Action.Child.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementUnitOfChild())
        val daycareId = db.transaction { tx ->
            val areaId = tx.insertTestCareArea(DevCareArea())
            val daycareId = tx.insertTestDaycare(DevDaycare(areaId = areaId))
            tx.insertTestPlacement(DevPlacement(childId = childId, unitId = daycareId, endDate = LocalDate.of(2100, 1, 1)))
            daycareId
        }
        val unitSupervisor = createTestEmployee(emptySet(), mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        assertTrue(accessControl.hasPermissionFor(unitSupervisor, action, childId))

        val staff = createTestEmployee(emptySet(), mapOf(daycareId to UserRole.STAFF))
        assertFalse(accessControl.hasPermissionFor(staff, action, childId))
    }

    @Test
    fun `IsMobile inPlacementUnitOfChild`() {
        val action = Action.Child.READ
        rules.add(action, IsMobile(requirePinLogin = false).inPlacementUnitOfChild())
        val (daycareId, otherDaycareId) = db.transaction { tx ->
            val areaId = tx.insertTestCareArea(DevCareArea())
            val daycareId = tx.insertTestDaycare(DevDaycare(areaId = areaId))
            val otherDaycareId = tx.insertTestDaycare(DevDaycare(areaId = areaId))
            tx.insertTestPlacement(DevPlacement(childId = childId, unitId = daycareId, endDate = LocalDate.of(2100, 1, 1)))
            Pair(daycareId, otherDaycareId)
        }
        val unitMobile = createTestMobile(daycareId)
        assertTrue(accessControl.hasPermissionFor(unitMobile, action, childId))

        val otherMobile = createTestMobile(otherDaycareId)
        assertFalse(accessControl.hasPermissionFor(otherMobile, action, childId))
    }
}
