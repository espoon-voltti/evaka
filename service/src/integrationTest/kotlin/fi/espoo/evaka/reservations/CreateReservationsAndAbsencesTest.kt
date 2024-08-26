// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.FullDayAbsenseUpsert
import fi.espoo.evaka.absence.getAbsencesCitizen
import fi.espoo.evaka.absence.getAbsencesOfChildByRange
import fi.espoo.evaka.absence.upsertFullDayAbsences
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevHolidayPeriod
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertServiceNeedOptions
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.snDaycareHours120
import io.opentracing.noop.NoopTracerFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CreateReservationsAndAbsencesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val monday = LocalDate.of(2021, 8, 23)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)
    private val saturday = monday.plusDays(5)
    private val startTime = LocalTime.of(9, 0)
    private val endTime: LocalTime = LocalTime.of(17, 0)
    private val queryRange = FiniteDateRange(monday.minusDays(10), monday.plusDays(10))

    private val citizenReservationThresholdHours = 150L
    private val beforeThreshold = HelsinkiDateTime.of(monday.minusDays(7), LocalTime.of(12, 0))
    private val afterThreshold = HelsinkiDateTime.of(monday.minusDays(7), LocalTime.of(21, 0))

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
    private val employee = DevEmployee()
    private val adult = DevPerson()
    private val child = DevPerson()

    @BeforeEach
    fun before() {
        db.transaction {
            it.insertServiceNeedOptions()
            it.insert(area)
            it.insert(daycare)
            it.insert(employee, unitRoles = mapOf(daycare.id to UserRole.STAFF))
            it.insert(adult, DevPersonType.ADULT)
            it.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `adding two reservations works in a basic case`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then 2 reservations are added
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(2, reservations.size)
    }

    @Test
    fun `reservation is not added outside placement nor for placements that don't require reservations`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday,
                )
            )
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = tuesday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        // Only tuesday has a placement that requires reservations
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = wednesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.map { it.date }
        assertEquals(1, reservations.size)
        assertEquals(tuesday, reservations.first())
    }

    @Test
    fun `reservation is not added if user is not guardian of the child`() {
        // given
        val child2 = DevPerson()
        db.transaction {
            it.insert(child2, DevPersonType.CHILD)
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child2.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then no reservation are added
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(0, reservations.size)
    }

    @Test
    fun `reservation is not added outside operating days`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday.minusDays(1),
                    endDate = monday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = saturday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.map { it.date }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `reservation is not added on holiday`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(DevHoliday(tuesday, "holiday"))
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then only 1 reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.map { it.date }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())
    }

    @Test
    fun `absences are removed from days with reservation`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = monday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = tuesday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours,
            )
        }

        // then 1 reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.map { it.date }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())

        // and 1st absence has been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
        assertEquals(1, absences.size)
        assertEquals(tuesday, absences.first().date)
    }

    @Test
    fun `reservations are removed from days with absence in unlocked range`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = monday,
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = employee.evakaUserId,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(DailyReservationRequest.Absent(childId = child.id, date = monday)),
                citizenReservationThresholdHours,
            )
        }

        // then reservations have been removed
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(0, reservations.size)

        // and absence has been added
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
        assertEquals(1, absences.size)
        assertEquals(monday, absences.first().date)
    }

    @Test
    fun `reservations are kept on days with absence in confirmed range`() {
        // given
        db.transaction {
            it.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)

            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = monday,
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = employee.evakaUserId,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                afterThreshold,
                employee.user,
                listOf(DailyReservationRequest.Absent(childId = child.id, date = monday)),
                citizenReservationThresholdHours,
            )
        }

        // then reservations have been kept
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.map { it.date }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())

        // and absence has been added
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
        assertEquals(1, absences.size)
        assertEquals(monday, absences.first().date)
    }

    @Test
    fun `reservations can be replaced by employee in confirmed range`() {
        val reservation1 = LocalTime.of(8, 0) to LocalTime.of(12, 0)
        val reservation2 = LocalTime.of(16, 0) to LocalTime.of(19, 0)
        val reservation3 = TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))

        // given
        db.transaction {
            it.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)

            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            listOf(reservation1, reservation2).forEach { (start, end) ->
                it.insert(
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = start,
                        endTime = end,
                        createdBy = employee.evakaUserId,
                    )
                )
            }
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                afterThreshold,
                employee.user,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        reservation = reservation3,
                        secondReservation = null,
                    )
                ),
                citizenReservationThresholdHours,
            )
        }

        // then reservations are replaced
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(1, reservations.size)
        reservations.first().let {
            assertEquals(monday, it.date)
            assertTrue { it.reservation is Reservation.Times }
            assertEquals(reservation3, (it.reservation as Reservation.Times).range)
        }
    }

    @Test
    fun `absences and reservations are removed from empty days`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = monday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = tuesday,
                    startTime = startTime,
                    endTime = endTime,
                    createdBy = EvakaUserId(adult.id.raw),
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Nothing(childId = child.id, date = monday),
                    DailyReservationRequest.Nothing(childId = child.id, date = tuesday),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then no reservations exist
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(listOf(), reservations)

        // and no absences exist
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
        assertEquals(listOf(), absences)
    }

    @Test
    fun `correct absence types are created`() {
        // given
        val unlockedDate = monday.plusDays(15)
        db.transaction { tx ->
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)

            tx.insertServiceNeedOption(snDaycareContractDays15)
            tx.insertServiceNeedOption(snDaycareHours120)

            // monday: no service need
            // tuesday: contract days
            // wednesday: hour based service need
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = unlockedDate.plusDays(1),
                    )
                )
                .let { placementId ->
                    val period = FiniteDateRange(tuesday, unlockedDate)
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period.start,
                            endDate = period.end,
                            optionId = snDaycareContractDays15.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                    val period1 =
                        FiniteDateRange(unlockedDate.plusDays(1), unlockedDate.plusDays(1))
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period1.start,
                            endDate = period1.end,
                            optionId = snDaycareHours120.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                }
            tx.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                afterThreshold,
                employee.user,
                listOf(
                    DailyReservationRequest.Absent(childId = child.id, date = monday),
                    DailyReservationRequest.Absent(childId = child.id, date = tuesday),
                    DailyReservationRequest.Absent(childId = child.id, date = unlockedDate),
                    DailyReservationRequest.Absent(
                        childId = child.id,
                        date = unlockedDate.plusDays(1),
                    ),
                ),
                citizenReservationThresholdHours,
                true,
            )
        }

        // then 3 absences with correct types are added
        val absences =
            db.read {
                    it.getAbsencesCitizen(
                        monday,
                        adult.id,
                        queryRange.copy(end = unlockedDate.plusDays(2)),
                    )
                }
                .map { absence -> absence.date to absence.absenceType }

        assertEquals(
            listOf(
                monday to AbsenceType.OTHER_ABSENCE,
                tuesday to AbsenceType.OTHER_ABSENCE,
                unlockedDate to AbsenceType.PLANNED_ABSENCE,
                unlockedDate.plusDays(1) to AbsenceType.PLANNED_ABSENCE,
            ),
            absences,
        )
    }

    @Test
    fun `free absences are not overwritten`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = monday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = tuesday,
                    absenceType = AbsenceType.FREE_ABSENCE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = wednesday,
                    absenceType = AbsenceType.FREE_ABSENCE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Nothing(childId = child.id, date = wednesday),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then 1 reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.map { it.date }
        assertEquals(1, reservations.size)
        assertEquals(monday, reservations.first())

        // and 1st absence has been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, wednesday)) }
        assertEquals(listOf(tuesday, wednesday), absences.map { it.date })
        assertEquals(
            listOf(AbsenceType.FREE_ABSENCE, AbsenceType.FREE_ABSENCE),
            absences.map { it.absenceType },
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
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = wednesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertDaycareAclRow(daycare.id, employee.id, UserRole.UNIT_SUPERVISOR)
        }

        dailyServiceTimesController.postDailyServiceTimes(
            dbInstance(),
            employee.user,
            MockEvakaClock(HelsinkiDateTime.of(monday.minusDays(1), LocalTime.of(12, 0))),
            child.id,
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
            ),
        )

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Absent(childId = child.id, date = tuesday),
                    DailyReservationRequest.Nothing(childId = child.id, date = wednesday),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then 3 non-editable absences are left
        val absences =
            db.read { it.getAbsencesCitizen(monday, adult.id, queryRange) }
                .map { absence ->
                    Triple(absence.date, absence.absenceType, absence.editableByCitizen())
                }
        assertEquals(
            listOf(
                Triple(monday, AbsenceType.OTHER_ABSENCE, false),
                Triple(tuesday, AbsenceType.OTHER_ABSENCE, false),
                Triple(wednesday, AbsenceType.OTHER_ABSENCE, false),
            ),
            absences,
        )
    }

    @Test
    fun `previous reservation is overwritten`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(LocalTime.of(12, 0), endTime),
                    )
                ),
                citizenReservationThresholdHours,
            )
        }

        // then 1 reservation is changed
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }.sortedBy { it.date }
        assertEquals(2, reservations.size)
        assertEquals(monday, reservations[0].date)
        assertEquals(
            TimeRange(LocalTime.of(12, 0), endTime),
            (reservations[0].reservation as Reservation.Times).range,
        )
        assertEquals(tuesday, reservations[1].date)
        assertEquals(
            TimeRange(startTime, endTime),
            (reservations[1].reservation as Reservation.Times).range,
        )
    }

    @Test
    fun `reservations without times can be added if reservations are not required (removes absences)`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL, // <-- reservations are not required
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = monday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(adult.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(DailyReservationRequest.Present(childId = child.id, date = monday)),
                citizenReservationThresholdHours,
            )
        }

        // then
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, FiniteDateRange(monday, monday)) }
        val absences =
            db.read { it.getAbsencesCitizen(monday, adult.id, FiniteDateRange(monday, monday)) }
        assertEquals(0, reservations.size)
        assertEquals(0, absences.size)
    }

    @Test
    fun `no reservations or absences can be added to holiday period days if reservations are not yet open by citizen or employee`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)
        val reservationsOpenOn = beforeThreshold.toLocalDate().plusDays(1)
        val reservationDeadline = monday

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday.minusYears(1),
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevHolidayPeriod(
                    period = holidayPeriod,
                    reservationsOpenOn = reservationsOpenOn,
                    reservationDeadline = reservationDeadline,
                )
            )
        }

        // when
        listOf(adult.user(CitizenAuthLevel.STRONG), employee.user).forEach { user ->
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    beforeThreshold,
                    user,
                    listOf(
                        DailyReservationRequest.Present(
                            childId = child.id,
                            date = holidayPeriodStart,
                        ),
                        DailyReservationRequest.Absent(
                            childId = child.id,
                            date = holidayPeriodStart.plusDays(1),
                        ),
                    ),
                    citizenReservationThresholdHours,
                )
            }

            // then
            assertEquals(
                0,
                db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }.size,
            )
            assertEquals(0, db.read { it.getAbsencesCitizen(monday, adult.id, holidayPeriod) }.size)
        }
    }

    @Test
    fun `reservations without times can be added to open holiday period`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(holidayPeriod, beforeThreshold.toLocalDate(), monday)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Present(childId = child.id, date = holidayPeriodStart)
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val dailyReservations =
            db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }
        assertEquals(1, dailyReservations.size)
        dailyReservations.first().let {
            assertEquals(holidayPeriodStart, it.date)
            assertEquals(Reservation.NoTimes, it.reservation)
            assertFalse(it.staffCreated)
        }
    }

    @Test
    fun `reservations with times added to open holiday period are changed to reservations without times`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(holidayPeriod, beforeThreshold.toLocalDate(), monday)
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = holidayPeriodStart,
                        reservation = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = holidayPeriodStart.plusDays(1),
                        reservation = TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        secondReservation = TimeRange(LocalTime.of(16, 0), LocalTime.of(19, 0)),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val dailyReservations =
            db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }
        assertEquals(2, dailyReservations.size)
        dailyReservations.first().let {
            assertEquals(holidayPeriodStart, it.date)
            assertEquals(Reservation.NoTimes, it.reservation)
            assertFalse(it.staffCreated)
        }
        dailyReservations.last().let {
            assertEquals(holidayPeriodStart.plusDays(1), it.date)
            assertEquals(Reservation.NoTimes, it.reservation)
            assertFalse(it.staffCreated)
        }
    }

    @Test
    fun `reservations without times cannot be added to closed holiday period or outside holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(
                holidayPeriod,
                beforeThreshold.toLocalDate().minusDays(1),
                beforeThreshold.toLocalDate().minusDays(1),
            )
        }

        // when
        assertThrows<BadRequest> {
            // Closed holiday period
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    beforeThreshold,
                    adult.user(CitizenAuthLevel.STRONG),
                    listOf(
                        DailyReservationRequest.Present(
                            childId = child.id,
                            date = holidayPeriodStart,
                        )
                    ),
                    citizenReservationThresholdHours,
                )
            }
        }

        assertThrows<BadRequest> {
            // Outside holiday periods
            db.transaction {
                createReservationsAndAbsences(
                    it,
                    beforeThreshold,
                    adult.user(CitizenAuthLevel.STRONG),
                    listOf(
                        DailyReservationRequest.Present(
                            childId = child.id,
                            date = holidayPeriodEnd.plusDays(1),
                        )
                    ),
                    citizenReservationThresholdHours,
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
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = beforeThreshold.toLocalDate().minusDays(2),
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(
                holidayPeriod,
                beforeThreshold.toLocalDate().minusDays(1),
                beforeThreshold.toLocalDate().minusDays(1),
            )
            it.upsertFullDayAbsences(
                adult.user(CitizenAuthLevel.STRONG).evakaUserId,
                HelsinkiDateTime.now(),
                listOf(
                    FullDayAbsenseUpsert(
                        childId = child.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    ),
                    FullDayAbsenseUpsert(
                        childId = child.id,
                        date = holidayPeriodStart.plusDays(1),
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    ),
                ),
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = holidayPeriodStart,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Nothing(
                        childId = child.id,
                        date = holidayPeriodStart.plusDays(1),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = holidayPeriodStart.plusDays(2),
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val allReservations = db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }
        val absenceDates =
            db.read { it.getAbsencesCitizen(monday, adult.id, holidayPeriod) }.map { it.date }
        assertEquals(0, allReservations.size)
        assertEquals(listOf(holidayPeriodStart, holidayPeriodStart.plusDays(1)), absenceDates)
    }

    @Test
    fun `citizen cannot override absences in closed holiday periods when reservations are not required`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)
        val reservationsOpenOn = beforeThreshold.toLocalDate().minusDays(1)

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL, // <-- reservations not required
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday.minusYears(1),
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(holidayPeriod, reservationsOpenOn, reservationsOpenOn)
            it.upsertFullDayAbsences(
                adult.user(CitizenAuthLevel.STRONG).evakaUserId,
                HelsinkiDateTime.now(),
                listOf(
                    FullDayAbsenseUpsert(
                        childId = child.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                ),
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Present(childId = child.id, date = holidayPeriodStart)
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val allReservations = db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }
        val absenceDates =
            db.read { it.getAbsencesCitizen(monday, adult.id, holidayPeriod) }.map { it.date }
        assertEquals(0, allReservations.size)
        assertEquals(listOf(holidayPeriodStart), absenceDates)
    }

    @Test
    fun `citizen cannot remove reservations without times in closed holiday periods when reservations are required`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)
        val reservationsOpenOn = beforeThreshold.toLocalDate().minusDays(1)

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday.minusYears(1),
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(holidayPeriod, reservationsOpenOn, reservationsOpenOn)
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = holidayPeriodStart,
                    startTime = null,
                    endTime = null,
                    createdBy = adult.user(CitizenAuthLevel.STRONG).evakaUserId,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Nothing(childId = child.id, date = holidayPeriodStart)
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val allReservations = db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }

        assertEquals(1, allReservations.size)
        allReservations.first().let {
            assertEquals(child.id, it.childId)
            assertEquals(Reservation.NoTimes, it.reservation)
            assertFalse(it.staffCreated)
        }
    }

    @Test
    fun `employee can override absences in closed holiday periods`() {
        val holidayPeriodStart = monday.plusMonths(1)
        val holidayPeriodEnd = holidayPeriodStart.plusWeeks(1).minusDays(1)
        val holidayPeriod = FiniteDateRange(holidayPeriodStart, holidayPeriodEnd)

        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(
                holidayPeriod,
                beforeThreshold.toLocalDate().minusDays(1),
                beforeThreshold.toLocalDate().minusDays(1),
            )
            it.upsertFullDayAbsences(
                adult.user(CitizenAuthLevel.STRONG).evakaUserId,
                HelsinkiDateTime.now(),
                listOf(
                    FullDayAbsenseUpsert(
                        childId = child.id,
                        date = holidayPeriodStart,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                    )
                ),
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                employee.user,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = holidayPeriodStart,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val allReservations = db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }
        val absenceDates =
            db.read { it.getAbsencesCitizen(monday, adult.id, holidayPeriod) }.map { it.date }
        assertEquals(1, allReservations.size)
        allReservations.first().let {
            assertEquals(holidayPeriodStart, it.date)
            assertEquals(Reservation.Times(TimeRange(startTime, endTime)), it.reservation)
            assertTrue(it.staffCreated)
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
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = monday.plusYears(1),
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insertHolidayPeriod(
                holidayPeriod,
                beforeThreshold.toLocalDate().minusDays(1),
                beforeThreshold.toLocalDate().minusDays(1),
            )
            it.insert(
                // NoTimes reservation
                DevReservation(
                    childId = child.id,
                    date = holidayPeriodStart,
                    startTime = null,
                    endTime = null,
                    createdBy = adult.user(CitizenAuthLevel.STRONG).evakaUserId,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                employee.user,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = holidayPeriodStart,
                        TimeRange(startTime, endTime),
                    )
                ),
                citizenReservationThresholdHours,
            )
        }

        // then
        val allReservations = db.read { it.getReservationsCitizen(monday, adult.id, holidayPeriod) }
        assertEquals(1, allReservations.size)
        allReservations.first().let {
            assertEquals(holidayPeriodStart, it.date)
            assertEquals(Reservation.Times(TimeRange(startTime, endTime)), it.reservation)
            assertTrue(it.staffCreated)
        }
    }

    @Test
    fun `citizen cannot override employee created absences with reservations`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = monday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(employee.user.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = tuesday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(employee.user.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.STRONG),
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then reservation is not added
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(0, reservations.size)

        // and absence has not been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
        assertEquals(2, absences.size)
    }

    @Test
    fun `employee can override employee created absences with reservations`() {
        // given
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = tuesday,
                )
            )
            it.insertGuardian(guardianId = adult.id, childId = child.id)
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = monday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(employee.user.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            it.insert(
                DevAbsence(
                    childId = child.id,
                    date = tuesday,
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(employee.user.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        // when
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                employee.user,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = monday,
                        TimeRange(startTime, endTime),
                    ),
                    DailyReservationRequest.Reservations(
                        childId = child.id,
                        date = tuesday,
                        TimeRange(startTime, endTime),
                    ),
                ),
                citizenReservationThresholdHours,
            )
        }

        // then reservations are added
        val reservations = db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
        assertEquals(2, reservations.size)

        // and absences have been removed
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, tuesday)) }
        assertEquals(0, absences.size)
    }

    @ParameterizedTest
    @EnumSource(names = ["PRESCHOOL_DAYCARE", "PRESCHOOL_CLUB", "PREPARATORY_DAYCARE"])
    fun `automatic absence creation when creating part-day reservations`(
        placementType: PlacementType
    ) {
        // given
        val term = FiniteDateRange.ofYear(monday.year)
        val placementRange = FiniteDateRange(monday, thursday)

        val start = LocalTime.of(8, 0)
        val fixedScheduleStart = LocalTime.of(9, 0)
        val fixedScheduleEnd = LocalTime.of(13, 0)
        val end = LocalTime.of(16, 0)
        val daycare =
            DevDaycare(
                areaId = area.id,
                dailyPreschoolTime = TimeRange(fixedScheduleStart, fixedScheduleEnd),
                dailyPreparatoryTime = TimeRange(fixedScheduleStart, fixedScheduleEnd),
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
            )

        db.transaction { tx ->
            tx.insert(
                DevPreschoolTerm(
                    finnishPreschool = term,
                    swedishPreschool = term,
                    extendedTerm = term,
                    applicationPeriod = term,
                    termBreaks = DateSet.empty(),
                )
            )
            tx.insertServiceNeedOption(snDaycareHours120)
            tx.insert(daycare)
            tx.insert(
                    DevPlacement(
                        type = placementType,
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareHours120.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                }
            tx.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when

        //    -TTTT---  fixed schedule time range
        // mo -xxxx---  exact match
        // tu --xx----  inside fixed schedule time range
        // we xxxx----  partly outside fixed schedule time range
        // th -----xxx  outside fixed schedule time range
        val times =
            listOf(
                monday to TimeRange(fixedScheduleStart, fixedScheduleEnd),
                tuesday to
                    TimeRange(fixedScheduleStart.plusHours(1), fixedScheduleEnd.minusHours(1)),
                wednesday to TimeRange(start, fixedScheduleEnd.minusHours(1)),
                thursday to TimeRange(fixedScheduleEnd, end),
            )
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.WEAK),
                times.map { (date, timeRange) ->
                    DailyReservationRequest.Reservations(child.id, date, timeRange)
                },
                citizenReservationThresholdHours,
                plannedAbsenceEnabledForHourBasedServiceNeeds = true,
                automaticFixedScheduleAbsencesEnabled = true,
            )
        }

        // then reservations are added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
                .sortedWith(compareBy({ it.date }, { it.reservation }))
        assertEquals(
            times.map { (date, timeRange) -> date to Reservation.Times(timeRange) },
            reservations.map { it.date to it.reservation },
        )

        // and absences are automatically created based on reserved times
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, placementRange.asDateRange()) }
                .sortedBy { it.date }
        assertEquals(
            listOf(
                Triple(monday, AbsenceType.PLANNED_ABSENCE, AbsenceCategory.BILLABLE),
                Triple(tuesday, AbsenceType.PLANNED_ABSENCE, AbsenceCategory.BILLABLE),
                // No absence for wednesday
                Triple(thursday, AbsenceType.PLANNED_ABSENCE, AbsenceCategory.NONBILLABLE),
            ),
            absences.map { Triple(it.date, it.absenceType, it.category) },
        )
    }

    @Test
    fun `automatic absence creation for 5-year-olds`() {
        // given
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = monday,
                    endDate = wednesday,
                )
            )
            tx.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when
        val times =
            listOf(
                // > 4 hours -> no absence
                monday to TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                // = 4 hours -> billable absence
                tuesday to TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                // < 4 hours -> billable absence
                wednesday to TimeRange(LocalTime.of(10, 0), LocalTime.of(13, 45)),
            )

        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.WEAK),
                times.map { (date, timeRange) ->
                    DailyReservationRequest.Reservations(child.id, date, timeRange)
                },
                citizenReservationThresholdHours,
                plannedAbsenceEnabledForHourBasedServiceNeeds = true,
                automaticFixedScheduleAbsencesEnabled = true,
            )
        }

        // then reservations are added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
                .sortedWith(compareBy({ it.date }, { it.reservation }))
        assertEquals(
            times.map { (date, timeRange) -> date to Reservation.Times(timeRange) },
            reservations.map { it.date to it.reservation },
        )

        // and absences are automatically created based on reserved times
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, DateRange(monday, wednesday)) }
                .sortedBy { it.date }
        assertEquals(
            listOf(
                Triple(tuesday, AbsenceType.OTHER_ABSENCE, AbsenceCategory.BILLABLE),
                Triple(wednesday, AbsenceType.OTHER_ABSENCE, AbsenceCategory.BILLABLE),
            ),
            absences.map { Triple(it.date, it.absenceType, it.category) },
        )
    }

    @Test
    fun `automatic absence creation should delete the full-day absence`() {
        // given
        val term = FiniteDateRange.ofYear(monday.year)
        val placementRange = FiniteDateRange(monday, monday)

        val preschoolTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0))
        val daycare =
            DevDaycare(
                areaId = area.id,
                dailyPreschoolTime = preschoolTime,
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
            )

        db.transaction { tx ->
            tx.insert(
                DevPreschoolTerm(
                    finnishPreschool = term,
                    swedishPreschool = term,
                    extendedTerm = term,
                    applicationPeriod = term,
                    termBreaks = DateSet.empty(),
                )
            )
            tx.insert(daycare)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = placementRange.start,
                    endDate = placementRange.end,
                )
            )
            tx.insertGuardian(guardianId = adult.id, childId = child.id)

            // Already has a full-day absence created by the citizen
            listOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE).forEach { category ->
                tx.insert(
                    DevAbsence(
                        childId = child.id,
                        date = monday,
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = category,
                        modifiedBy = adult.evakaUserId(),
                    )
                )
            }
        }

        // when

        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.WEAK),
                listOf(DailyReservationRequest.Reservations(child.id, monday, preschoolTime)),
                citizenReservationThresholdHours,
                automaticFixedScheduleAbsencesEnabled = true,
            )
        }

        // then reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
                .sortedWith(compareBy({ it.date }, { it.reservation }))
        assertEquals(
            listOf(monday to Reservation.Times(preschoolTime)),
            reservations.map { it.date to it.reservation },
        )

        // full-day absence is gone, only the automatic absence remains
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, placementRange.asDateRange()) }
                .sortedBy { it.date }
        assertEquals(
            listOf(Triple(monday, AbsenceType.OTHER_ABSENCE, AbsenceCategory.BILLABLE)),
            absences.map { Triple(it.date, it.absenceType, it.category) },
        )
    }

    @Test
    fun `if automatic absence is a full-day one, the reservations are still created`() {
        // given
        val term = FiniteDateRange.ofYear(monday.year)
        val placementRange = FiniteDateRange(monday, monday)

        db.transaction { tx ->
            tx.insert(
                DevPreschoolTerm(
                    finnishPreschool = term,
                    swedishPreschool = term,
                    extendedTerm = term,
                    applicationPeriod = term,
                    termBreaks = DateSet.empty(),
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = placementRange.start,
                    endDate = placementRange.end,
                )
            )
            tx.insertGuardian(guardianId = adult.id, childId = child.id)
        }

        // when

        val veryShortReservation = TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 14))
        db.transaction {
            createReservationsAndAbsences(
                it,
                beforeThreshold,
                adult.user(CitizenAuthLevel.WEAK),
                listOf(
                    DailyReservationRequest.Reservations(child.id, monday, veryShortReservation)
                ),
                citizenReservationThresholdHours,
                automaticFixedScheduleAbsencesEnabled = true,
            )
        }

        // then reservation is added
        val reservations =
            db.read { it.getReservationsCitizen(monday, adult.id, queryRange) }
                .sortedWith(compareBy({ it.date }, { it.reservation }))
        assertEquals(
            listOf(monday to Reservation.Times(veryShortReservation)),
            reservations.map { it.date to it.reservation },
        )

        // full-day absence is created, because the reservation is so short
        val absences =
            db.read { it.getAbsencesOfChildByRange(child.id, placementRange.asDateRange()) }
                .sortedWith(compareBy({ it.date }, { it.category }))
        assertEquals(
            listOf(
                Triple(monday, AbsenceType.OTHER_ABSENCE, AbsenceCategory.BILLABLE),
                Triple(monday, AbsenceType.OTHER_ABSENCE, AbsenceCategory.NONBILLABLE),
            ),
            absences.map { Triple(it.date, it.absenceType, it.category) },
        )
    }
}
