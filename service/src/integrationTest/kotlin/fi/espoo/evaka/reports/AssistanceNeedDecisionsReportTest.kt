// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceneed.decision.AssistanceLevel
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecision
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployeeForm
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionForm
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionGuardian
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionMakerForm
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionRequest
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.ServiceOptions
import fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
import fi.espoo.evaka.assistanceneed.decision.UnitIdInfo
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testDecisionMaker_3
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AssistanceNeedDecisionsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val assistanceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val decisionMaker1 =
        AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN))
    private val decisionMaker2 =
        AuthenticatedUser.Employee(testDecisionMaker_3.id, setOf(UserRole.ADMIN))

    private val testDecision =
        AssistanceNeedDecisionForm(
            validityPeriod = DateRange(LocalDate.of(2022, 1, 1), null),
            status = AssistanceNeedDecisionStatus.DRAFT,
            language = OfficialLanguage.FI,
            decisionMade = LocalDate.of(2021, 12, 31),
            sentForDecision = null,
            selectedUnit = UnitIdInfo(id = testDaycare.id),
            preparedBy1 =
                AssistanceNeedDecisionEmployeeForm(
                    employeeId = assistanceWorker.id,
                    title = "worker",
                    phoneNumber = "01020405060",
                ),
            preparedBy2 = null,
            decisionMaker = null,
            pedagogicalMotivation = "Pedagogical motivation",
            structuralMotivationOptions =
                StructuralMotivationOptions(
                    smallerGroup = false,
                    specialGroup = true,
                    smallGroup = false,
                    groupAssistant = false,
                    childAssistant = false,
                    additionalStaff = false,
                ),
            structuralMotivationDescription = "Structural motivation description",
            careMotivation = "Care motivation",
            serviceOptions =
                ServiceOptions(
                    consultationSpecialEd = false,
                    partTimeSpecialEd = false,
                    fullTimeSpecialEd = false,
                    interpretationAndAssistanceServices = false,
                    specialAides = true,
                ),
            servicesMotivation = "Services Motivation",
            expertResponsibilities = "Expert responsibilities",
            guardiansHeardOn = LocalDate.of(2021, 11, 30),
            guardianInfo =
                setOf(
                    AssistanceNeedDecisionGuardian(
                        id = null,
                        personId = testAdult_1.id,
                        name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                        isHeard = true,
                        details = "Lots of details",
                    )
                ),
            viewOfGuardians = "The view of the guardians",
            otherRepresentativeHeard = false,
            otherRepresentativeDetails = null,
            assistanceLevels = setOf(AssistanceLevel.ENHANCED_ASSISTANCE),
            motivationForDecision = "Motivation for decision",
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testDecisionMaker_2)
            tx.insert(testDecisionMaker_3)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
    }

    @Test
    fun `unopened indication is only for decisions where current employee is the decision-maker`() {
        this.createTestDecisions()

        val decisionMaker1Report =
            whenGetAssistanceNeedDecisionsReportThenExpectSuccess(decisionMaker1)
        assertEquals(3, decisionMaker1Report.count { it.isOpened == false })
        assertEquals(1, decisionMaker1Report.count { it.isOpened == null })

        val decisionMaker2Report =
            whenGetAssistanceNeedDecisionsReportThenExpectSuccess(decisionMaker2)
        assertEquals(1, decisionMaker2Report.count { it.isOpened == false })
        assertEquals(3, decisionMaker2Report.count { it.isOpened == null })
    }

    @Test
    fun `unread count is decreased when a decision is opened`() {
        val decisions = this.createTestDecisions()

        val decisionMaker1UnreadCount =
            whenGetAssistanceNeedDecisionsReportUnreadCountThenExpectSuccess(decisionMaker1)
        assertEquals(3, decisionMaker1UnreadCount)

        whenPostAssistanceNeedDecisionMarkAsOpenedThenExpectSuccess(
            decisions.get(decisionMaker1.id)!!.first(),
            decisionMaker1,
        )

        val decisionMaker1UnreadCountAfterOpen =
            whenGetAssistanceNeedDecisionsReportUnreadCountThenExpectSuccess(decisionMaker1)
        assertEquals(2, decisionMaker1UnreadCountAfterOpen)

        val decisionMaker2UnreadCountAfterOpen =
            whenGetAssistanceNeedDecisionsReportUnreadCountThenExpectSuccess(decisionMaker2)
        assertEquals(1, decisionMaker2UnreadCountAfterOpen)
    }

    @Test
    fun `unopened indication is updated for opened decision`() {
        val decisions = this.createTestDecisions()

        val firstDecisionId = decisions.get(decisionMaker1.id)!!.first()
        whenPostAssistanceNeedDecisionMarkAsOpenedThenExpectSuccess(firstDecisionId, decisionMaker1)

        val report = whenGetAssistanceNeedDecisionsReportThenExpectSuccess(decisionMaker1)
        assertEquals(true, report.find { it.id == firstDecisionId.raw }?.isOpened)
        assertEquals(2, report.count { it.isOpened == false })
        assertEquals(1, report.count { it.isOpened == null })
    }

    private fun whenPostAssistanceNeedDecisionAndSendThenExpectSuccess(
        request: AssistanceNeedDecisionRequest
    ): AssistanceNeedDecision {
        val (_, res, result) =
            http
                .post("/employee/children/${testChild_1.id}/assistance-needs/decision")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(assistanceWorker)
                .responseObject<AssistanceNeedDecision>(jsonMapper)

        assertEquals(200, res.statusCode)

        val (_, sendRes) =
            http
                .post("/employee/assistance-need-decision/${result.get().id}/send")
                .asUser(assistanceWorker)
                .response()

        assertEquals(200, sendRes.statusCode)

        return result.get()
    }

    private fun whenGetAssistanceNeedDecisionsReportThenExpectSuccess(
        decisionMaker: AuthenticatedUser
    ): List<AssistanceNeedDecisionsReportRow> {
        val (_, res, result) =
            http
                .get("/employee/reports/assistance-need-decisions")
                .asUser(decisionMaker)
                .responseObject<List<AssistanceNeedDecisionsReportRow>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenGetAssistanceNeedDecisionsReportUnreadCountThenExpectSuccess(
        decisionMaker: AuthenticatedUser
    ): Int {
        val (_, res, result) =
            http
                .get("/employee/reports/assistance-need-decisions/unread-count")
                .asUser(decisionMaker)
                .responseObject<Int>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPostAssistanceNeedDecisionMarkAsOpenedThenExpectSuccess(
        id: AssistanceNeedDecisionId,
        decisionMaker: AuthenticatedUser,
    ) {
        val (_, res) =
            http
                .post("/employee/assistance-need-decision/$id/mark-as-opened")
                .asUser(decisionMaker)
                .response()

        assertEquals(200, res.statusCode)
    }

    private fun createTestDecisions() =
        mapOf(decisionMaker1.id to 3, decisionMaker2.id to 1).mapValues { entry ->
            List(entry.value) {
                whenPostAssistanceNeedDecisionAndSendThenExpectSuccess(
                        AssistanceNeedDecisionRequest(
                            decision =
                                testDecision.copy(
                                    decisionMaker =
                                        AssistanceNeedDecisionMakerForm(
                                            employeeId = entry.key,
                                            title = "regional director",
                                        )
                                )
                        )
                    )
                    .id
            }
        }
}
