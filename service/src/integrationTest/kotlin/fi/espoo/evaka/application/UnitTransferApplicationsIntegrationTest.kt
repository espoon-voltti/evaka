// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class UnitTransferApplicationsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationController: ApplicationControllerV2

    private lateinit var admin: AuthenticatedUser.Employee
    private val area = DevCareArea()
    private val placementUnit1 = DevDaycare(areaId = area.id)
    private val applicationUnit1 = DevDaycare(areaId = area.id)

    @BeforeEach
    fun setup() {
        admin =
            db.transaction { tx ->
                DevEmployee(roles = setOf(UserRole.ADMIN)).also { tx.insert(it) }.user
            }
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(placementUnit1)
            tx.insert(applicationUnit1)
        }
    }

    @Nested
    inner class SimpleDataTest {
        private lateinit var expected: List<TransferApplicationUnitSummary>

        @BeforeEach
        fun insertTestData() {
            val guardian =
                db.transaction { tx -> DevPerson().also { tx.insert(it, DevPersonType.ADULT) } }
            val child =
                db.transaction { tx -> DevPerson().also { tx.insert(it, DevPersonType.CHILD) } }
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = placementUnit1.id,
                        startDate = LocalDate.of(2020, 1, 1),
                        endDate = LocalDate.of(2025, 12, 31),
                    )
                )
            }
            val preferredStartDate = LocalDate.of(2021, 1, 1)
            val applicationId =
                db.transaction { tx ->
                    tx.insertTestApplication(
                        type = ApplicationType.DAYCARE,
                        status = ApplicationStatus.SENT,
                        guardianId = guardian.id,
                        childId = child.id,
                        transferApplication = true,
                        document =
                            DaycareFormV0(
                                type = ApplicationType.DAYCARE,
                                child = child.toDaycareFormChild(),
                                guardian = guardian.toDaycareFormAdult(restricted = false),
                                apply = Apply(preferredUnits = listOf(applicationUnit1.id)),
                                preferredStartDate = preferredStartDate,
                            ),
                    )
                }
            expected =
                listOf(
                    TransferApplicationUnitSummary(
                        applicationId = applicationId,
                        firstName = child.firstName,
                        lastName = child.lastName,
                        dateOfBirth = child.dateOfBirth,
                        preferredStartDate = preferredStartDate,
                    )
                )
        }

        @Test
        fun `getUnitApplications returns transfer applications`() {
            assertEquals(expected, getUnitApplications().transferApplications)
        }

        @Test
        fun `getUnitApplications returns null transfer applications for unit supervisor`() {
            val unitSupervisor =
                db.transaction { tx ->
                    val employee = DevEmployee()
                    tx.insert(
                        employee,
                        unitRoles = mapOf(placementUnit1.id to UserRole.UNIT_SUPERVISOR),
                    )
                    employee.user
                }
            assertEquals(null, getUnitApplications(user = unitSupervisor).transferApplications)
        }

        @Test
        fun `getUnitApplications throws forbidden for staff`() {
            val staff =
                db.transaction { tx ->
                    val employee = DevEmployee()
                    tx.insert(employee, unitRoles = mapOf(placementUnit1.id to UserRole.STAFF))
                    employee.user
                }
            assertThrows<Forbidden> { getUnitApplications(user = staff) }
        }

        @Test
        fun `getUnitApplications returns empty transfer applications before placement`() {
            val clock = MockEvakaClock(2019, 12, 31, 17, 47)
            assertEquals(emptyList(), getUnitApplications(clock = clock).transferApplications)
        }

        @Test
        fun `getUnitApplications returns transfer applications on placement start`() {
            val clock = MockEvakaClock(2020, 1, 1, 17, 47)
            assertEquals(expected, getUnitApplications(clock = clock).transferApplications)
        }

        @Test
        fun `getUnitApplications returns transfer applications on placement end`() {
            val clock = MockEvakaClock(2025, 12, 31, 17, 47)
            assertEquals(expected, getUnitApplications(clock = clock).transferApplications)
        }

        @Test
        fun `getUnitApplications returns empty transfer applications after placement`() {
            val clock = MockEvakaClock(2026, 1, 1, 17, 47)
            assertEquals(emptyList(), getUnitApplications(clock = clock).transferApplications)
        }

        @Test
        fun `getUnitApplications returns empty transfer applications for unit in application`() {
            assertEquals(
                emptyList(),
                getUnitApplications(unitId = applicationUnit1.id).transferApplications,
            )
        }

        @Test
        fun `getUnitApplications returns empty transfer applications for another placement unit`() {
            val placementUnit2Id = db.transaction { tx -> tx.insert(DevDaycare(areaId = area.id)) }
            assertEquals(
                emptyList(),
                getUnitApplications(unitId = placementUnit2Id).transferApplications,
            )
        }
    }

    private fun getUnitApplications(
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2020, 8, 10), LocalTime.of(8, 0))),
        unitId: DaycareId = placementUnit1.id,
    ) = applicationController.getUnitApplications(dbInstance(), user, clock, unitId)
}
