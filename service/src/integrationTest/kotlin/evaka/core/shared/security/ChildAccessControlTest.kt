// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

import evaka.core.pis.service.insertGuardian
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.HasUnitRole
import evaka.core.shared.security.actionrule.IsCitizen
import evaka.core.shared.security.actionrule.IsMobile
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChildAccessControlTest : AccessControlTest() {
    private val child = DevPerson()

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(child, DevPersonType.CHILD) }
    }

    @Test
    fun `IsCitizen guardianOfChild`() {
        val action = Action.Child.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).guardianOfChild())
        val guardianCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        db.transaction { tx -> tx.insertGuardian(guardianCitizen.id, child.id) }
        val otherCitizen = createTestCitizen(CitizenAuthLevel.STRONG)

        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, guardianCitizen, clock, action, child.id))
            assertFalse(accessControl.hasPermissionFor(tx, otherCitizen, clock, action, child.id))
            assertFalse(
                accessControl.hasPermissionFor(
                    tx,
                    guardianCitizen.copy(authLevel = CitizenAuthLevel.WEAK),
                    clock,
                    action,
                    child.id,
                )
            )
        }
    }

    @Test
    fun `HasUnitRole inPlacementUnitOfChild`() {
        val action = Action.Child.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementUnitOfChild())
        val daycareId = db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val daycareId = tx.insert(DevDaycare(areaId = areaId))
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    endDate = LocalDate.of(2100, 1, 1),
                )
            )
            daycareId
        }
        val unitSupervisor =
            createTestEmployee(emptySet(), mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, child.id))
        }

        val staff = createTestEmployee(emptySet(), mapOf(daycareId to UserRole.STAFF))
        db.read { tx ->
            assertFalse(accessControl.hasPermissionFor(tx, staff, clock, action, child.id))
        }
    }

    @Test
    fun `IsMobile inPlacementUnitOfChild`() {
        val action = Action.Child.READ
        rules.add(action, IsMobile(requirePinLogin = false).inPlacementUnitOfChild())
        val (daycareId, otherDaycareId) =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val daycareId = tx.insert(DevDaycare(areaId = areaId))
                val otherDaycareId = tx.insert(DevDaycare(areaId = areaId))
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycareId,
                        endDate = LocalDate.of(2100, 1, 1),
                    )
                )
                Pair(daycareId, otherDaycareId)
            }
        val unitMobile = createTestMobile(daycareId)
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, unitMobile, clock, action, child.id))
        }

        val otherMobile = createTestMobile(otherDaycareId)
        db.read { tx ->
            assertFalse(accessControl.hasPermissionFor(tx, otherMobile, clock, action, child.id))
        }
    }
}
