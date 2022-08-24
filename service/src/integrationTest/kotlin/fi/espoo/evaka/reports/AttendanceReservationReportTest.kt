// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.LocalTimeRange
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestReservation
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.snDaycareContractDays10
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_DATE

internal class AttendanceReservationReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin =
        AuthenticatedUser.Employee(unitSupervisorOfTestDaycare.id, setOf(UserRole.ADMIN))

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `returns only unit's operation days`() {
        val startDate = LocalDate.of(2022, 8, 8) // Mon
        val endDate = LocalDate.of(2022, 8, 14) // Sun

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to startDate.format(ISO_DATE), "end" to endDate.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(startDate, endDate.minusDays(2)) // Mon-Fri
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `end date is inclusive`() {
        val date = LocalDate.of(2022, 8, 10)

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date)
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
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
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date)
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
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
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date)
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `service need factor is picked from placement's default option`() {
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
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 30)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                )
            )
        }
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `service need factor is picked from placement's service need`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            val placementId = tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestServiceNeed(
                confirmedBy = admin.evakaUserId,
                placementId = placementId,
                period = FiniteDateRange(date, date),
                optionId = snDaycareContractDays10.id
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
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 0.88,
                    staffCountRequired = 0.1
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 30)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 0.88,
                    staffCountRequired = 0.1
                )
            )
        }
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `age is calculated from attendance reservation date`() {
        val startDate = LocalDate.of(2020, 5, 29) // Fri
        val endDate = LocalDate.of(2020, 6, 1) // Mon & 3rd birthday
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = startDate,
                endDate = endDate
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
            tx.insertTestReservation(
                DevReservation(
                    childId = testChild_1.id,
                    date = endDate,
                    startTime = LocalTime.of(8, 15),
                    endTime = LocalTime.of(8, 16),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to startDate.format(ISO_DATE), "end" to endDate.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(startDate, endDate).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 29), LocalTime.of(8, 0)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 29), LocalTime.of(8, 30)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 6, 1), LocalTime.of(8, 0)),
                    childCountUnder3 = 0,
                    childCountOver3 = 1,
                    childCount = 1,
                    capacityFactor = 1.0,
                    staffCountRequired = 0.1
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 6, 1), LocalTime.of(8, 30)),
                    childCountUnder3 = 0,
                    childCountOver3 = 1,
                    childCount = 1,
                    capacityFactor = 1.0,
                    staffCountRequired = 0.1
                )
            )
        }
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `assistance need factor is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = date,
                endDate = date
            )
            tx.insertTestAssistanceNeed(
                DevAssistanceNeed(
                    childId = testChild_1.id,
                    updatedBy = admin.evakaUserId,
                    startDate = date,
                    endDate = date,
                    capacityFactor = 5.0
                )
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
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 8.75,
                    staffCountRequired = 1.3
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 30)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 8.75,
                    staffCountRequired = 1.3
                )
            )
        }

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `backup care is supported`() {
        val date = LocalDate.of(2020, 5, 28)
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare2.id,
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
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 30)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                )
            )
        }

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `end time is supported in first 30 minutes`() {
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
                    endTime = LocalTime.of(15, 22),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val changedRows = LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(15, 30), Duration.ofMinutes(30)).map {
            AttendanceReservationReportRow(
                HelsinkiDateTime.of(
                    LocalDate.of(2020, 5, 28),
                    it,
                ),
                childCountUnder3 = 1,
                childCountOver3 = 0,
                childCount = 1,
                capacityFactor = 1.75,
                staffCountRequired = 0.3
            )
        }
        val expected = createEmptyReport(date, date).also {
            addExpectedRow(it, *(changedRows).toTypedArray())
        }
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `end time is supported in last 30 minutes`() {
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
                    endTime = LocalTime.of(15, 48),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val changedRows = LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0), Duration.ofMinutes(30)).map {
            AttendanceReservationReportRow(
                HelsinkiDateTime.of(
                    LocalDate.of(2020, 5, 28),
                    it,
                ),
                childCountUnder3 = 1,
                childCountOver3 = 0,
                childCount = 1,
                capacityFactor = 1.75,
                staffCountRequired = 0.3
            )
        }
        val expected = createEmptyReport(date, date).also {
            addExpectedRow(it, *(changedRows).toTypedArray())
        }
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }

    @Test
    fun `times are inclusive`() {
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
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 30),
                    createdBy = admin.evakaUserId
                )
            )
        }

        val (_, res, result) = http.get(
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 30)),
                    childCountUnder3 = 1,
                    childCountOver3 = 0,
                    childCount = 1,
                    capacityFactor = 1.75,
                    staffCountRequired = 0.3
                )
            )
        }
        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
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
            "/reports/attendance-reservation/${testDaycare.id}",
            listOf("start" to date.format(ISO_DATE), "end" to date.format(ISO_DATE))
        )
            .asUser(admin)
            .responseObject<List<AttendanceReservationReportRow>>(jsonMapper)

        val expected = createEmptyReport(date, date).also {
            addExpectedRow(
                it,
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 0)),
                    childCountUnder3 = 6,
                    childCountOver3 = 2,
                    childCount = 8,
                    capacityFactor = 12.5,
                    staffCountRequired = 1.8
                ),
                AttendanceReservationReportRow(
                    HelsinkiDateTime.of(LocalDate.of(2020, 5, 28), LocalTime.of(8, 30)),
                    childCountUnder3 = 6,
                    childCountOver3 = 2,
                    childCount = 8,
                    capacityFactor = 12.5,
                    staffCountRequired = 1.8
                )
            )
        }

        assertThat(res.statusCode).isEqualTo(200)
        assertThat(result.get()).containsExactlyElementsOf(expected.values)
    }
}

private fun createEmptyReport(
    start: LocalDate,
    end: LocalDate,
    operationDays: Set<DayOfWeek> = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
): MutableMap<HelsinkiDateTime, AttendanceReservationReportRow> {
    val startDateTime = HelsinkiDateTime.of(start, LocalTime.MIN)
    val endDateTime = HelsinkiDateTime.of(end, LocalTime.MAX)
    val times = mutableMapOf<HelsinkiDateTime, AttendanceReservationReportRow>()
    var time = startDateTime
    while (time < endDateTime) {
        if (operationDays.contains(time.dayOfWeek)) {
            times[time] = AttendanceReservationReportRow(time, 0, 0, 0, 0.0, 0.0)
        }
        time = time.plusMinutes(30)
    }
    return times
}

private fun addExpectedRow(
    map: MutableMap<HelsinkiDateTime, AttendanceReservationReportRow>,
    vararg rows: AttendanceReservationReportRow
) {
    rows.forEach { row -> map[row.dateTime] = row }
}
