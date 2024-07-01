// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.Translatable
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HolidayPeriodControllerCitizenIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    private val emptyTranslatable = Translatable("", "", "")
    private val freePeriodQuestionnaire =
        FixedPeriodQuestionnaireBody(
            active = FiniteDateRange(LocalDate.of(2021, 4, 1), LocalDate.of(2021, 5, 31)),
            periodOptions =
                listOf(
                    FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 7)),
                    FiniteDateRange(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 14))
                ),
            periodOptionLabel = emptyTranslatable,
            description = emptyTranslatable,
            descriptionLink = emptyTranslatable,
            conditions = QuestionnaireConditions(),
            title = emptyTranslatable,
            absenceType = AbsenceType.FREE_ABSENCE,
            requiresStrongAuth = false
        )
    private val mockToday: LocalDate = freePeriodQuestionnaire.active.end.minusWeeks(1)

    private val child1 = testChild_1
    private val parent = testAdult_1
    private val authenticatedParent = AuthenticatedUser.Citizen(parent.id, CitizenAuthLevel.STRONG)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            listOf(testChild_1, testChild_2, testChild_3, testChild_4).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }

            tx.insertGuardian(parent.id, child1.id)

            tx.insertPlacement(
                PlacementType.DAYCARE,
                child1.id,
                testDaycare.id,
                mockToday.minusYears(2),
                mockToday.plusYears(1),
                false
            )
        }
    }

    @Test
    fun `active questionnaire is eligible for all children when there are no conditions`() {
        val child2 = testChild_2
        db.transaction { tx -> tx.insertGuardian(parent.id, child2.id) }
        createFixedPeriodQuestionnaire(freePeriodQuestionnaire)

        val response = getActiveQuestionnaires(mockToday)

        assertEquals(1, response.size)
        assertEquals(listOf(child1.id, child2.id).sorted(), response[0].eligibleChildren.sorted())
    }

    @Test
    fun `active questionnaire is eligible for children that fulfil the continuous placement condition`() {
        val child2 = testChild_2
        val child3 = testChild_3
        val child4 = testChild_4
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
                testDaycare.id,
                condition.start,
                condition.end.minusDays(1),
                false
            )
            // child4 has two placements that cover the period together
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child4.id,
                testDaycare.id,
                condition.start,
                condition.start.plusDays(5),
                false
            )
            tx.insertPlacement(
                PlacementType.DAYCARE,
                child4.id,
                testDaycare.id,
                condition.start.plusDays(6),
                condition.end,
                false
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
        assertEquals(listOf(child1.id, child4.id).sorted(), response[0].eligibleChildren.sorted())
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
                FiniteDateRange(firstOption.start, firstOption.end)
            )
        assertEquals(
            listOf(expectedAnswer),
            db.read { it.getQuestionnaireAnswers(id, listOf(child1.id, testChild_2.id)) }
        )

        assertEquals(listOf(expectedAnswer), getActiveQuestionnaires(mockToday)[0].previousAnswers)
    }

    @Test
    fun `free absences cannot be saved outside of a free period`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        reportFreePeriods(id, freeAbsence(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)), 400)
    }

    @Test
    fun `free absences cannot be saved after the deadline`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        val firstOption = freePeriodQuestionnaire.periodOptions.first()
        reportFreePeriods(
            id,
            freeAbsence(firstOption.start, firstOption.end),
            400,
            freePeriodQuestionnaire.active.end.plusDays(1)
        )
    }

    @Test
    fun `can answer questionnaire before the deadline`() {
        val id = createFixedPeriodQuestionnaire(freePeriodQuestionnaire)
        val firstOption = freePeriodQuestionnaire.periodOptions[0]
        reportFreePeriods(id, freeAbsence(firstOption.start, firstOption.end))

        assertEquals(
            firstOption.dates().map { Absence(child1.id, it, AbsenceType.FREE_ABSENCE) }.toList(),
            db.read { it.getAllAbsences() }
        )

        val secondOption = freePeriodQuestionnaire.periodOptions[1]
        reportFreePeriods(id, freeAbsence(secondOption.start, secondOption.end))
        assertEquals(
            secondOption.dates().map { Absence(child1.id, it, AbsenceType.FREE_ABSENCE) }.toList(),
            db.read { it.getAllAbsences() }
        )
    }

    @Test
    fun `free absences cannot be saved if a child is not eligible`() {
        val child2 = testChild_2
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
        reportFreePeriods(
            id,
            FixedPeriodsBody(mapOf(child1.id to firstOption, child2.id to firstOption)),
            expectedStatus = 400
        )
    }

    private fun getActiveQuestionnaires(mockedDay: LocalDate): List<ActiveQuestionnaire> {
        val (_, _, res) =
            http
                .get("/citizen/holiday-period/questionnaire")
                .asUser(authenticatedParent)
                .withMockedTime(HelsinkiDateTime.of(mockedDay, LocalTime.of(0, 0)))
                .responseObject<List<ActiveQuestionnaire>>(jsonMapper)
        return res.get()
    }

    private fun reportFreePeriods(
        id: HolidayQuestionnaireId,
        body: FixedPeriodsBody,
        expectedStatus: Int = 200,
        mockedDay: LocalDate = mockToday
    ) {
        http
            .post("/citizen/holiday-period/questionnaire/fixed-period/$id")
            .jsonBody(jsonMapper.writeValueAsString(body))
            .asUser(authenticatedParent)
            .withMockedTime(HelsinkiDateTime.of(mockedDay, LocalTime.of(0, 0)))
            .response()
            .also { assertEquals(expectedStatus, it.second.statusCode) }
    }

    private fun freeAbsence(start: LocalDate, end: LocalDate) =
        FixedPeriodsBody(mapOf(child1.id to FiniteDateRange(start, end)))

    private fun createFixedPeriodQuestionnaire(body: FixedPeriodQuestionnaireBody) =
        db.transaction { it.createFixedPeriodQuestionnaire(body) }

    private data class Absence(val childId: ChildId, val date: LocalDate, val type: AbsenceType)

    private fun Database.Read.getAllAbsences(): List<Absence> =
        createQuery {
                sql(
                    "SELECT a.child_id, a.date, a.absence_type as type FROM absence a ORDER BY date"
                )
            }
            .toList<Absence>()
}
