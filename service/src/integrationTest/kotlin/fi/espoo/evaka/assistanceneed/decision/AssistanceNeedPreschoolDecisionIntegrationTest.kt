// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecision
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionBasics
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionForm
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionGuardian
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionService
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionType
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionGuardianId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testDecisionMaker_3
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.*
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import mu.KotlinLogging
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

private val logger = KotlinLogging.logger {}

class AssistanceNeedPreschoolDecisionIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var assistanceNeedDecisionController: AssistanceNeedPreschoolDecisionController
    @Autowired
    private lateinit var assistanceNeedDecisionService: AssistanceNeedPreschoolDecisionService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val assistanceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val decisionMaker =
        AuthenticatedUser.Employee(
            testDecisionMaker_2.id,
            setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
        )

    private val testForm =
        AssistanceNeedPreschoolDecisionForm(
            language = AssistanceNeedDecisionLanguage.FI,
            type = AssistanceNeedPreschoolDecisionType.NEW,
            validFrom = LocalDate.of(2022, 1, 1),
            extendedCompulsoryEducation = true,
            extendedCompulsoryEducationInfo = "extendedCompulsoryEducationInfo",
            grantedAssistanceService = true,
            grantedInterpretationService = true,
            grantedAssistiveDevices = true,
            grantedServicesBasis = "grantedServicesBasis",
            selectedUnit = testDaycare.id,
            primaryGroup = "primaryGroup",
            decisionBasis = "decisionBasis",
            basisDocumentPedagogicalReport = true,
            basisDocumentPsychologistStatement = false,
            basisDocumentSocialReport = false,
            basisDocumentDoctorStatement = true,
            basisDocumentPedagogicalReportDate = LocalDate.of(2021, 11, 30),
            basisDocumentPsychologistStatementDate = null,
            basisDocumentSocialReportDate = null,
            basisDocumentDoctorStatementDate = null,
            basisDocumentOtherOrMissing = true,
            basisDocumentOtherOrMissingInfo = "basisDocumentOtherOrMissingInfo",
            basisDocumentsInfo = "basisDocumentsInfo",
            guardiansHeardOn = LocalDate.of(2021, 11, 30),
            guardianInfo =
                setOf(
                    AssistanceNeedPreschoolDecisionGuardian(
                        id = AssistanceNeedPreschoolDecisionGuardianId(UUID.randomUUID()),
                        personId = testAdult_2.id,
                        name = "${testAdult_2.lastName} ${testAdult_2.firstName}",
                        isHeard = true,
                        details = "Lots of details"
                    )
                ),
            otherRepresentativeHeard = false,
            otherRepresentativeDetails = "",
            viewOfGuardians = "The view of the guardians",
            preparer1EmployeeId = assistanceWorker.id,
            preparer1Title = "worker",
            preparer1PhoneNumber = "01020405060",
            preparer2EmployeeId = null,
            preparer2Title = "",
            preparer2PhoneNumber = "",
            decisionMakerEmployeeId = decisionMaker.id,
            decisionMakerTitle = "Decider of everything"
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertGuardian(testAdult_2.id, testChild_1.id)
        }
    }

    @Test
    fun `guardians are added when draft is initialized`() {
        val assistanceNeedDecision = createEmptyDraft()
        assertEquals(AssistanceNeedDecisionStatus.DRAFT, assistanceNeedDecision.status)
        assertEquals(null, assistanceNeedDecision.sentForDecision)

        val firstGuardian = assistanceNeedDecision.form.guardianInfo.first()
        assertEquals(testAdult_2.id, firstGuardian.personId)
        assertEquals("${testAdult_2.lastName} ${testAdult_2.firstName}", firstGuardian.name)
    }

    @Test
    fun `create, update and get an assistance need decision`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)

        assertEquals(testChild_1.id, assistanceNeedDecision.child.id)
        assertEquals(AssistanceNeedDecisionStatus.DRAFT, assistanceNeedDecision.status)
        assertEquals(
            testForm.copy(
                guardianInfo =
                    setOf(
                        testForm.guardianInfo
                            .first()
                            .copy(id = assistanceNeedDecision.form.guardianInfo.first().id)
                    )
            ),
            assistanceNeedDecision.form
        )
    }

    @Test
    fun `updating only updates one`() {
        val decision1 = createAndFillDecision(testForm)
        val decision2 =
            createAndFillDecision(
                testForm.copy(
                    guardianInfo =
                        testForm.guardianInfo
                            .map {
                                it.copy(
                                    id =
                                        AssistanceNeedPreschoolDecisionGuardianId(
                                            (UUID.randomUUID())
                                        )
                                )
                            }
                            .toSet()
                )
            )

        updateDecision(decision2.form.copy(decisionBasis = "changed"), decision2)

        assertEquals(decision1.form.decisionBasis, getDecision(decision1.id).form.decisionBasis)
        assertEquals("changed", getDecision(decision2.id).form.decisionBasis)
    }

    @Test
    fun `listing decisions`() {
        val expected = createAndFillDecision(testForm)

        val received =
            getDecisionsByChild(expected.child.id).also { assertEquals(1, it.size) }.first()

        assertEquals(expected.id, received.id)
        assertEquals(expected.child.id, received.childId)
        assertEquals(expected.status, received.status)
        assertEquals(expected.form.type, received.type)
        assertEquals(expected.form.validFrom, received.validFrom)
        assertEquals(null, received.validTo)
        assertEquals(UnitInfoBasics(testDaycare.id, testDaycare.name), received.selectedUnit)
        assertEquals(expected.sentForDecision, received.sentForDecision)
        assertEquals(expected.decisionMade, received.decisionMade)
        assertEquals(expected.annulmentReason, received.annulmentReason)
        assertEquals(null, received.unreadGuardianIds)
    }

    @Test
    fun `Deleting a decision removes it`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)

        deleteDecision(assistanceNeedDecision.id)
        assertThrows<NotFound> { getDecision(assistanceNeedDecision.id) }
    }

    @Test
    fun `Sending a decision marks the sent date`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)

        sendAssistanceNeedDecision(assistanceNeedDecision.id)

        val sentDecision = getDecision(assistanceNeedDecision.id)
        assertEquals(LocalDate.now(), sentDecision.sentForDecision)
    }

    @Test
    fun `Decision maker can mark decision as opened`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)

        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        assertThrows<Forbidden> {
            markAssistanceNeedDecisionOpened(assistanceNeedDecision.id, assistanceWorker)
        }
        markAssistanceNeedDecisionOpened(assistanceNeedDecision.id, decisionMaker)
    }

    @Test
    fun `Decision maker can make a decision and annul it`() {
        MockSfiMessagesClient.clearMessages()

        val assistanceNeedDecision = createAndFillDecision(testForm)

        // must be sent before a decision can be made
        assertThrows<Forbidden> {
            decideDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionStatus.ACCEPTED,
                decisionMaker,
            )
        }
        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        // only the decision-maker can make the decision
        assertThrows<Forbidden> {
            decideDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionStatus.ACCEPTED,
                assistanceWorker,
            )
        }
        // the decision cannot be DRAFT
        assertThrows<BadRequest> {
            decideDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionStatus.DRAFT,
                decisionMaker,
            )
        }
        decideDecision(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionStatus.ACCEPTED,
            decisionMaker,
        )
        val decision = getDecision(assistanceNeedDecision.id)
        assertEquals(LocalDate.now(), decision.decisionMade)
        // decisions cannot be re-decided
        assertThrows<BadRequest> {
            decideDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionStatus.REJECTED,
                decisionMaker,
            )
        }

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val messages = MockSfiMessagesClient.getMessages()
        assertEquals(1, messages.size)
        assertContains(messages[0].messageContent, "päätös tuesta")
        assertEquals(
            "assistance-need-preschool-decisions/assistance_need_preschool_decision_${decision.id}.pdf",
            messages[0].documentKey
        )

        annulDecision(decision.id, "oops", decisionMaker)
        val annulled = getDecision(decision.id)
        assertEquals(AssistanceNeedDecisionStatus.ANNULLED, annulled.status)
        assertEquals("oops", annulled.annulmentReason)
    }

    @Test
    fun `decision maker options returns 200 with employees`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)

        val decisionMakers = getDecisionMakerOptions(assistanceNeedDecision.id)

        assertEquals(
            listOf(
                Tuple(
                    testDecisionMaker_1.id,
                    testDecisionMaker_1.lastName,
                    testDecisionMaker_1.firstName
                ),
                Tuple(
                    testDecisionMaker_2.id,
                    testDecisionMaker_2.lastName,
                    testDecisionMaker_2.firstName
                ),
                Tuple(
                    testDecisionMaker_3.id,
                    testDecisionMaker_3.lastName,
                    testDecisionMaker_3.firstName
                ),
                Tuple(
                    unitSupervisorOfTestDaycare.id,
                    unitSupervisorOfTestDaycare.lastName,
                    unitSupervisorOfTestDaycare.firstName
                )
            ),
            decisionMakers.map { Tuple(it.id, it.lastName, it.firstName) }
        )
    }

    @Test
    fun `decision maker options returns 404 when assistance decision doesn't exist`() {
        assertThrows<NotFound> {
            getDecisionMakerOptions(
                AssistanceNeedPreschoolDecisionId(UUID.randomUUID()),
                assistanceWorker,
            )
        }
    }

    @Test
    fun `decision maker options returns 403 when user doesn't have access`() {
        assertThrows<Forbidden> {
            getDecisionMakerOptions(
                AssistanceNeedPreschoolDecisionId(UUID.randomUUID()),
                decisionMaker,
            )
        }
    }

    @Test
    fun `Assistance need decision is notified via email to guardians`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)

        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        decideDecision(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionStatus.ACCEPTED,
            decisionMaker,
        )
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEquals(setOf(testAdult_2.email), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Päätös eVakassa / Beslut i eVaka / Decision on eVaka",
            getEmailFor(testAdult_2).content.subject
        )
        assertEquals(
            "Test email sender fi <testemail_fi@test.com>",
            getEmailFor(testAdult_2).fromAddress
        )
    }

    @Test
    fun `Decision PDF generation is successful`() {
        val decision =
            createAndFillDecision(testForm)
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        val pdf =
            assistanceNeedDecisionService.generatePdf(
                sentDate = LocalDate.now(),
                decision,
                validTo = null
            )

        assertNotNull(pdf)

        val file = File.createTempFile("assistance_need_decision_", ".pdf")

        FileOutputStream(file).use { it.write(pdf) }

        logger.debug { "Generated assistance need decision PDF to ${file.absolutePath}" }
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }

    private fun createEmptyDraft(): AssistanceNeedPreschoolDecision {
        return assistanceNeedDecisionController.createAssistanceNeedPreschoolDecision(
            dbInstance(),
            assistanceWorker,
            RealEvakaClock(),
            testChild_1.id
        )
    }

    private fun updateDecision(
        form: AssistanceNeedPreschoolDecisionForm,
        decision: AssistanceNeedPreschoolDecision
    ) {
        assistanceNeedDecisionController.updateAssistanceNeedPreschoolDecision(
            dbInstance(),
            assistanceWorker,
            RealEvakaClock(),
            decision.id,
            form.copy(
                guardianInfo =
                    setOf(
                        form.guardianInfo.first().copy(id = decision.form.guardianInfo.first().id)
                    )
            )
        )
    }

    private fun getDecision(
        id: AssistanceNeedPreschoolDecisionId
    ): AssistanceNeedPreschoolDecision {
        return assistanceNeedDecisionController
            .getAssistanceNeedPreschoolDecision(
                dbInstance(),
                assistanceWorker,
                RealEvakaClock(),
                id
            )
            .decision
    }

    private fun getDecisionsByChild(childId: ChildId): List<AssistanceNeedPreschoolDecisionBasics> {
        return assistanceNeedDecisionController
            .getAssistanceNeedPreschoolDecisions(
                dbInstance(),
                assistanceWorker,
                RealEvakaClock(),
                childId
            )
            .map { it.decision }
    }

    private fun createAndFillDecision(
        form: AssistanceNeedPreschoolDecisionForm
    ): AssistanceNeedPreschoolDecision {
        return createEmptyDraft().also { updateDecision(form, it) }.let { getDecision(it.id) }
    }

    private fun deleteDecision(id: AssistanceNeedPreschoolDecisionId) {
        assistanceNeedDecisionController.deleteAssistanceNeedPreschoolDecision(
            dbInstance(),
            assistanceWorker,
            RealEvakaClock(),
            id
        )
    }

    private fun sendAssistanceNeedDecision(
        id: AssistanceNeedPreschoolDecisionId,
    ) {
        assistanceNeedDecisionController.sendForDecision(
            dbInstance(),
            assistanceWorker,
            RealEvakaClock(),
            id
        )
    }

    private fun markAssistanceNeedDecisionOpened(
        id: AssistanceNeedPreschoolDecisionId,
        user: AuthenticatedUser
    ) {
        assistanceNeedDecisionController.markAsOpened(dbInstance(), user, RealEvakaClock(), id)
    }

    private fun decideDecision(
        id: AssistanceNeedPreschoolDecisionId,
        status: AssistanceNeedDecisionStatus,
        user: AuthenticatedUser,
    ) {
        assistanceNeedDecisionController.decideAssistanceNeedDecision(
            dbInstance(),
            user,
            RealEvakaClock(),
            id,
            AssistanceNeedPreschoolDecisionController.DecideAssistanceNeedPreschoolDecisionRequest(
                status = status
            )
        )
    }

    private fun annulDecision(
        id: AssistanceNeedPreschoolDecisionId,
        reason: String,
        user: AuthenticatedUser,
    ) {
        assistanceNeedDecisionController.annulAssistanceNeedDecision(
            dbInstance(),
            user,
            RealEvakaClock(),
            id,
            AssistanceNeedPreschoolDecisionController.AnnulAssistanceNeedPreschoolDecisionRequest(
                reason = reason
            )
        )
    }

    private fun getDecisionMakerOptions(
        id: AssistanceNeedPreschoolDecisionId,
        user: AuthenticatedUser = assistanceWorker,
    ): List<Employee> {
        return assistanceNeedDecisionController.getAssistancePreschoolDecisionMakerOptions(
            dbInstance(),
            user,
            RealEvakaClock(),
            id
        )
    }
}
