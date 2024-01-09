// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.getAbsencesOfChildByRange
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import io.opentracing.noop.NoopTracerFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CreateReservationsAndAbsencesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val monday = LocalDate.of(2021, 8, 23)
    private val mondayNoon = HelsinkiDateTime.of(monday, LocalTime.NOON)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val startTime = LocalTime.of(9, 0)
    private val endTime: LocalTime = LocalTime.of(17, 0)
    private val queryRange = FiniteDateRange(monday.minusDays(10), monday.plusDays(10))

    private val citizenUser = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
    private val employeeUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf())

    private val citizenReservationThresholdHours: Long = 150

    @BeforeEach
    fun before() {
        db.transaction { it.insertGeneralTestFixtures() }
    }

    @Test
    fun `adding two reservations works in a basic case`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then 2 reservations are added
        val reservations =
            db.read { it.getReservationsCitizen(monday, testAdult_1.id, queryRange) }
                .flatMap {
                    it.children.mapNotNull { child ->
                        child.reservations.takeIf { it.isNotEmpty() }
                    }
                }
        assertEquals(2, reservations.size)
    }

    @Test
    fun `reservation is not added outside placement nor for placements that don't require reservations`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL,
                startDate = monday,
                endDate = monday
            )
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = tuesday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        // Only tuesday has a placement that requires reservations
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = wednesday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(tuesday, reservations.first())
    }

    @Test
    fun `reservation is not added if user is not guardian of the child`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_2.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then no reservation are added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(0, reservations.size)
    }

    @Test
    fun `reservation is not added outside operating days`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday.minusDays(1),
                endDate = monday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday.minusDays(1),
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `reservation is not added on holiday`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestHoliday(tuesday)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `absences are removed from days with reservation`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tuesday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())

        // and 1st absence has been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday)) }
        assertEquals(1, absences.size)
        assertEquals(tuesday, absences.first().date)
    }

    @Test
    fun `absences and reservations are removed from empty days`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = tuesday,
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = EvakaUserId(testAdult_1.id.raw)
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = monday,
                    ),
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = tuesday,
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then no reservations exist
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .flatMap { dailyData -> dailyData.children.flatMap { it.reservations } }
        assertEquals(listOf(), reservations)

        // and no absences exist
        val absences =
            db.read { it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday)) }
        assertEquals(listOf(), absences)
    }

    @Test
    fun `correct absence types are created`() {
        // given
        val unlockedDate = monday.plusDays(15)
        db.transaction { tx ->
            // monday: no service need
            // tuesday: contract days
            tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = monday,
                    endDate = unlockedDate
                )
                .let { placementId ->
                    tx.insertTestServiceNeed(
                        placementId = placementId,
                        period = FiniteDateRange(tuesday, unlockedDate),
                        optionId = snDaycareContractDays15.id,
                        confirmedBy = employeeUser.evakaUserId,
                    )
                }
            tx.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Absent(
                        childId = testChild_1.id,
                        date = monday,
                    ),
                    DailyReservationRequest.Absent(
                        childId = testChild_1.id,
                        date = tuesday,
                    ),
                    DailyReservationRequest.Absent(
                        childId = testChild_1.id,
                        date = unlockedDate,
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then 3 absences with correct types are added
        val absences =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange.copy(end = unlockedDate.plusDays(2))
                    )
                }
                .flatMap { day -> day.children.map { child -> day.date to child.absence } }

        assertEquals(
            listOf(
                monday to AbsenceType.OTHER_ABSENCE,
                tuesday to AbsenceType.OTHER_ABSENCE,
                unlockedDate to AbsenceType.PLANNED_ABSENCE
            ),
            absences
        )
    }

    @Test
    fun `free absences are not overwritten`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tuesday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.FREE_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = wednesday,
                category = AbsenceCategory.BILLABLE,
                absenceType = AbsenceType.FREE_ABSENCE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = wednesday,
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then 1 reservation is added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(monday, reservations.first())
        assertEquals(1, reservations.size)

        // and 1st absence has been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, wednesday)) }
        assertEquals(listOf(tuesday, wednesday), absences.map { it.date })
        assertEquals(
            listOf(AbsenceType.FREE_ABSENCE, AbsenceType.FREE_ABSENCE),
            absences.map { it.absenceType }
        )
    }

    @Test
    fun `irregular daily service times absences are not overwritten`() {
        val dailyServiceTimesController =
            DailyServiceTimesController(
                AccessControl(EspooActionRuleMapping(), NoopTracerFactory.create())
            )
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = wednesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertDaycareAclRow(testDaycare.id, testDecisionMaker_1.id, UserRole.UNIT_SUPERVISOR)
        }

        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf()),
            MockEvakaClock(HelsinkiDateTime.of(monday.minusDays(1), LocalTime.of(12, 0))),
            testChild_1.id,
            DailyServiceTimesValue.IrregularTimes(
                validityPeriod = DateRange(monday, null),
                // absences are generated for null days
                monday = null,
                tuesday = null,
                wednesday = null,
                thursday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                friday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                saturday = null,
                sunday = null,
            )
        )

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Absent(
                        childId = testChild_1.id,
                        date = tuesday,
                    ),
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = wednesday,
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 3 non-editable absences are left
        val absences =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child ->
                        Triple(dailyData.date, child.absence, child.absenceEditable)
                    }
                }
        assertEquals(
            listOf(
                Triple(monday, AbsenceType.OTHER_ABSENCE, false),
                Triple(tuesday, AbsenceType.OTHER_ABSENCE, false),
                Triple(wednesday, AbsenceType.OTHER_ABSENCE, false),
            ),
            absences
        )
    }

    @Test
    fun `previous reservation is overwritten`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(LocalTime.of(12, 0), endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then 1 reservation is changed
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child -> dailyData.date to child.reservations }
                }
        assertEquals(2, reservations.size)
        assertEquals(monday, reservations[0].first)
        assertEquals(
            LocalTime.of(12, 0),
            (reservations[0].second[0] as Reservation.Times).startTime
        )
        assertEquals(tuesday, reservations[1].first)
        assertEquals(LocalTime.of(9, 0), (reservations[1].second[0] as Reservation.Times).startTime)
    }

    @Test
    fun `reservations without times can be added if reservations are not required (removes absences)`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL, // <-- reservations are not required
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(testAdult_1.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Present(
                        childId = testChild_1.id,
                        date = monday,
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val data =
            db.read {
                it.getReservationsCitizen(monday, testAdult_1.id, FiniteDateRange(monday, monday))
            }
        val reservations =
            data.flatMap { dailyData -> dailyData.children.flatMap { child -> child.reservations } }
        val absences =
            data.flatMap { dailyData -> dailyData.children.map { child -> child.absence } }
        assertEquals(0, reservations.size)
        assertEquals(0, absences.size)
    }

    @Test
    fun `reservations without times can be added to open holiday period`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Present(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val dailyReservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        holidayPeriod,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child -> dailyData.date to child.reservations }
                }
        assertEquals(1, dailyReservations.size)
        dailyReservations.first().let { (date, reservations) ->
            assertEquals(holidayPeriodStart, date)
            assertEquals(listOf(Reservation.NoTimes), reservations)
        }
    }

    @Test
    fun `reservations without times cannot be added to closed holiday period or outside holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday.minusDays(1))
        }

        // when
        assertThrows<BadRequest> {
            // Closed holiday period
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    mondayNoon,
                    citizenUser,
                    listOf(
                        DailyReservationRequest.Present(
                            childId = testChild_1.id,
                            date = holidayPeriodStart,
                        )
                    ),
                    citizenReservationThresholdHours
                )
            }
        }

        assertThrows<BadRequest> {
            // Outside holiday periods
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    mondayNoon,
                    citizenUser,
                    listOf(
                        DailyReservationRequest.Present(
                            childId = testChild_1.id,
                            date = holidayPeriodEnd.plusDays(1),
                        )
                    ),
                    citizenReservationThresholdHours
                )
            }
        }
    }

    @Test
    fun `citizen cannot override days with absences or without reservations in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insertAbsences(
                citizenUser.evakaUserId,
                listOf(
                    AbsenceInsert(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    ),
                    AbsenceInsert(
                        childId = testChild_1.id,
                        date = holidayPeriodStart.plusDays(1),
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = holidayPeriodStart.plusDays(1),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart.plusDays(2),
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val reservationData =
            db.read {
                it.getReservationsCitizen(
                    monday,
                    testAdult_1.id,
                    holidayPeriod,
                )
            }
        val allReservations =
            reservationData.flatMap { dailyData ->
                dailyData.children.flatMap { child -> child.reservations }
            }
        val absenceDates =
            reservationData.flatMap { dailyData ->
                if (dailyData.children.any { child -> child.absence != null })
                    listOf(dailyData.date)
                else emptyList()
            }
        assertEquals(0, allReservations.size)
        assertEquals(listOf(holidayPeriodStart, holidayPeriodStart.plusDays(1)), absenceDates)
    }

    @Test
    fun `citizen cannot override absences in closed holiday periods when reservations are not required`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL, // <-- reservations not required
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insertAbsences(
                citizenUser.evakaUserId,
                listOf(
                    AbsenceInsert(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Present(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val reservationData =
            db.read {
                it.getReservationsCitizen(
                    monday,
                    testAdult_1.id,
                    holidayPeriod,
                )
            }
        val allReservations =
            reservationData.flatMap { dailyData ->
                dailyData.children.flatMap { child -> child.reservations }
            }
        val absenceDates =
            reservationData.flatMap { dailyData ->
                if (dailyData.children.any { child -> child.absence != null })
                    listOf(dailyData.date)
                else emptyList()
            }
        assertEquals(0, allReservations.size)
        assertEquals(listOf(holidayPeriodStart), absenceDates)
    }

    @Test
    fun `citizen cannot remove reservations without times in closed holiday periods when reservations are required`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = holidayPeriodStart,
                    startTime = null,
                    endTime = null,
                    createdBy = citizenUser.evakaUserId,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Nothing(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val reservationData =
            db.read {
                it.getReservationsCitizen(
                    monday,
                    testAdult_1.id,
                    holidayPeriod,
                )
            }
        val allReservations =
            reservationData.flatMap { dailyData ->
                dailyData.children.flatMap { child ->
                    child.reservations.map { child.childId to it }
                }
            }

        assertEquals(listOf(testChild_1.id to Reservation.NoTimes), allReservations)
    }

    @Test
    fun `employee can override absences in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insertAbsences(
                citizenUser.evakaUserId,
                listOf(
                    AbsenceInsert(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                employeeUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val reservationData =
            db.read {
                it.getReservationsCitizen(
                    monday,
                    testAdult_1.id,
                    holidayPeriod,
                )
            }
        val allReservations =
            reservationData.flatMap { dailyData ->
                dailyData.children.map { child -> dailyData.date to child.reservations }
            }
        val absenceDates =
            reservationData.flatMap { dailyData ->
                if (dailyData.children.any { child -> child.absence != null })
                    listOf(dailyData.date)
                else emptyList()
            }
        assertEquals(1, allReservations.size)
        allReservations.first().let { (date, reservations) ->
            assertEquals(holidayPeriodStart, date)
            assertEquals(listOf(Reservation.Times(startTime, endTime)), reservations)
        }
        assertEquals(0, absenceDates.size)
    }

    @Test
    fun `reservations can be overridden in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = monday.plusYears(1)
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertHolidayPeriod(holidayPeriod, monday.minusDays(1))
            it.insert(
                // NoTimes reservation
                DevReservation(
                    childId = testChild_1.id,
                    date = holidayPeriodStart,
                    startTime = null,
                    endTime = null,
                    createdBy = citizenUser.evakaUserId
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                employeeUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = holidayPeriodStart,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours
            )
        }

        // then
        val allReservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        holidayPeriod,
                    )
                }
                .flatMap { dailyData ->
                    dailyData.children.map { child -> dailyData.date to child.reservations }
                }
        assertEquals(1, allReservations.size)
        allReservations.first().let { (date, reservations) ->
            assertEquals(holidayPeriodStart, date)
            assertEquals(listOf(Reservation.Times(startTime, endTime)), reservations)
        }
    }

    @Test
    fun `citizen cannot override employee created absences with reservations`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(employeeUser.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tuesday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(employeeUser.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                citizenUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then reservation is not added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(0, reservations.size)

        // and absence has not been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday)) }
        assertEquals(2, absences.size)
    }

    @Test
    fun `employee can override employee created absences with reservations`() {
        // given
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = monday,
                endDate = tuesday
            )
            it.insertGuardian(guardianId = testAdult_1.id, childId = testChild_1.id)
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = monday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(employeeUser.id.raw)
            )
            it.insertTestAbsence(
                childId = testChild_1.id,
                date = tuesday,
                category = AbsenceCategory.BILLABLE,
                modifiedBy = EvakaUserId(employeeUser.id.raw)
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                mondayNoon,
                employeeUser,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = testChild_1.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then reservations are added
        val reservations =
            db.read {
                    it.getReservationsCitizen(
                        monday,
                        testAdult_1.id,
                        queryRange,
                    )
                }
                .mapNotNull { dailyData ->
                    dailyData.date.takeIf {
                        dailyData.children.any { it.reservations.isNotEmpty() }
                    }
                }
        assertEquals(2, reservations.size)

        // and absences have been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(testChild_1.id, DateRange(monday, tuesday)) }
        assertEquals(0, absences.size)
    }
}
