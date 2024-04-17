// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AclIntegrationTest : PureJdbiTest(resetDbBeforeEach = false) {
    private lateinit var employeeId: EmployeeId
    private lateinit var daycareId: DaycareId
    private lateinit var groupId: GroupId
    private lateinit var childId: ChildId
    private lateinit var applicationId: ApplicationId
    private lateinit var decisionId: DecisionId
    private lateinit var placementId: PlacementId
    private lateinit var mobileId: MobileDeviceId
    private lateinit var fridgeParentId: PersonId
    private lateinit var guardianId: PersonId

    private lateinit var accessControl: AccessControl

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeAll
    fun before() {
        db.transaction {
            employeeId = it.insert(DevEmployee())
            val areaId = it.insert(DevCareArea(name = "Test area"))
            daycareId = it.insert(DevDaycare(areaId = areaId))
            groupId = it.insert(DevDaycareGroup(daycareId = daycareId))
            guardianId = it.insert(DevPerson(), DevPersonType.RAW_ROW)
            childId = it.insert(DevPerson(), DevPersonType.CHILD)
            fridgeParentId = it.insert(DevPerson(), DevPersonType.RAW_ROW)
            it.insert(
                DevFridgeChild(
                    childId = childId,
                    headOfChild = fridgeParentId,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2030, 1, 1)
                )
            )
            it.insertGuardian(guardianId, childId)
            applicationId =
                it.insertTestApplication(
                    childId = childId,
                    guardianId = guardianId,
                    type = ApplicationType.DAYCARE
                )
            decisionId =
                it.insertTestDecision(
                    TestDecision(
                        createdBy = EvakaUserId(employeeId.raw),
                        unitId = daycareId,
                        applicationId = applicationId,
                        type = DecisionType.DAYCARE,
                        startDate = LocalDate.of(2019, 1, 1),
                        endDate = LocalDate.of(2100, 1, 1)
                    )
                )
            placementId =
                it.insertTestPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2100, 1, 1)
                )
            mobileId = it.insert(DevMobileDevice(unitId = daycareId))
        }
        accessControl = AccessControl(DefaultActionRuleMapping(), noopTracer)
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.execute { sql("TRUNCATE daycare_acl") } }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["ADMIN", "SERVICE_WORKER", "FINANCE_ADMIN"])
    fun testGlobalRoleAuthorization(role: UserRole) {
        val user = AuthenticatedUser.Employee(employeeId, setOf(role))

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, fridgeParentId)
            )
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, guardianId)
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["UNIT_SUPERVISOR", "STAFF"])
    fun testAclRoleAuthorization(role: UserRole) {
        val user = AuthenticatedUser.Employee(employeeId, setOf(role))

        db.read { tx ->
            assertFalse(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, fridgeParentId)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, guardianId)
            )
        }

        db.transaction { it.insertDaycareAclRow(daycareId, employeeId, role) }

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, fridgeParentId)
            )
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, guardianId)
            )
        }
    }
}
