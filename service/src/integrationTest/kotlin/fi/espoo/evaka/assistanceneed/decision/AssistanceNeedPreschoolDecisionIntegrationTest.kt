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
import fi.espoo.evaka.assistanceneed.preschooldecision.endActivePreschoolAssistanceDecisions
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.cancelPlacement
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.ProcessMetadataController
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionGuardianId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
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
import org.assertj.core.api.Assertions.assertThat
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
    @Autowired private lateinit var processMetadataController: ProcessMetadataController

    private val assistanceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private val specialEducationTeacherOfTestDaycare =
        DevEmployee(firstName = "Sally", lastName = "SpecialEducationTeacher")

    private val decisionMaker =
        AuthenticatedUser.Employee(
            specialEducationTeacherOfTestDaycare.id,
            setOf(UserRole.SPECIAL_EDUCATION_TEACHER),
        )
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val clock = MockEvakaClock(2024, 3, 5, 13, 30)

    private val testForm =
        AssistanceNeedPreschoolDecisionForm(
            language = OfficialLanguage.FI,
            type = AssistanceNeedPreschoolDecisionType.NEW,
            validFrom = LocalDate.of(2022, 1, 1),
            validTo = null,
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
                        details = "Lots of details",
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
            decisionMakerEmployeeId = specialEducationTeacherOfTestDaycare.id,
            decisionMakerTitle = "Decider of everything",
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testDecisionMaker_1)
            tx.insert(
                specialEducationTeacherOfTestDaycare,
                mapOf(testDaycare.id to UserRole.SPECIAL_EDUCATION_TEACHER),
            )
            tx.insert(testDecisionMaker_3)
            tx.insert(
                unitSupervisorOfTestDaycare,
                mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR),
            )
            tx.insert(testAdult_2, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertGuardian(testAdult_2.id, testChild_1.id)
            tx.insert(admin)

            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5),
                )
            )
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
    fun `Special Education Teacher is not allowed to read own decisions for child no longer placed in her unit`() {
        val assistanceNeedDecision =
            createAndFillDecision(testForm, specialEducationTeacherOfTestDaycare.user)

        assertEquals(
            assistanceNeedDecision.id,
            getDecision(assistanceNeedDecision.id, specialEducationTeacherOfTestDaycare.user).id,
        )

        // remove child placement so child is not in VEO's unit so no document should be visible
        db.transaction { tx ->
                tx.createUpdate {
                    sql("DELETE FROM placement WHERE child_id = ${bind(testChild_1.id)}")
                }
            }
            .execute()

        assertThrows<Forbidden> {
            getDecision(assistanceNeedDecision.id, specialEducationTeacherOfTestDaycare.user)
        }
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
            assistanceNeedDecision.form,
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
        assertEquals(clock.today(), sentDecision.sentForDecision)
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
        assertEquals(clock.today(), decision.decisionMade)
        // decisions cannot be re-decided
        assertThrows<BadRequest> {
            decideDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionStatus.REJECTED,
                decisionMaker,
            )
        }

        asyncJobRunner.runPendingJobsSync(clock)

        val messages = MockSfiMessagesClient.getMessages()
        assertEquals(1, messages.size)
        assertContains(messages[0].messageContent, "päätös tuesta")
        assertEquals(
            "assistance-need-preschool-decisions/assistance_need_preschool_decision_${decision.id}.pdf",
            messages[0].documentKey,
        )

        annulDecision(decision.id, "oops", decisionMaker)
        val annulled = getDecision(decision.id)
        assertEquals(AssistanceNeedDecisionStatus.ANNULLED, annulled.status)
        assertEquals("oops", annulled.annulmentReason)
    }

    @Test
    fun `Metadata is collected`() {
        val assistanceNeedDecision = createAndFillDecision(testForm)
        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        decideDecision(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionStatus.ACCEPTED,
            decisionMaker,
        )
        asyncJobRunner.runPendingJobsSync(clock)

        val metadata =
            processMetadataController
                .getAssistanceNeedPreschoolDecisionMetadata(
                    dbInstance(),
                    admin.user,
                    clock,
                    assistanceNeedDecision.id,
                )
                .data
        assertNotNull(metadata)
        assertEquals("1/123.456.b/2024", metadata.process.processNumber)
        assertEquals(1440, metadata.process.archiveDurationMonths)
        assertEquals("Espoon kaupungin esiopetus ja varhaiskasvatus", metadata.process.organization)
        assertEquals(4, metadata.process.history.size)
        assertEquals(ArchivedProcessState.INITIAL, metadata.process.history[0].state)
        assertEquals(assistanceWorker.evakaUserId, metadata.process.history[0].enteredBy.id)
        assertEquals(ArchivedProcessState.PREPARATION, metadata.process.history[1].state)
        assertEquals(assistanceWorker.evakaUserId, metadata.process.history[1].enteredBy.id)
        assertEquals(ArchivedProcessState.DECIDING, metadata.process.history[2].state)
        assertEquals(decisionMaker.evakaUserId, metadata.process.history[2].enteredBy.id)
        assertEquals(ArchivedProcessState.COMPLETED, metadata.process.history[3].state)
        assertEquals(
            AuthenticatedUser.SystemInternalUser.evakaUserId,
            metadata.process.history[3].enteredBy.id,
        )
        assertEquals("Päätös tuesta esiopetuksessa", metadata.primaryDocument.name)
        assertEquals(assistanceWorker.evakaUserId, metadata.primaryDocument.createdBy?.id)
        assertEquals(true, metadata.primaryDocument.confidential)
        assertEquals(
            "/employee/assistance-need-preschool-decisions/${assistanceNeedDecision.id}/pdf",
            metadata.primaryDocument.downloadPath,
        )
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
                    testDecisionMaker_1.firstName,
                ),
                Tuple(
                    specialEducationTeacherOfTestDaycare.id,
                    specialEducationTeacherOfTestDaycare.lastName,
                    specialEducationTeacherOfTestDaycare.firstName,
                ),
                Tuple(
                    testDecisionMaker_3.id,
                    testDecisionMaker_3.lastName,
                    testDecisionMaker_3.firstName,
                ),
                Tuple(
                    unitSupervisorOfTestDaycare.id,
                    unitSupervisorOfTestDaycare.lastName,
                    unitSupervisorOfTestDaycare.firstName,
                ),
                Tuple(admin.id, admin.lastName, admin.firstName),
            ),
            decisionMakers.map { Tuple(it.id, it.lastName, it.firstName) },
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
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(setOf(testAdult_2.email), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Päätös eVakassa / Beslut i eVaka / Decision on eVaka",
            getEmailFor(testAdult_2).content.subject,
        )
        assertEquals(
            "Test email sender fi <testemail_fi@test.com>",
            getEmailFor(testAdult_2).fromAddress,
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
                validTo = null,
            )

        assertNotNull(pdf)

        val file = File.createTempFile("assistance_need_decision_", ".pdf")

        FileOutputStream(file).use { it.write(pdf) }

        logger.debug { "Generated assistance need decision PDF to ${file.absolutePath}" }
    }

    @Test
    fun `accepting earlier decision works`() {
        val decision1 = createAndFillDecision(testForm.copy(validFrom = LocalDate.of(2022, 1, 1)))
        sendAssistanceNeedDecision(decision1.id)
        decideDecision(decision1.id, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker)

        val decision2 = createAndFillDecision(testForm.copy(validFrom = LocalDate.of(2023, 1, 1)))
        sendAssistanceNeedDecision(decision2.id)
        decideDecision(decision2.id, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker)

        val decision3 = createAndFillDecision(testForm.copy(validFrom = LocalDate.of(2021, 1, 1)))
        sendAssistanceNeedDecision(decision3.id)
        decideDecision(decision3.id, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker)

        val decisions = getDecisionsByChild(testChild_1.id)
        assertThat(decisions)
            .extracting({ it.status }, { it.validFrom }, { it.validTo })
            .containsExactlyInAnyOrder(
                Tuple(
                    AssistanceNeedDecisionStatus.ACCEPTED,
                    LocalDate.of(2021, 1, 1),
                    LocalDate.of(2021, 12, 31),
                ),
                Tuple(
                    AssistanceNeedDecisionStatus.ACCEPTED,
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 12, 31),
                ),
                Tuple(AssistanceNeedDecisionStatus.ACCEPTED, LocalDate.of(2023, 1, 1), null),
            )
    }

    @Test
    fun `endActivePreschoolAssistanceDecisions should not set end date if already set`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 6, 30)
        val today = LocalDate.of(2024, 1, 1)
        val placementId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
            }
        val decision =
            createAndFillDecision(
                    testForm.copy(
                        validFrom = startDate,
                        validTo = null,
                        selectedUnit = testDaycare.id,
                    )
                )
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, LocalDate.of(2022, 6, 30)),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )

        db.transaction { tx ->
            tx.cancelPlacement(placementId)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = LocalDate.of(2022, 12, 31),
                )
            )
        }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, LocalDate.of(2022, 6, 30)),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )
    }

    @Test
    fun `endActivePreschoolAssistanceDecisions should set end date if placement unit changes`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = LocalDate.of(2023, 1, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                )
            )
        }
        val decision =
            createAndFillDecision(
                    testForm.copy(
                        validFrom = startDate,
                        validTo = null,
                        selectedUnit = testDaycare.id,
                    )
                )
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )
    }

    @Test
    fun `endActivePreschoolAssistanceDecisions should not set end date if placement is ending today`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2022, 12, 31)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAndFillDecision(
                    testForm.copy(
                        validFrom = startDate,
                        validTo = null,
                        selectedUnit = testDaycare.id,
                    )
                )
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, null),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )
    }

    @Test
    fun `endActivePreschoolAssistanceDecisions should set end date if placement ended yesterday`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2023, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAndFillDecision(
                    testForm.copy(
                        validFrom = startDate,
                        validTo = null,
                        selectedUnit = testDaycare.id,
                    )
                )
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )
    }

    @Test
    fun `endActivePreschoolAssistanceDecisions should set end date to the end of the last placement if placement type changes to preschool daycare`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = LocalDate.of(2022, 6, 30),
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2022, 7, 1),
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAndFillDecision(
                    testForm.copy(
                        validFrom = startDate,
                        validTo = null,
                        selectedUnit = testDaycare.id,
                    )
                )
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )
    }

    @Test
    fun `endActivePreschoolAssistanceDecisions should not set end date if end date is before start date`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAndFillDecision(
                    testForm.copy(
                        validFrom = endDate.plusDays(1),
                        validTo = null,
                        selectedUnit = testDaycare.id,
                    )
                )
                .id
                .also { sendAssistanceNeedDecision(it) }
                .also { decideDecision(it, AssistanceNeedDecisionStatus.ACCEPTED, decisionMaker) }
                .let { getDecision(it) }

        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(today) }

        assertEquals(
            DateRange(endDate.plusDays(1), null),
            getDecision(decision.id).form.let { DateRange(it.validFrom!!, it.validTo) },
        )
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }

    private fun createEmptyDraft(
        user: AuthenticatedUser.Employee = assistanceWorker
    ): AssistanceNeedPreschoolDecision {
        return assistanceNeedDecisionController.createAssistanceNeedPreschoolDecision(
            dbInstance(),
            user,
            clock,
            testChild_1.id,
        )
    }

    private fun updateDecision(
        form: AssistanceNeedPreschoolDecisionForm,
        decision: AssistanceNeedPreschoolDecision,
        user: AuthenticatedUser.Employee = assistanceWorker,
    ) {
        assistanceNeedDecisionController.updateAssistanceNeedPreschoolDecision(
            dbInstance(),
            user,
            clock,
            decision.id,
            form.copy(
                guardianInfo =
                    setOf(
                        form.guardianInfo.first().copy(id = decision.form.guardianInfo.first().id)
                    )
            ),
        )
    }

    private fun getDecision(
        id: AssistanceNeedPreschoolDecisionId,
        user: AuthenticatedUser.Employee = assistanceWorker,
    ): AssistanceNeedPreschoolDecision {
        return assistanceNeedDecisionController
            .getAssistanceNeedPreschoolDecision(dbInstance(), user, clock, id)
            .decision
    }

    private fun getDecisionsByChild(childId: ChildId): List<AssistanceNeedPreschoolDecisionBasics> {
        return assistanceNeedDecisionController
            .getAssistanceNeedPreschoolDecisions(dbInstance(), assistanceWorker, clock, childId)
            .map { it.decision }
    }

    private fun createAndFillDecision(
        form: AssistanceNeedPreschoolDecisionForm,
        user: AuthenticatedUser.Employee = assistanceWorker,
    ): AssistanceNeedPreschoolDecision {
        return createEmptyDraft(user)
            .also { updateDecision(form, it, user) }
            .let { getDecision(it.id, user) }
    }

    private fun deleteDecision(id: AssistanceNeedPreschoolDecisionId) {
        assistanceNeedDecisionController.deleteAssistanceNeedPreschoolDecision(
            dbInstance(),
            assistanceWorker,
            clock,
            id,
        )
    }

    private fun sendAssistanceNeedDecision(id: AssistanceNeedPreschoolDecisionId) {
        assistanceNeedDecisionController.sendAssistanceNeedPreschoolDecisionForDecision(
            dbInstance(),
            assistanceWorker,
            clock,
            id,
        )
    }

    private fun markAssistanceNeedDecisionOpened(
        id: AssistanceNeedPreschoolDecisionId,
        user: AuthenticatedUser.Employee,
    ) {
        assistanceNeedDecisionController.markAssistanceNeedPreschoolDecisionAsOpened(
            dbInstance(),
            user,
            clock,
            id,
        )
    }

    private fun decideDecision(
        id: AssistanceNeedPreschoolDecisionId,
        status: AssistanceNeedDecisionStatus,
        user: AuthenticatedUser.Employee,
    ) {
        assistanceNeedDecisionController.decideAssistanceNeedPreschoolDecision(
            dbInstance(),
            user,
            clock,
            id,
            AssistanceNeedPreschoolDecisionController.DecideAssistanceNeedPreschoolDecisionRequest(
                status = status
            ),
        )
    }

    private fun annulDecision(
        id: AssistanceNeedPreschoolDecisionId,
        reason: String,
        user: AuthenticatedUser.Employee,
    ) {
        assistanceNeedDecisionController.annulAssistanceNeedPreschoolDecision(
            dbInstance(),
            user,
            clock,
            id,
            AssistanceNeedPreschoolDecisionController.AnnulAssistanceNeedPreschoolDecisionRequest(
                reason = reason
            ),
        )
    }

    private fun getDecisionMakerOptions(
        id: AssistanceNeedPreschoolDecisionId,
        user: AuthenticatedUser.Employee = assistanceWorker,
    ): List<Employee> {
        return assistanceNeedDecisionController.getAssistancePreschoolDecisionMakerOptions(
            dbInstance(),
            user,
            clock,
            id,
        )
    }
}
