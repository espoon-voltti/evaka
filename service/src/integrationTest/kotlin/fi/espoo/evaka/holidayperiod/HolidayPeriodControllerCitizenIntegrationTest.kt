// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOptions
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.Translatable
import fi.espoo.evaka.shared.domain.isHoliday
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.AccessControlTest.TestActionRuleMapping
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class HolidayPeriodControllerCitizenIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var holidayPeriodControllerCitizen: HolidayPeriodControllerCitizen

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

    final val area = DevCareArea()
    val daycare = DevDaycare(areaId = area.id)
    val child1 = DevPerson(id = ChildId(UUID.randomUUID()))
    val child2 = DevPerson(id = ChildId(UUID.randomUUID()))
    val child3 = DevPerson(id = ChildId(UUID.randomUUID()))
    val child4 = DevPerson(id = ChildId(UUID.randomUUID()))
    private val parent = DevPerson()
    private val authenticatedParent = AuthenticatedUser.Citizen(parent.id, CitizenAuthLevel.STRONG)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(parent, DevPersonType.ADULT)
            listOf(child1, child2, child3, child4).forEach { tx.insert(it, DevPersonType.CHILD) }

            tx.insertGuardian(parent.id, child1.id)

            val placement =
                tx.insertPlacement(
                    PlacementType.DAYCARE,
                    child1.id,
                    daycare.id,
                    mockToday.minusYears(2),
                    mockToday.plusYears(1),
                    false,
                )
            tx.insertServiceNeedOptions()
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                    optionId =
                        ServiceNeedOptionId(
                            UUID.fromString("7406df92-e715-11ec-9ec2-9b7ff580dcb4") // full time daycare
                        ),
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
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child3.id,
                daycare.id,
                condition.start,
                condition.end.minusDays(1),
                false,
            )
            // child4 has two placements that cover the period together
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child4.id,
                daycare.id,
                condition.start,
                condition.start.plusDays(5),
                false,
            )
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child4.id,
                daycare.id,
                condition.start.plusDays(6),
                condition.end,
                false,
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
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child2.id,
                daycare.id,
                firstOption.start.plusDays(1),
                firstOption.end,
                false,
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
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child2.id,
                daycare.id,
                firstOption.start,
                firstOption.end.minusDays(1),
                false,
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
            )
        val response = getActiveQuestionnaires(mockToday, controller)

        assertEquals(1, response.size)
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
        val mockClock =
            MockEvakaClock(HelsinkiDateTime.Companion.of(mockedDay, LocalTime.of(11, 0)))
        return controller.getActiveQuestionnaires(dbInstance(), authenticatedParent, mockClock)
    }

    private fun reportFreePeriods(
        id: HolidayQuestionnaireId,
        body: FixedPeriodsBody,
        mockedDay: LocalDate = mockToday,
    ) {
        val mockClock =
            MockEvakaClock(HelsinkiDateTime.Companion.of(mockedDay, LocalTime.of(11, 0)))
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
        db.transaction { tx ->
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = area.id,
                        operationTimes =
                            listOf(
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                                TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                            ),
                        shiftCareOpenOnHolidays = true,
                    )
                )
            tx.insertGuardian(parent.id, child2.id)

            val placement =
                tx.insertPlacement(
                    PlacementType.DAYCARE,
                    child2.id,
                    daycareId,
                    mockToday.minusYears(2),
                    mockToday.plusYears(1),
                    false,
                )
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                    optionId =
                        ServiceNeedOptionId(
                            UUID.fromString("7406df92-e715-11ec-9ec2-9b7ff580dcb4") // full time daycare
                        ),
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
        val mockClock =
            MockEvakaClock(HelsinkiDateTime.Companion.of(mockedDay, LocalTime.of(11, 0)))
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

    private fun Database.Read.getAllAbsences(): List<Absence> =
        createQuery {
                sql(
                    "SELECT a.child_id, a.date, a.absence_type as type FROM absence a ORDER BY date"
                )
            }
            .toList<Absence>()
}
