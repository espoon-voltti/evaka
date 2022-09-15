// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.children.Group
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestReservation
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testChild_8
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_DATE

internal class AttendanceReservationReportByChildTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin =
        AuthenticatedUser.Employee(unitSupervisorOfTestDaycare.id, setOf(UserRole.ADMIN))

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `returns only unit's operation days`() {
        val startDate = LocalDate.of(2022, 9, 1) // Thu
        val endDate = LocalDate.of(2022, 9, 6) // Tue
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = startDate,
                endDate = endDate
            )
            FiniteDateRange(startDate, endDate).dates().forEach { date ->
                tx.insertTestReservation(
                    DevReservation(
                        childId = testChild_1.id,
                        date = date,
                        startTime = LocalTime.of(8, 15),
                        endTime = LocalTime.of(15, 48),
                        createdBy = admin.evakaUserId
                    )
                )
            }
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to startDate.format(ISO_DATE), "end" to endDate.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.childId }, { it.reservationDate })
            .containsExactlyInAnyOrder(
                Tuple(testChild_1.id, LocalDate.of(2022, 9, 1)), // Thu
                Tuple(testChild_1.id, LocalDate.of(2022, 9, 2)), // Fri
                Tuple(testChild_1.id, LocalDate.of(2022, 9, 5)), // Mon
                Tuple(testChild_1.id, LocalDate.of(2022, 9, 6)), // Tue
            )
    }

    @Test
    fun `end date is inclusive`() {
        val date = LocalDate.of(2022, 9, 2)
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.childId }, { it.reservationDate })
            .containsExactlyInAnyOrder(
                Tuple(testChild_1.id, LocalDate.of(2022, 9, 2)),
            )
    }

    @Test
    fun `child without placement is not included`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).isEmpty()
    }

    @Test
    fun `child with placement to a different unit is not included`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare2.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).isEmpty()
    }

    @Test
    fun `backup care is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val groupId = tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    daycareId = testDaycare2.id,
                    startDate = date,
                    endDate = date,
                )
            )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = date,
                    endDate = date
                ),
                groupId = groupId,
                startDate = date,
                endDate = date
            )
            tx.insertTestBackUpCare(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
            tx.insertTestPlacement(
                childId = testChild_2.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_2.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.childId }, { it.reservationDate }, { it.isBackupCare })
            .containsExactlyInAnyOrder(
                Tuple(testChild_1.id, date, true),
                Tuple(testChild_2.id, date, false),
            )
    }

    @Test
    fun `multiple children is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            listOf(
                testChild_1,
                testChild_2,
                testChild_3,
                testChild_4,
                testChild_5,
                testChild_6,
                testChild_7,
                testChild_8
            )
                .forEach { testChild ->
                    tx.insertTestPlacement(
                        childId = testChild.id,
                        unitId = testDaycare.id,
                        startDate = date,
                        endDate = date
                    )
                    tx.insertTestReservation(
                        DevReservation(
                            childId = testChild.id,
                            date = date,
                            startTime = LocalTime.of(8, 15),
                            endTime = LocalTime.of(8, 16),
                            createdBy = admin.evakaUserId
                        )
                    )
                }
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.childLastName }, { it.childFirstName }, { it.reservationDate })
            .containsExactlyInAnyOrder(
                Tuple(testChild_1.lastName, testChild_1.firstName, date),
                Tuple(testChild_2.lastName, testChild_2.firstName, date),
                Tuple(testChild_3.lastName, testChild_3.firstName, date),
                Tuple(testChild_4.lastName, testChild_4.firstName, date),
                Tuple(testChild_5.lastName, testChild_5.firstName, date),
                Tuple(testChild_6.lastName, testChild_6.firstName, date),
                Tuple(testChild_7.lastName, testChild_7.firstName, date),
                Tuple(testChild_8.lastName, testChild_8.firstName, date),
            )
    }

    @Test
    fun `group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        val group1 = db.transaction { tx ->
            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    daycareId = testDaycare.id,
                    name = "Testiläiset 1"
                )
            ).let { Group(it, "Testiläiset 1") }
        }
        val group2 = db.transaction { tx ->
            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    daycareId = testDaycare.id,
                    name = "Testiläiset 2"
                )
            ).let { Group(it, "Testiläiset 2") }
        }
        db.transaction { tx ->
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                ),
                groupId = group1.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = tx.insertTestPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                ),
                groupId = group1.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_2.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = tx.insertTestPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = date,
                    endDate = date
                ),
                groupId = group2.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_3.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
            tx.insertTestPlacement(
                childId = testChild_4.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_4.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf(
                "start" to date.format(ISO_DATE),
                "end" to date.format(ISO_DATE),
                "groupIds" to "${group1.id},${group2.id}"
            )
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.groupId }, { it.groupName }, { it.childId })
            .containsExactlyInAnyOrder(
                Tuple(group1.id, group1.name, testChild_1.id),
                Tuple(group1.id, group1.name, testChild_2.id),
                Tuple(group2.id, group2.name, testChild_3.id),
            )
    }

    @Test
    fun `group placement without group ids filter works`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId = tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            val groupId = tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    daycareId = testDaycare.id,
                    startDate = date,
                    endDate = date
                )
            )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementId,
                groupId = groupId,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.groupId }, { it.childId })
            .containsExactlyInAnyOrder(
                Tuple(null, testChild_1.id),
            )
    }

    @Test
    fun `empty group ids works`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = date,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}/by-child",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE), "groupIds" to "")
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportByChildRow>>(jsonMapper)

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get())
            .extracting({ it.groupId }, { it.childId })
            .containsExactlyInAnyOrder(
                Tuple(null, testChild_1.id),
            )
    }
}
