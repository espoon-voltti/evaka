// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.holidayperiod

import evaka.core.CitizenCalendarEnv
import evaka.core.FullApplicationTest
import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.daycare.domain.ProviderType
import evaka.core.pis.service.insertGuardian
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.serviceneed.ShiftCareType
import evaka.core.shared.ChildId
import evaka.core.shared.HolidayQuestionnaireId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.TimeRange
import evaka.core.shared.domain.Translatable
import evaka.core.shared.domain.isHoliday
import evaka.core.shared.domain.isWeekend
import evaka.core.shared.noopTracer
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.AccessControlTest.TestActionRuleMapping
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.IsCitizen
import evaka.core.snDefaultDaycare
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class HolidayPeriodControllerCitizenIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var holidayPeriodControllerCitizen: HolidayPeriodControllerCitizen
    @Autowired private lateinit var citizenCalendarEnv: CitizenCalendarEnv

    private val emptyTranslatable = Translatable("", "", "")
    private val freePeriodQuestionnaire =
        QuestionnaireBody.FixedPeriodQuestionnaireBody(
            active = FiniteDateRange(LocalDate.of(2021, 4, 1), LocalDate.of(2021, 5, 31)),
            periodOptions =
                listOf(
                    FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 7)),
                    FiniteDateRange(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 14)),
                ),
            periodOptionLabel = emptyTranslatable,
            description = emptyTranslatable,
            descriptionLink = emptyTranslatable,
            conditions = QuestionnaireConditions(),
            title = emptyTranslatable,
            absenceType = AbsenceType.FREE_ABSENCE,
            requiresStrongAuth = false,
        )
    private val freeRangesQuestionnaire =
        QuestionnaireBody.OpenRangesQuestionnaireBody(
            active = FiniteDateRange(LocalDate.of(2021, 4, 1), LocalDate.of(2021, 5, 31)),
            period = FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 8, 31)),
            absenceTypeThreshold = 30,
            description = emptyTranslatable,
            descriptionLink = emptyTranslatable,
            conditions = QuestionnaireConditions(),
            title = emptyTranslatable,
            absenceType = AbsenceType.FREE_ABSENCE,
            requiresStrongAuth = false,
        )
    private val mockToday: LocalDate = freePeriodQuestionnaire.active.end.minusWeeks(1)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val voucherDaycare =
        DevDaycare(areaId = area.id, providerType = ProviderType.PRIVATE_SERVICE_VOUCHER)
    private val child1 = DevPerson()
    private val child2 = DevPerson()
    private val child3 = DevPerson()
    private val child4 = DevPerson()
    private val parent = DevPerson()
    private val authenticatedParent = parent.user(CitizenAuthLevel.STRONG)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(parent, DevPersonType.ADULT)
            listOf(child1, child2, child3, child4).forEach { tx.insert(it, DevPersonType.CHILD) }

            tx.insertGuardian(parent.id, child1.id)

            val placement =
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = mockToday.minusYears(2),
                    endDate = mockToday.plusYears(1),
                )
            tx.insert(placement)
            tx.insertServiceNeedOption(snDefaultDaycare)
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = authenticatedParent.evakaUserId,
                )
            )
        }
    }

    @Test
    fun `active questionnaire is eligible for all children with active placement when there are no conditions`() {
        db.transaction { tx -> tx.insertGuardian(parent.id, child2.id) }
        createFixedPeriodQuestionnaire(freePeriodQuestionnaire)

        val response = getActiveQuestionnaires(mockToday)

        assertEquals(1, response.size)
        assertThat(response[0].eligibleChildren)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(child1.id to freePeriodQuestionnaire.periodOptions)
            )
    }

    @Test
    fun `active questionnaire is eligible for children that fulfil the continuous placement condition`() {
        val condition = FiniteDateRange(mockToday, mockToday.plusMonths(1))

        db.transaction { tx ->
            tx.insertGuardian(parent.id, child2.id)
            tx.insertGuardian(parent.id, child3.id)
            tx.insertGuardian(parent.id, child4.id)

            // child1 has a placement that covers the whole period
            // child2 has no placement
            // child3 has a placement that covers the period partly
            tx.insert(
                DevPlacement(
                    childId = child3.id,
                    unitId = daycare.id,
                    startDate = condition.start,
                    endDate = condition.end.minusDays(1),
                )
            )
            // child4 has two placements that cover the period together
            tx.insert(
                DevPlacement(
                    childId = child4.id,
                    unitId = daycare.id,
                    startDate = condition.start,
                    endDate = condition.start.plusDays(5),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child4.id,
                    unitId = daycare.id,
                    startDate = condition.start.plusDays(6),
                    endDate = condition.end,
                )
            )
        }
        createFixedPeriodQuestionnaire(
            freePeriodQuestionnaire.copy(
                conditions =
                    QuestionnaireConditions(
                        continuousPlacement = FiniteDateRange(mockToday, mockToday.plusMonths(1))
                    )
            )
        )

        val response = getActiveQuestionnaires(mockToday)

        assertEquals(1, response.size)
        assertThat(response[0].eligibleChildren)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(child1.id to freePeriodQuestionnaire.periodOptions)
            )
    }

    @Test
    fun `active questionnaire is eligible for children that do not have active placement in private voucher value unit`() {
        val condition = FiniteDateRange(mockToday, mockToday.plusMonths(1))

        db.transaction { tx ->
            tx.insertGuardian(parent.id, child4.id)
            tx.insert(voucherDaycare)
            tx.insert(
                DevPlacement(
                    childId = child4.id,
                    unitId = voucherDaycare.id,
                    startDate = mockToday.minusYears(2),
                    endDate = mockToday.plusYears(1),
                )
            )
        }

        createFixedPeriodQuestionnaire(freePeriodQuestionnaire)

        val response = getActiveQuestionnaires(mockToday)

        assertEquals(1, response.size)
        assertThat(response[0].eligibleChildren)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(child1.id to freePeriodQuestionnaire.periodOptions)
            )
    }

    @Test
    fun `questionnaire answer is saved and returned with active questionnaire`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire.copy())

        val response = getActiveQuestionnaires(mockToday)

        assertEquals(1, response.size)
        assertEquals(listOf(), response[0].previousAnswers)

        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        reportFreePeriods(id, freeAbsence(firstOption.start, firstOption.end))

        val expectedAnswer =
            HolidayQuestionnaireAnswer(
                id,
                child1.id,
                FiniteDateRange(firstOption.start, firstOption.end),
                listOf(),
            )
        assertEquals(
            listOf(expectedAnswer),
            db.read { it.getQuestionnaireAnswers(id, listOf(child1.id, child2.id)) },
        )

        assertEquals(listOf(expectedAnswer), getActiveQuestionnaires(mockToday)[0].previousAnswers)
    }

    @Test
    fun `free absences cannot be saved outside of a free period`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        assertThrows<BadRequest> {
            reportFreePeriods(id, freeAbsence(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)))
        }
    }

    @Test
    fun `free absences cannot be saved after the deadline`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        val firstOption = freePeriodQuestionnaire.periodOptions.first()
        assertThrows<BadRequest> {
            reportFreePeriods(
                id,
                freeAbsence(firstOption.start, firstOption.end),
                freePeriodQuestionnaire.active.end.plusDays(1),
            )
        }
    }

    @Test
    fun `can answer questionnaire before the deadline`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        reportFreePeriods(id, freeAbsence(firstOption.start, firstOption.end))

        assertEquals(
            firstOption
                .dates()
                .filter { !it.isWeekend() && !it.isHoliday() }
                .map { Absence(child1.id, it, AbsenceType.FREE_ABSENCE) }
                .toList(),
            db.read { it.getAllAbsences() },
        )

        val secondOption = freePeriodQuestionnaire.periodOptions[1]
        reportFreePeriods(id, freeAbsence(secondOption.start, secondOption.end))
        assertEquals(
            secondOption
                .dates()
                .filter { !it.isWeekend() && !it.isHoliday() }
                .map { Absence(child1.id, it, AbsenceType.FREE_ABSENCE) }
                .toList(),
            db.read { it.getAllAbsences() },
        )
    }

    @Test
    fun `free absences are saved only on operation days`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        reportFreePeriods(id, freeAbsence(firstOption.start, firstOption.end))
        reportFreePeriods(id, freeAbsenceShiftCare(firstOption.start, firstOption.end))
        assertEquals(
            firstOption
                .dates()
                .filter { !it.isWeekend() && !it.isHoliday() }
                .map { Absence(child1.id, it, AbsenceType.FREE_ABSENCE) }
                .toList(),
            db.read { it.getAllAbsences().filter { absence -> absence.childId == child1.id } },
        )
        assertEquals(
            firstOption.dates().map { Absence(child2.id, it, AbsenceType.FREE_ABSENCE) }.toList(),
            db.read { it.getAllAbsences().filter { absence -> absence.childId == child2.id } },
        )
    }

    @Test
    fun `free absences are saved on weekends for a shift care child that has an earlier non-shift-care service need on the same placement`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        val firstOption = freePeriodQuestionnaire.periodOptions[0]

        val fullDay = TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59"))
        db.transaction { tx ->
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = area.id,
                        operationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, null, null),
                        shiftCareOperationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, fullDay, fullDay),
                        shiftCareOpenOnHolidays = true,
                    )
                )
            tx.insertGuardian(parent.id, child2.id)

            val placement =
                DevPlacement(
                    childId = child2.id,
                    unitId = daycareId,
                    startDate = mockToday.minusYears(2),
                    endDate = mockToday.plusYears(1),
                )
            tx.insert(placement)
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = placement.startDate,
                    endDate = firstOption.start.minusDays(1),
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.NONE,
                    confirmedBy = authenticatedParent.evakaUserId,
                )
            )
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = firstOption.start,
                    endDate = placement.endDate,
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.FULL,
                    confirmedBy = authenticatedParent.evakaUserId,
                )
            )
        }

        reportFreePeriods(id, FixedPeriodsBody(mapOf(child2.id to firstOption)))

        assertEquals(
            firstOption.dates().map { Absence(child2.id, it, AbsenceType.FREE_ABSENCE) }.toList(),
            db.read { it.getAllAbsences().filter { absence -> absence.childId == child2.id } },
        )
    }

    @Test
    fun `free absences cannot be saved if a child is not eligible`() {
        db.transaction { tx ->
            tx.insertGuardian(parent.id, child2.id)
            // child2 has no placement
        }
        val id =
            createFixedPeriodQuestionnaire(
                freePeriodQuestionnaire.copy(
                    conditions =
                        QuestionnaireConditions(
                            continuousPlacement =
                                FiniteDateRange(mockToday, mockToday.plusMonths(1))
                        )
                )
            )

        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        assertThrows<BadRequest> {
            reportFreePeriods(
                id,
                FixedPeriodsBody(mapOf(child1.id to firstOption, child2.id to firstOption)),
            )
        }
    }

    @Test
    fun `free absences cannot be saved if a child's placement starts after given option`() {
        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        db.transaction { tx ->
            tx.insertGuardian(parent.id, child2.id)
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = firstOption.start.plusDays(1),
                    endDate = firstOption.end,
                )
            )
        }
        val id =
            createFixedPeriodQuestionnaire(
                freePeriodQuestionnaire.copy(
                    conditions = QuestionnaireConditions(continuousPlacement = null)
                )
            )

        assertThrows<BadRequest> {
            reportFreePeriods(
                id,
                FixedPeriodsBody(mapOf(child1.id to firstOption, child2.id to firstOption)),
            )
        }
    }

    @Test
    fun `free absences cannot be saved if a child's placement ends before given option`() {
        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        db.transaction { tx ->
            tx.insertGuardian(parent.id, child2.id)
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = firstOption.start,
                    endDate = firstOption.end.minusDays(1),
                )
            )
        }
        val id =
            createFixedPeriodQuestionnaire(
                freePeriodQuestionnaire.copy(
                    conditions = QuestionnaireConditions(continuousPlacement = null)
                )
            )

        assertThrows<BadRequest> {
            reportFreePeriods(
                id,
                FixedPeriodsBody(mapOf(child1.id to firstOption, child2.id to firstOption)),
            )
        }
    }

    @Test
    fun `correct type of active questionnaire is fetched based on feature flag`() {
        createOpenRangesQuestionnaire(freeRangesQuestionnaire)

        val rules = TestActionRuleMapping()
        rules.add(
            Action.Global.READ_ACTIVE_HOLIDAY_QUESTIONNAIRES,
            IsCitizen(allowWeakLogin = false).any(),
        )

        val controller =
            HolidayPeriodControllerCitizen(
                AccessControl(rules, noopTracer()),
                featureConfig.copy(holidayQuestionnaireType = QuestionnaireType.OPEN_RANGES),
                citizenCalendarEnv,
                evakaEnv,
            )
        val response = getActiveQuestionnaires(mockToday, controller)

        assertEquals(1, response.size)
    }

    @Test
    fun `open ranges answer uses PLANNED_ABSENCE for child with hour-based service need`() {
        val hourBasedOptionId = ServiceNeedOptionId(UUID.randomUUID())
        val hourBasedOption =
            ServiceNeedOption(
                id = hourBasedOptionId,
                nameFi = "Tuntiperusteinen",
                nameSv = "Tuntiperusteinen",
                nameEn = "Hour-based",
                validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
                defaultOption = false,
                feeCoefficient = BigDecimal("1.0"),
                occupancyCoefficient = BigDecimal("1.0"),
                occupancyCoefficientUnder3y = BigDecimal("1.75"),
                realizedOccupancyCoefficient = BigDecimal("1.0"),
                realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
                daycareHoursPerWeek = 35,
                contractDaysPerMonth = null,
                daycareHoursPerMonth = 120,
                partDay = false,
                partWeek = false,
                feeDescriptionFi = "",
                feeDescriptionSv = "",
                voucherValueDescriptionFi = "",
                voucherValueDescriptionSv = "",
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = null,
                showForCitizen = true,
            )
        db.transaction { tx ->
            tx.insertGuardian(parent.id, child2.id)
            tx.insertServiceNeedOption(hourBasedOption)
            val placement =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = mockToday.minusYears(2),
                        endDate = mockToday.plusYears(1),
                    )
                )
            tx.insert(
                DevServiceNeed(
                    placementId = placement,
                    startDate = mockToday.minusYears(2),
                    endDate = mockToday.plusYears(1),
                    optionId = hourBasedOptionId,
                    confirmedBy = authenticatedParent.evakaUserId,
                )
            )
        }
        whenever(evakaEnv.plannedAbsenceEnabledForHourBasedServiceNeeds).thenReturn(true)

        val id = createOpenRangesQuestionnaire(freeRangesQuestionnaire)
        val shortRange =
            FiniteDateRange(
                freeRangesQuestionnaire.period.start,
                freeRangesQuestionnaire.period.start.plusDays(10),
            )

        val rules = TestActionRuleMapping()
        rules.add(
            Action.Citizen.Child.CREATE_HOLIDAY_ABSENCE,
            IsCitizen(allowWeakLogin = false).any(),
        )
        val controller =
            HolidayPeriodControllerCitizen(
                AccessControl(rules, noopTracer()),
                featureConfig.copy(holidayQuestionnaireType = QuestionnaireType.OPEN_RANGES),
                citizenCalendarEnv,
                evakaEnv,
            )
        val mockClock = MockEvakaClock(HelsinkiDateTime.of(mockToday, LocalTime.of(11, 0)))
        controller.answerOpenRangeQuestionnaire(
            dbInstance(),
            authenticatedParent,
            mockClock,
            id,
            OpenRangesBody(mapOf(child2.id to listOf(shortRange))),
        )

        val absences =
            db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT child_id, date, absence_type, category FROM absence ORDER BY date"
                        )
                    }
                    .toList<BillableAbsence>()
            }
        val billableAbsences =
            absences.filter {
                it.childId == child2.id &&
                    it.category == AbsenceCategory.BILLABLE &&
                    !it.date.isWeekend()
            }
        assertThat(billableAbsences).isNotEmpty
        billableAbsences.forEach { absence ->
            assertEquals(AbsenceType.PLANNED_ABSENCE, absence.absenceType)
        }
        val nonbillableAbsences =
            absences.filter {
                it.childId == child2.id &&
                    it.category == AbsenceCategory.NONBILLABLE &&
                    !it.date.isWeekend()
            }
        assertThat(nonbillableAbsences).isNotEmpty
        nonbillableAbsences.forEach { absence ->
            assertEquals(AbsenceType.OTHER_ABSENCE, absence.absenceType)
        }
    }

    @Test
    fun `free absences are saved only when length of absence exceeds threshold`() {
        val id = createOpenRangesQuestionnaire(freeRangesQuestionnaire)
        reportFreeRanges(
            id,
            openAbsences(
                listOf(
                    FiniteDateRange(
                        freeRangesQuestionnaire.period.start,
                        freeRangesQuestionnaire.period.start.plusDays(30),
                    ),
                    FiniteDateRange(
                        freeRangesQuestionnaire.period.end.minusDays(10),
                        freeRangesQuestionnaire.period.end,
                    ),
                )
            ),
        )
        val absences = db.read { it.getAllAbsences() }
        assertEquals(
            Absence(
                child1.id,
                freeRangesQuestionnaire.period.start,
                freeRangesQuestionnaire.absenceType,
            ),
            absences.first { !it.date.isWeekend() },
        )
        assertEquals(
            Absence(child1.id, freeRangesQuestionnaire.period.end, AbsenceType.OTHER_ABSENCE),
            absences.last { !it.date.isWeekend() },
        )
    }

    private fun getActiveQuestionnaires(
        mockedDay: LocalDate,
        controller: HolidayPeriodControllerCitizen = holidayPeriodControllerCitizen,
    ): List<ActiveQuestionnaire> {
        val mockClock = MockEvakaClock(HelsinkiDateTime.of(mockedDay, LocalTime.of(11, 0)))
        return controller.getActiveQuestionnaires(dbInstance(), authenticatedParent, mockClock)
    }

    private fun reportFreePeriods(
        id: HolidayQuestionnaireId,
        body: FixedPeriodsBody,
        mockedDay: LocalDate = mockToday,
    ) {
        val mockClock = MockEvakaClock(HelsinkiDateTime.of(mockedDay, LocalTime.of(11, 0)))
        holidayPeriodControllerCitizen.answerFixedPeriodQuestionnaire(
            dbInstance(),
            authenticatedParent,
            mockClock,
            id,
            body,
        )
    }

    private fun freeAbsence(start: LocalDate, end: LocalDate) =
        FixedPeriodsBody(mapOf(child1.id to FiniteDateRange(start, end)))

    private fun freeAbsenceShiftCare(start: LocalDate, end: LocalDate): FixedPeriodsBody {
        val fullDay = TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59"))
        db.transaction { tx ->
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = area.id,
                        operationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, null, null),
                        shiftCareOperationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, fullDay, fullDay),
                        shiftCareOpenOnHolidays = true,
                    )
                )
            tx.insertGuardian(parent.id, child2.id)

            val placement =
                DevPlacement(
                    childId = child2.id,
                    unitId = daycareId,
                    startDate = mockToday.minusYears(2),
                    endDate = mockToday.plusYears(1),
                )
            tx.insert(placement)
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                    optionId = snDefaultDaycare.id,
                    shiftCare = ShiftCareType.FULL,
                    confirmedBy = authenticatedParent.evakaUserId,
                )
            )
        }

        return FixedPeriodsBody(mapOf(child2.id to FiniteDateRange(start, end)))
    }

    private fun createFixedPeriodQuestionnaire(
        body: QuestionnaireBody.FixedPeriodQuestionnaireBody
    ) = db.transaction { it.createFixedPeriodQuestionnaire(body) }

    private fun reportFreeRanges(
        id: HolidayQuestionnaireId,
        body: OpenRangesBody,
        mockedDay: LocalDate = mockToday,
    ) {
        val mockClock = MockEvakaClock(HelsinkiDateTime.of(mockedDay, LocalTime.of(11, 0)))
        holidayPeriodControllerCitizen.answerOpenRangeQuestionnaire(
            dbInstance(),
            authenticatedParent,
            mockClock,
            id,
            body,
        )
    }

    private fun openAbsences(ranges: List<FiniteDateRange>) =
        OpenRangesBody(mapOf(child1.id to ranges))

    private fun createOpenRangesQuestionnaire(body: QuestionnaireBody.OpenRangesQuestionnaireBody) =
        db.transaction { it.createOpenRangesQuestionnaire(body) }

    private data class Absence(val childId: ChildId, val date: LocalDate, val type: AbsenceType)

    private data class BillableAbsence(
        val childId: ChildId,
        val date: LocalDate,
        val absenceType: AbsenceType,
        val category: AbsenceCategory,
    )

    private fun Database.Read.getAllAbsences(): List<Absence> =
        createQuery {
                sql(
                    "SELECT a.child_id, a.date, a.absence_type as type FROM absence a ORDER BY date"
                )
            }
            .toList<Absence>()
}
