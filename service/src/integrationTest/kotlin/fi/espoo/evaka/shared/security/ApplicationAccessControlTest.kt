// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationAccessControlTest : AccessControlTest() {
    private lateinit var creatorCitizen: AuthenticatedUser.Citizen
    private lateinit var childId: ChildId
    private lateinit var applicationId: ApplicationId
    private lateinit var daycareId: DaycareId

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeEach
    fun beforeEach() {
        creatorCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        db.transaction { tx ->
            childId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val areaId = tx.insert(DevCareArea())
            daycareId = tx.insert(DevDaycare(areaId = areaId))
            applicationId =
                tx.insertTestApplication(
                    guardianId = creatorCitizen.id,
                    childId = childId,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = LocalDate.of(2019, 1, 1)),
                            Adult(),
                            apply = Apply(preferredUnits = listOf(daycareId)),
                        ),
                )
        }
    }

    @Test
    fun `IsCitizen ownerOfApplication`() {
        val action = Action.Application.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).ownerOfApplication())
        val otherCitizen = createTestCitizen(CitizenAuthLevel.STRONG)

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, creatorCitizen, clock, action, applicationId)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, otherCitizen, clock, action, applicationId)
            )
        }
    }

    @Test
    fun `HasUnitRole inPreferredUnitOfApplication`() {
        val action = Action.Application.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPreferredUnitOfApplication())
        val unitSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.UNIT_SUPERVISOR),
            )
        val otherEmployee =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.STAFF),
            )
        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, applicationId)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, otherEmployee, clock, action, applicationId)
            )
        }
    }

    @Test
    fun `HasUnitRole inPlacementPlanUnitOfApplication`() {
        val action = Action.Application.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication())
        val daycareId =
            db.transaction { tx ->
                tx.execute {
                    sql(
                        "UPDATE application SET status = 'ACTIVE' WHERE id = ${bind(applicationId)}"
                    )
                }
                tx.insertTestPlacementPlan(applicationId, daycareId)
                daycareId
            }
        val unitSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.UNIT_SUPERVISOR),
            )
        val otherEmployee =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(daycareId to UserRole.STAFF),
            )
        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, applicationId)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, otherEmployee, clock, action, applicationId)
            )
        }
    }
}
