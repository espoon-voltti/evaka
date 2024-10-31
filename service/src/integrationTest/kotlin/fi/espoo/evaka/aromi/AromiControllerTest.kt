// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

class AromiControllerTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var aromiController: AromiController

    private lateinit var admin: AuthenticatedUser.Employee
    private lateinit var today: LocalDate
    private lateinit var clock: EvakaClock

    private lateinit var unitId: DaycareId
    private lateinit var groupId1: GroupId
    private lateinit var groupId2: GroupId

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insertServiceNeedOption(snDefaultDaycare) }
        admin =
            db.transaction { tx ->
                val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
                tx.insert(employee)
                employee.user
            }
        today = LocalDate.of(2024, 10, 24)
        clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 18)))

        unitId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(DevDaycare(areaId = areaId, name = "Päiväkoti A"))
            }
        groupId1 =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = unitId, name = "Ryhmä 1"))
            }
        groupId2 =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = unitId, name = "Ryhmä 2"))
            }

        db.transaction { tx ->
            val dateOfBirth = LocalDate.of(2021, 7, 2)
            val placementEnd = LocalDate.of(2027, 7, 31)
            tx.insert(
                    DevPerson(
                        lastName = "Hämäläinen",
                        firstName = "Harri",
                        ssn = null,
                        dateOfBirth = dateOfBirth,
                        id = PersonId(UUID.fromString("f800197e-336c-4e98-ac2c-da305e3d605b")),
                    ),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    val placementId =
                        tx.insert(
                            DevPlacement(
                                childId = childId,
                                unitId = unitId,
                                startDate = dateOfBirth,
                                endDate = placementEnd,
                            )
                        )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = groupId1,
                            startDate = dateOfBirth,
                            endDate = LocalDate.of(2024, 10, 1),
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = groupId2,
                            startDate = LocalDate.of(2024, 10, 2),
                            endDate = placementEnd,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 10, 1),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 10, 2),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 10, 3),
                            absenceCategory = AbsenceCategory.BILLABLE,
                        )
                    )
                }
        }
    }

    @Test
    fun `ok admin`() {
        assertEquals(
            getExpectedCsv("aromi/ok-admin.csv"),
            getMealOrders(
                FiniteDateRange(LocalDate.of(2024, 9, 30), LocalDate.of(2024, 10, 4)),
                listOf(groupId1, groupId2),
            ),
        )
    }

    @Test
    fun `forbidden director`() {
        val director =
            db.transaction { tx ->
                val employee = DevEmployee(roles = setOf(UserRole.DIRECTOR))
                tx.insert(employee)
                employee.user
            }

        assertThrows<Forbidden> {
            getMealOrders(director, FiniteDateRange.ofYear(2024), listOf(groupId1, groupId2))
        }
    }

    @Test
    fun `forbidden staff`() {
        val staff =
            db.transaction { tx ->
                val employee = DevEmployee()
                tx.insert(
                    employee,
                    unitRoles = mapOf(unitId to UserRole.STAFF),
                    groupAcl = mapOf(unitId to listOf(groupId1, groupId2)),
                )
                employee.user
            }

        assertThrows<Forbidden> {
            getMealOrders(staff, FiniteDateRange.ofYear(2024), listOf(groupId1, groupId2))
        }
    }

    @Test
    fun `bad request group ids empty`() {
        assertThrows<BadRequest> { getMealOrders(FiniteDateRange.ofYear(2024), emptyList()) }
    }

    @Test
    fun `bad request group ids null`() {
        assertThrows<BadRequest> { getMealOrders(FiniteDateRange.ofYear(2024), null) }
    }

    private fun getMealOrders(range: FiniteDateRange, groupIds: List<GroupId>?): String =
        getMealOrders(admin, range, groupIds)

    private fun getMealOrders(
        user: AuthenticatedUser.Employee,
        range: FiniteDateRange,
        groupIds: List<GroupId>?,
    ): String =
        aromiController
            .getMealOrders(dbInstance(), user, clock, range.start, range.end, groupIds)
            .toString(StandardCharsets.ISO_8859_1)

    private fun getExpectedCsv(path: String): String =
        ClassPathResource(path).getContentAsString(StandardCharsets.ISO_8859_1)
}
