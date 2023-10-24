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
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import fi.espoo.evaka.shared.security.actionrule.IsMobile
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChildAccessControlTest : AccessControlTest() {
    private lateinit var childId: ChildId

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeEach
    fun beforeEach() {
        childId =
            db.transaction { tx ->
                tx.insert(DevPerson(), DevPersonType.RAW_ROW).also { tx.insert(DevChild(id = it)) }
            }
    }

    @Test
    fun `IsCitizen guardianOfChild`() {
        val action = Action.Child.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).guardianOfChild())
        val guardianCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        db.transaction { tx -> tx.insertGuardian(guardianCitizen.id, childId) }
        val otherCitizen = createTestCitizen(CitizenAuthLevel.STRONG)

        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, guardianCitizen, clock, action, childId))
            assertFalse(accessControl.hasPermissionFor(tx, otherCitizen, clock, action, childId))
            assertFalse(
                accessControl.hasPermissionFor(
                    tx,
                    guardianCitizen.copy(authLevel = CitizenAuthLevel.WEAK),
                    clock,
                    action,
                    childId
                )
            )
        }
    }

    @Test
    fun `HasUnitRole inPlacementUnitOfChild`() {
        val action = Action.Child.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementUnitOfChild())
        val daycareId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val daycareId = tx.insert(DevDaycare(areaId = areaId))
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        endDate = LocalDate.of(2100, 1, 1)
                    )
                )
                daycareId
            }
        val unitSupervisor =
            createTestEmployee(emptySet(), mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, childId))
        }

        val staff = createTestEmployee(emptySet(), mapOf(daycareId to UserRole.STAFF))
        db.read { tx ->
            assertFalse(accessControl.hasPermissionFor(tx, staff, clock, action, childId))
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
                        childId = childId,
                        unitId = daycareId,
                        endDate = LocalDate.of(2100, 1, 1)
                    )
                )
                Pair(daycareId, otherDaycareId)
            }
        val unitMobile = createTestMobile(daycareId)
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, unitMobile, clock, action, childId))
        }

        val otherMobile = createTestMobile(otherDaycareId)
        db.read { tx ->
            assertFalse(accessControl.hasPermissionFor(tx, otherMobile, clock, action, childId))
        }
    }
}
