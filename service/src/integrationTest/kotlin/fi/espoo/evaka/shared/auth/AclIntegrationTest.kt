// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDecision
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
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val employee = DevEmployee()
    private val guardian = DevPerson()
    private val child = DevPerson()
    private val fridgeParent = DevPerson()
    private val mobile = DevMobileDevice(unitId = daycare.id)

    private lateinit var accessControl: AccessControl

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeAll
    fun before() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(guardian, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(fridgeParent, DevPersonType.RAW_ROW)
            tx.insert(
                DevFridgeChild(
                    childId = child.id,
                    headOfChild = fridgeParent.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2030, 1, 1),
                )
            )
            tx.insertGuardian(guardian.id, child.id)
            val applicationId =
                tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                        ),
                )
            tx.insertTestDecision(
                TestDecision(
                    createdBy = employee.evakaUserId,
                    unitId = daycare.id,
                    applicationId = applicationId,
                    type = DecisionType.DAYCARE,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2100, 1, 1),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2100, 1, 1),
                )
            )
            tx.insert(mobile)
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
        val user = AuthenticatedUser.Employee(employee.id, setOf(role))

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, fridgeParent.id)
            )
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, guardian.id)
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["UNIT_SUPERVISOR", "STAFF"])
    fun testAclRoleAuthorization(role: UserRole) {
        val user = AuthenticatedUser.Employee(employee.id, setOf(role))

        db.read { tx ->
            assertFalse(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, fridgeParent.id)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, guardian.id)
            )
        }

        db.transaction { it.insertDaycareAclRow(daycare.id, employee.id, role) }

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, fridgeParent.id)
            )
            assertTrue(
                accessControl.hasPermissionFor(tx, user, clock, Action.Person.READ, guardian.id)
            )
        }
    }
}
