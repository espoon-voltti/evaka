// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class AssistanceNeedDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val assistanceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private val testDecision = AssistanceNeedDecisionForm(
        startDate = LocalDate.of(2022, 1, 1),
        endDate = LocalDate.of(2023, 1, 1),
        status = AssistanceNeedDecisionStatus.DRAFT,
        language = AssistanceNeedDecisionLanguage.FI,
        decisionMade = LocalDate.of(2021, 12, 31),
        sentForDecision = LocalDate.of(2021, 12, 1),
        selectedUnit = UnitIdInfo(id = testDaycare.id),
        preparedBy1 = AssistanceNeedDecisionEmployeeForm(employeeId = assistanceWorker.id, title = "worker", phoneNumber = "01020405060"),
        preparedBy2 = null,
        decisionMaker = AssistanceNeedDecisionMakerForm(
            employeeId = testDecisionMaker_2.id,
            title = "Decider of everything"
        ),

        pedagogicalMotivation = "Pedagogical motivation",
        structuralMotivationOptions = StructuralMotivationOptions(
            smallerGroup = false,
            specialGroup = true,
            smallGroup = false,
            groupAssistant = false,
            childAssistant = false,
            additionalStaff = false,
        ),
        structuralMotivationDescription = "Structural motivation description",
        careMotivation = "Care motivation",
        serviceOptions = ServiceOptions(
            consultationSpecialEd = false,
            partTimeSpecialEd = false,
            fullTimeSpecialEd = false,
            interpretationAndAssistanceServices = false,
            specialAides = true,
        ),
        servicesMotivation = "Services Motivation",
        expertResponsibilities = "Expert responsibilities",
        guardiansHeardOn = LocalDate.of(2021, 11, 30),
        guardianInfo = setOf(
            AssistanceNeedDecisionGuardian(
                id = null,
                personId = testAdult_1.id,
                name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                isHeard = true,
                details = "Lots of details"
            ),
        ),
        viewOfGuardians = "The view of the guardians",
        otherRepresentativeHeard = false,
        otherRepresentativeDetails = null,

        assistanceLevel = AssistanceLevel.ENHANCED_ASSISTANCE,
        assistanceServicesTime = null,
        motivationForDecision = "Motivation for decision"
    )

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `post and get an assistance need decision`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        assertEquals(testChild_1.id, assistanceNeedDecision.childId)
        assertEquals(testDecision.startDate, assistanceNeedDecision.startDate)
        assertEquals(testDecision.endDate, assistanceNeedDecision.endDate)
        assertEquals(testDecision.status, assistanceNeedDecision.status)
        assertEquals(testDecision.language, assistanceNeedDecision.language)
        assertEquals(testDecision.decisionMade, assistanceNeedDecision.decisionMade)
        assertEquals(testDecision.sentForDecision, assistanceNeedDecision.sentForDecision)
        assertEquals(assistanceNeedDecision.selectedUnit?.id, testDaycare.id)
        assertEquals(assistanceNeedDecision.selectedUnit?.name, testDaycare.name)
        assertEquals(assistanceNeedDecision.selectedUnit?.postOffice, "ESPOO")
        assertEquals(assistanceNeedDecision.selectedUnit?.postalCode, "02100")
        assertEquals(assistanceNeedDecision.selectedUnit?.streetAddress, "Joku katu 9")
        assertEquals(testDecision.preparedBy1?.employeeId, assistanceNeedDecision.preparedBy1?.employeeId)
        assertEquals(testDecision.preparedBy1?.title, assistanceNeedDecision.preparedBy1?.title)
        assertEquals(testDecision.preparedBy1?.phoneNumber, assistanceNeedDecision.preparedBy1?.phoneNumber)
        assertEquals(assistanceNeedDecision.preparedBy1?.name, "${testDecisionMaker_1.firstName} ${testDecisionMaker_1.lastName}")
        assertEquals(assistanceNeedDecision.preparedBy1?.email, testDecisionMaker_1.email)
        assertEquals(assistanceNeedDecision.preparedBy2, null)
        assertEquals(testDecision.decisionMaker?.employeeId, assistanceNeedDecision.decisionMaker?.employeeId)
        assertEquals(testDecision.decisionMaker?.title, assistanceNeedDecision.decisionMaker?.title)
        assertEquals(assistanceNeedDecision.decisionMaker?.name, "${testDecisionMaker_2.firstName} ${testDecisionMaker_2.lastName}")

        assertEquals(testDecision.pedagogicalMotivation, assistanceNeedDecision.pedagogicalMotivation)
        assertEquals(testDecision.structuralMotivationOptions, assistanceNeedDecision.structuralMotivationOptions)
        assertEquals(
            testDecision.structuralMotivationDescription,
            assistanceNeedDecision.structuralMotivationDescription
        )
        assertEquals(testDecision.careMotivation, assistanceNeedDecision.careMotivation)
        assertEquals(testDecision.serviceOptions, assistanceNeedDecision.serviceOptions)
        assertEquals(testDecision.servicesMotivation, assistanceNeedDecision.servicesMotivation)
        assertEquals(testDecision.expertResponsibilities, assistanceNeedDecision.expertResponsibilities)

        assertEquals(testDecision.guardiansHeardOn, assistanceNeedDecision.guardiansHeardOn)
        val storedGuardiansWithoutId = assistanceNeedDecision.guardianInfo.map { g ->
            AssistanceNeedDecisionGuardian(
                id = null,
                personId = g.personId,
                name = g.name,
                isHeard = g.isHeard,
                details = g.details
            )
        }.toSet()
        assertEquals(testDecision.guardianInfo, storedGuardiansWithoutId)

        assertEquals(testDecision.viewOfGuardians, assistanceNeedDecision.viewOfGuardians)
        assertEquals(testDecision.otherRepresentativeHeard, assistanceNeedDecision.otherRepresentativeHeard)
        assertEquals(testDecision.otherRepresentativeDetails, assistanceNeedDecision.otherRepresentativeDetails)

        assertEquals(testDecision.assistanceLevel, assistanceNeedDecision.assistanceLevel)
        assertEquals(testDecision.assistanceServicesTime, assistanceNeedDecision.assistanceServicesTime)
        assertEquals(testDecision.motivationForDecision, assistanceNeedDecision.motivationForDecision)
    }

    @Test
    fun `posting without guardians adds guardians before saving`() {
        db.transaction { tx ->
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
        }
        val testDecisionWithoutGuardian = testDecision.copy(guardianInfo = setOf())
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecisionWithoutGuardian
            )
        )
        val firstGuardian = assistanceNeedDecision.guardianInfo.first()
        assertEquals(testAdult_1.id, firstGuardian.personId)
        assertEquals("${testAdult_1.lastName} ${testAdult_1.firstName}", firstGuardian.name)
    }

    @Test
    fun `Updating a decision stores the new information`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )
        val updatedDecision = assistanceNeedDecision.copy(
            pedagogicalMotivation = "Such Pedagogical motivation",
            structuralMotivationOptions = assistanceNeedDecision.structuralMotivationOptions,
            structuralMotivationDescription = "Very Structural motivation",
            careMotivation = "wow",
            guardianInfo = assistanceNeedDecision.guardianInfo.map {
                AssistanceNeedDecisionGuardian(
                    id = it.id,
                    personId = it.personId,
                    name = it.name,
                    isHeard = true,
                    details = "Updated details"
                )
            }.toSet()
        ).toForm()

        whenPutAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = updatedDecision
            ),
            assistanceNeedDecision.id!!
        )

        val finalDecision = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)

        assertEquals(updatedDecision.pedagogicalMotivation, finalDecision.pedagogicalMotivation)
        assertEquals(updatedDecision.structuralMotivationDescription, finalDecision.structuralMotivationDescription)
        assertEquals(updatedDecision.careMotivation, finalDecision.careMotivation)
        assertEquals(updatedDecision.guardianInfo, finalDecision.guardianInfo)
    }

    @Test
    fun `Deleting a decision removes it`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)
        whenDeleteAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)
        whenGetAssistanceNeedDecisionThenExpectNotFound(assistanceNeedDecision.id)
    }

    private fun whenPostAssistanceNeedDecisionThenExpectSuccess(request: AssistanceNeedDecisionRequest): AssistanceNeedDecision {
        val (_, res, result) = http.post("/children/${testChild_1.id}/assistance-needs/decision")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceNeedDecision>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceNeedDecisionThenExpectSuccess(request: AssistanceNeedDecisionRequest, decisionId: AssistanceNeedDecisionId) {
        val (_, res) = http.put("/assistance-need-decision/$decisionId")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun whenGetAssistanceNeedDecisionThenExpectSuccess(id: AssistanceNeedDecisionId?): AssistanceNeedDecision {
        val (_, res, result) = http.get("/assistance-need-decision/$id")
            .asUser(assistanceWorker)
            .responseObject<AssistanceNeedDecisionController.AssistanceNeedDecisionResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get().decision
    }

    private fun whenGetAssistanceNeedDecisionThenExpectNotFound(id: AssistanceNeedDecisionId?) {
        val (_, res) = http.get("/assistance-need-decision/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(404, res.statusCode)
    }

    private fun whenDeleteAssistanceNeedDecisionThenExpectSuccess(id: AssistanceNeedDecisionId?) {
        val (_, res) = http.delete("/assistance-need-decision/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(200, res.statusCode)
    }
}
