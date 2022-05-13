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
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationAccessControlTest : AccessControlTest() {
    private lateinit var creatorCitizen: AuthenticatedUser.Citizen
    private lateinit var childId: ChildId
    private lateinit var applicationId: ApplicationId
    private lateinit var daycareId: DaycareId

    @BeforeEach
    private fun beforeEach() {
        creatorCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        db.transaction { tx ->
            childId = tx.insertTestPerson(DevPerson())
            applicationId = tx.insertTestApplication(guardianId = creatorCitizen.id, childId = childId, type = ApplicationType.DAYCARE)
            val areaId = tx.insertTestCareArea(DevCareArea())
            daycareId = tx.insertTestDaycare(DevDaycare(areaId = areaId))
            tx.insertTestApplicationForm(
                applicationId,
                DaycareFormV0(
                    child = Child(
                        dateOfBirth = LocalDate.of(2019, 1, 1)
                    ),
                    Adult(),
                    apply = Apply(
                        preferredUnits = listOf(daycareId)
                    ),
                    type = ApplicationType.DAYCARE
                )
            )
        }
    }

    @Test
    fun `IsCitizen ownerOfApplication`() {
        val action = Action.Application.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).ownerOfApplication())
        val otherCitizen = createTestCitizen(CitizenAuthLevel.STRONG)

        assertTrue(accessControl.hasPermissionFor(creatorCitizen, action, applicationId))
        assertFalse(accessControl.hasPermissionFor(otherCitizen, action, applicationId))
    }

    @Test
    fun `HasUnitRole inPreferredUnitOfApplication`() {
        val action = Action.Application.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPreferredUnitOfApplication())
        val unitSupervisor = createTestEmployee(globalRoles = emptySet(), unitRoles = mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        val otherEmployee = createTestEmployee(globalRoles = emptySet(), unitRoles = mapOf(daycareId to UserRole.STAFF))
        assertTrue(accessControl.hasPermissionFor(unitSupervisor, action, applicationId))
        assertFalse(accessControl.hasPermissionFor(otherEmployee, action, applicationId))
    }

    @Test
    fun `HasUnitRole inPlacementPlanUnitOfApplication`() {
        val action = Action.Application.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication())
        val daycareId = db.transaction { tx ->
            tx.execute("UPDATE application SET status = 'ACTIVE' WHERE id = ?", applicationId)
            tx.insertTestPlacementPlan(applicationId, daycareId)
            daycareId
        }
        val unitSupervisor = createTestEmployee(globalRoles = emptySet(), unitRoles = mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        val otherEmployee = createTestEmployee(globalRoles = emptySet(), unitRoles = mapOf(daycareId to UserRole.STAFF))
        assertTrue(accessControl.hasPermissionFor(unitSupervisor, action, applicationId))
        assertFalse(accessControl.hasPermissionFor(otherEmployee, action, applicationId))
    }
}
