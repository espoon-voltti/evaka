// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.ProcessMetadataController
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
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
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_4
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
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import mu.KotlinLogging
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

private val logger = KotlinLogging.logger {}

class AssistanceNeedDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var assistanceNeedDecisionController: AssistanceNeedDecisionController
    @Autowired private lateinit var assistanceNeedDecisionService: AssistanceNeedDecisionService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var processMetadataController: ProcessMetadataController

    private val assistanceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private val specialEducationTeacherOfTestDaycare =
        DevEmployee(firstName = "Sally", lastName = "SpecialEducationTeacher")

    private val decisionMaker =
        AuthenticatedUser.Employee(
            specialEducationTeacherOfTestDaycare.id,
            setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
        )

    private val testAdmin =
        DevEmployee(
            id = EmployeeId(UUID.randomUUID()),
            firstName = "Ad",
            lastName = "Min",
            roles = setOf(UserRole.ADMIN)
        )
    private val admin = AuthenticatedUser.Employee(testAdmin.id, setOf(UserRole.ADMIN))

    private val clock = MockEvakaClock(2024, 3, 5, 13, 30)

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
                    phoneNumber = "01020405060"
                ),
            preparedBy2 = null,
            decisionMaker =
                AssistanceNeedDecisionMakerForm(
                    employeeId = decisionMaker.id,
                    title = "Decider of everything"
                ),
            pedagogicalMotivation = "Pedagogical motivation",
            structuralMotivationOptions =
                StructuralMotivationOptions(
                    smallerGroup = false,
                    specialGroup = true,
                    smallGroup = false,
                    groupAssistant = false,
                    childAssistant = false,
                    additionalStaff = false
                ),
            structuralMotivationDescription = "Structural motivation description",
            careMotivation = "Care motivation",
            serviceOptions =
                ServiceOptions(
                    consultationSpecialEd = false,
                    partTimeSpecialEd = false,
                    fullTimeSpecialEd = false,
                    interpretationAndAssistanceServices = false,
                    specialAides = true
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
                        details = "Lots of details"
                    )
                ),
            viewOfGuardians = "The view of the guardians",
            otherRepresentativeHeard = false,
            otherRepresentativeDetails = null,
            assistanceLevels = setOf(AssistanceLevel.ENHANCED_ASSISTANCE),
            motivationForDecision = "Motivation for decision"
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
                mapOf(testDaycare.id to UserRole.SPECIAL_EDUCATION_TEACHER)
            )
            tx.insert(testDecisionMaker_3)
            tx.insert(
                unitSupervisorOfTestDaycare,
                mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR)
            )
            listOf(testAdult_1, testAdult_4).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insert(testAdmin)
        }
    }

    @Test
    fun `post and get an assistance need decision`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        assertEquals(testChild_1.id, assistanceNeedDecision.child?.id)
        assertEquals(testDecision.validityPeriod, assistanceNeedDecision.validityPeriod)
        assertEquals(testDecision.status, assistanceNeedDecision.status)
        assertEquals(testDecision.language, assistanceNeedDecision.language)
        assertEquals(testDecision.decisionMade, assistanceNeedDecision.decisionMade)
        assertEquals(testDecision.sentForDecision, assistanceNeedDecision.sentForDecision)
        assertEquals(assistanceNeedDecision.selectedUnit?.id, testDaycare.id)
        assertEquals(assistanceNeedDecision.selectedUnit?.name, testDaycare.name)
        assertEquals(assistanceNeedDecision.selectedUnit?.postOffice, "ESPOO")
        assertEquals(assistanceNeedDecision.selectedUnit?.postalCode, "02100")
        assertEquals(assistanceNeedDecision.selectedUnit?.streetAddress, "Joku katu 9")
        assertEquals(
            testDecision.preparedBy1?.employeeId,
            assistanceNeedDecision.preparedBy1?.employeeId
        )
        assertEquals(testDecision.preparedBy1?.title, assistanceNeedDecision.preparedBy1?.title)
        assertEquals(
            testDecision.preparedBy1?.phoneNumber,
            assistanceNeedDecision.preparedBy1?.phoneNumber
        )
        assertEquals(
            assistanceNeedDecision.preparedBy1?.name,
            "${testDecisionMaker_1.firstName} ${testDecisionMaker_1.lastName}"
        )
        assertEquals(assistanceNeedDecision.preparedBy2, null)
        assertEquals(
            testDecision.decisionMaker?.employeeId,
            assistanceNeedDecision.decisionMaker?.employeeId
        )
        assertEquals(testDecision.decisionMaker?.title, assistanceNeedDecision.decisionMaker?.title)
        assertEquals(
            assistanceNeedDecision.decisionMaker?.name,
            "${specialEducationTeacherOfTestDaycare.firstName} ${specialEducationTeacherOfTestDaycare.lastName}"
        )

        assertEquals(
            testDecision.pedagogicalMotivation,
            assistanceNeedDecision.pedagogicalMotivation
        )
        assertEquals(
            testDecision.structuralMotivationOptions,
            assistanceNeedDecision.structuralMotivationOptions
        )
        assertEquals(
            testDecision.structuralMotivationDescription,
            assistanceNeedDecision.structuralMotivationDescription
        )
        assertEquals(testDecision.careMotivation, assistanceNeedDecision.careMotivation)
        assertEquals(testDecision.serviceOptions, assistanceNeedDecision.serviceOptions)
        assertEquals(testDecision.servicesMotivation, assistanceNeedDecision.servicesMotivation)
        assertEquals(
            testDecision.expertResponsibilities,
            assistanceNeedDecision.expertResponsibilities
        )

        assertEquals(testDecision.guardiansHeardOn, assistanceNeedDecision.guardiansHeardOn)
        val storedGuardiansWithoutId =
            assistanceNeedDecision.guardianInfo
                .map { g ->
                    AssistanceNeedDecisionGuardian(
                        id = null,
                        personId = g.personId,
                        name = g.name,
                        isHeard = g.isHeard,
                        details = g.details
                    )
                }
                .toSet()
        assertEquals(testDecision.guardianInfo, storedGuardiansWithoutId)

        assertEquals(testDecision.viewOfGuardians, assistanceNeedDecision.viewOfGuardians)
        assertEquals(
            testDecision.otherRepresentativeHeard,
            assistanceNeedDecision.otherRepresentativeHeard
        )
        assertEquals(
            testDecision.otherRepresentativeDetails,
            assistanceNeedDecision.otherRepresentativeDetails
        )

        assertEquals(testDecision.assistanceLevels, assistanceNeedDecision.assistanceLevels)
        assertEquals(
            testDecision.motivationForDecision,
            assistanceNeedDecision.motivationForDecision
        )
    }

    @Test
    fun `Special Education Teacher is not allowed to read own decisions for child no longer placed in her unit`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        db.transaction {
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5)
                )
            )
        }

        assertEquals(
            assistanceNeedDecision.id,
            getAssistanceNeedDecision(
                    assistanceNeedDecision.id,
                    specialEducationTeacherOfTestDaycare.user
                )
                .id
        )

        // remove child placement so child is not in VEO's unit so no document should be visible
        db.transaction { tx ->
                tx.createUpdate {
                    sql("DELETE FROM placement WHERE child_id = ${bind(testChild_1.id)}")
                }
            }
            .execute()

        assertThrows<Forbidden> {
            getAssistanceNeedDecision(
                assistanceNeedDecision.id,
                specialEducationTeacherOfTestDaycare.user
            )
        }
    }

    @Test
    fun `posting without guardians adds guardians before saving`() {
        val testDecisionWithoutGuardian = testDecision.copy(guardianInfo = setOf())
        val assistanceNeedDecision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(decision = testDecisionWithoutGuardian)
            )
        val firstGuardian = assistanceNeedDecision.guardianInfo.first()
        assertEquals(testAdult_1.id, firstGuardian.personId)
        assertEquals("${testAdult_1.lastName} ${testAdult_1.firstName}", firstGuardian.name)
    }

    @Test
    fun `Updating a decision stores the new information`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))
        val updatedDecision =
            assistanceNeedDecision
                .copy(
                    pedagogicalMotivation = "Such Pedagogical motivation",
                    structuralMotivationOptions =
                        assistanceNeedDecision.structuralMotivationOptions,
                    structuralMotivationDescription = "Very Structural motivation",
                    careMotivation = "wow",
                    guardianInfo =
                        assistanceNeedDecision.guardianInfo
                            .map {
                                AssistanceNeedDecisionGuardian(
                                    id = it.id,
                                    personId = it.personId,
                                    name = it.name,
                                    isHeard = true,
                                    details = "Updated details"
                                )
                            }
                            .toSet()
                )
                .toForm()

        updateAssistanceNeedDecision(
            AssistanceNeedDecisionRequest(decision = updatedDecision),
            assistanceNeedDecision.id
        )

        val finalDecision = getAssistanceNeedDecision(assistanceNeedDecision.id)

        assertEquals(updatedDecision.pedagogicalMotivation, finalDecision.pedagogicalMotivation)
        assertEquals(
            updatedDecision.structuralMotivationDescription,
            finalDecision.structuralMotivationDescription
        )
        assertEquals(updatedDecision.careMotivation, finalDecision.careMotivation)
        assertEquals(updatedDecision.guardianInfo, finalDecision.guardianInfo)
    }

    @Test
    fun `Deleting a decision removes it`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        getAssistanceNeedDecision(assistanceNeedDecision.id)
        deleteAssistanceNeedDecision(assistanceNeedDecision.id)
        assertThrows<NotFound> { getAssistanceNeedDecision(assistanceNeedDecision.id) }
    }

    @Test
    fun `Sending a decision marks the sent date and disables editing and re-sending`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        sendAssistanceNeedDecision(assistanceNeedDecision.id)

        val sentDecision = getAssistanceNeedDecision(assistanceNeedDecision.id)
        assertEquals(clock.today(), sentDecision.sentForDecision)

        assertThrows<Forbidden> { sendAssistanceNeedDecision(assistanceNeedDecision.id) }
        assertThrows<Forbidden> {
            updateAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    decision = assistanceNeedDecision.copy(pedagogicalMotivation = "Test").toForm()
                ),
                assistanceNeedDecision.id
            )
        }
    }

    @Test
    fun `Sent for decision and status cannot be changed using PUT`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        updateAssistanceNeedDecision(
            AssistanceNeedDecisionRequest(
                decision =
                    assistanceNeedDecision
                        .copy(
                            sentForDecision = LocalDate.of(2019, 1, 4),
                            status = AssistanceNeedDecisionStatus.ACCEPTED
                        )
                        .toForm()
            ),
            assistanceNeedDecision.id
        )

        val updatedDecision = getAssistanceNeedDecision(assistanceNeedDecision.id)

        assertEquals(assistanceNeedDecision.sentForDecision, updatedDecision.sentForDecision)
        assertEquals(assistanceNeedDecision.status, updatedDecision.status)
    }

    @Test
    fun `Newly created decisions have a draft status and don't have a sent for decision date`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    decision =
                        testDecision.copy(
                            sentForDecision = LocalDate.of(2019, 1, 4),
                            status = AssistanceNeedDecisionStatus.ACCEPTED
                        )
                )
            )

        assertEquals(null, assistanceNeedDecision.sentForDecision)
        assertEquals(AssistanceNeedDecisionStatus.DRAFT, assistanceNeedDecision.status)
    }

    @Test
    fun `Decision maker can mark decision as opened`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        assertThrows<Forbidden> {
            markAssistanceNeedDecisionOpened(assistanceNeedDecision.id, assistanceWorker)
        }
        markAssistanceNeedDecisionOpened(assistanceNeedDecision.id, decisionMaker)
    }

    @Test
    fun `Decision maker can make a decision`() {
        MockSfiMessagesClient.clearMessages()

        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        // must be sent before a decision can be made
        assertThrows<Forbidden> {
            decideAssistanceNeedDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                    status = AssistanceNeedDecisionStatus.ACCEPTED
                ),
                decisionMaker,
            )
        }
        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        // only the decision-maker can make the decision
        assertThrows<Forbidden> {
            decideAssistanceNeedDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                    status = AssistanceNeedDecisionStatus.ACCEPTED
                ),
                assistanceWorker,
            )
        }
        // the decision cannot be DRAFT
        assertThrows<BadRequest> {
            decideAssistanceNeedDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                    status = AssistanceNeedDecisionStatus.DRAFT
                ),
                decisionMaker,
            )
        }
        decideAssistanceNeedDecision(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
        )
        val decision = getAssistanceNeedDecision(assistanceNeedDecision.id)
        assertEquals(clock.today(), decision.decisionMade)
        // decisions cannot be re-decided
        assertThrows<BadRequest> {
            decideAssistanceNeedDecision(
                assistanceNeedDecision.id,
                AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                    status = AssistanceNeedDecisionStatus.REJECTED
                ),
                decisionMaker,
            )
        }

        asyncJobRunner.runPendingJobsSync(clock)

        val messages = MockSfiMessagesClient.getMessages()
        assertEquals(1, messages.size)
        assertContains(messages[0].messageContent, "päätös tuesta")
        assertEquals(
            "assistance-need-decisions/assistance_need_decision_${assistanceNeedDecision.id}.pdf",
            messages[0].documentKey
        )
    }

    @Test
    fun `Metadata is collected`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        decideAssistanceNeedDecision(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
        )
        asyncJobRunner.runPendingJobsSync(clock)

        val metadata =
            processMetadataController
                .getAssistanceNeedDecisionMetadata(
                    dbInstance(),
                    testAdmin.user,
                    clock,
                    assistanceNeedDecision.id
                )
                .data
        assertNotNull(metadata)
        assertEquals("1/123.456.a/2024", metadata.process.processNumber)
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
            metadata.process.history[3].enteredBy.id
        )
        assertEquals("Päätös tuesta varhaiskasvatuksessa", metadata.primaryDocument.name)
        assertEquals(assistanceWorker.evakaUserId, metadata.primaryDocument.createdBy?.id)
        assertEquals(true, metadata.primaryDocument.confidential)
        assertEquals(
            "/employee/assistance-need-decision/${assistanceNeedDecision.id}/pdf",
            metadata.primaryDocument.downloadPath
        )
    }

    @Test
    fun `Decision maker can be changed`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        val request =
            AssistanceNeedDecisionController.UpdateDecisionMakerForAssistanceNeedDecisionRequest(
                title = "regional manager"
            )

        assertThrows<BadRequest> {
            updateDecisionMakerForAssistanceNeedDecision(
                assistanceNeedDecision.id,
                request,
                admin,
            )
        }
        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        updateDecisionMakerForAssistanceNeedDecision(
            assistanceNeedDecision.id,
            request,
            admin,
        )

        val updatedDecision = getAssistanceNeedDecision(assistanceNeedDecision.id)

        assertEquals(testAdmin.id, updatedDecision.decisionMaker?.employeeId)
    }

    @Test
    fun `decision maker options returns 200 with employees`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        val decisionMakers = getDecisionMakerOptions(assistanceNeedDecision.id)

        assertEquals(
            listOf(
                Tuple(
                    testDecisionMaker_1.id,
                    testDecisionMaker_1.lastName,
                    testDecisionMaker_1.firstName
                ),
                Tuple(
                    specialEducationTeacherOfTestDaycare.id,
                    specialEducationTeacherOfTestDaycare.lastName,
                    specialEducationTeacherOfTestDaycare.firstName
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
                ),
                Tuple(testAdmin.id, testAdmin.lastName, testAdmin.firstName)
            ),
            decisionMakers.map { Tuple(it.id, it.lastName, it.firstName) }
        )
    }

    @Test
    fun `decision maker options returns 404 when assistance decision doesn't exist`() {
        assertThrows<NotFound> {
            getDecisionMakerOptions(
                AssistanceNeedDecisionId(UUID.randomUUID()),
                assistanceWorker,
            )
        }
    }

    @Test
    fun `decision maker options returns 403 when user doesn't have access`() {
        assertThrows<Forbidden> {
            getDecisionMakerOptions(
                AssistanceNeedDecisionId(UUID.randomUUID()),
                decisionMaker,
            )
        }
    }

    @Test
    fun `decision maker options returns employees with given roles`() {
        val directorId =
            db.transaction { tx ->
                tx.insert(
                    DevEmployee(
                        id = EmployeeId(UUID.randomUUID()),
                        firstName = "Fia",
                        lastName = "Finance",
                        roles = setOf(UserRole.FINANCE_ADMIN)
                    )
                )
                tx.insert(
                    DevEmployee(
                        id = EmployeeId(UUID.randomUUID()),
                        firstName = "Dirk",
                        lastName = "Director",
                        roles = setOf(UserRole.DIRECTOR)
                    )
                )
            }
        val assistanceDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        val decisionMakers =
            db.read { tx ->
                assistanceNeedDecisionService.getDecisionMakerOptions(
                    tx,
                    assistanceDecision.id,
                    setOf(UserRole.DIRECTOR, UserRole.UNIT_SUPERVISOR)
                )
            }

        assertEquals(
            listOf(
                Tuple(directorId, "Director", "Dirk"),
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
    fun `End date cannot be changed unless assistance services for time is selected`() {
        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        updateAssistanceNeedDecision(
            AssistanceNeedDecisionRequest(
                decision =
                    assistanceNeedDecision
                        .copy(
                            validityPeriod =
                                testDecision.validityPeriod.copy(end = LocalDate.of(2024, 1, 2)),
                            assistanceLevels = setOf(AssistanceLevel.SPECIAL_ASSISTANCE)
                        )
                        .toForm()
            ),
            assistanceNeedDecision.id
        )

        val updatedDecision = getAssistanceNeedDecision(assistanceNeedDecision.id)

        assertNull(updatedDecision.validityPeriod.end)

        val end = LocalDate.of(2024, 1, 2)

        updateAssistanceNeedDecision(
            AssistanceNeedDecisionRequest(
                decision =
                    assistanceNeedDecision
                        .copy(
                            validityPeriod = testDecision.validityPeriod.copy(end = end),
                            assistanceLevels = setOf(AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME)
                        )
                        .toForm()
            ),
            assistanceNeedDecision.id
        )

        val updatedDecisionWithAssistanceServices =
            getAssistanceNeedDecision(assistanceNeedDecision.id)
        assertEquals(updatedDecisionWithAssistanceServices.validityPeriod.end, end)

        updateAssistanceNeedDecision(
            AssistanceNeedDecisionRequest(
                decision =
                    assistanceNeedDecision
                        .copy(
                            validityPeriod = testDecision.validityPeriod.copy(end = null),
                            assistanceLevels = setOf(AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME)
                        )
                        .toForm()
            ),
            assistanceNeedDecision.id
        )

        assertThrows<BadRequest> { sendAssistanceNeedDecision(assistanceNeedDecision.id) }
    }

    @Test
    fun `Assistance need decision is notified via email to guardians`() {
        db.transaction { it.insertGuardian(testAdult_4.id, testChild_1.id) }

        val assistanceNeedDecision =
            createAssistanceNeedDecision(AssistanceNeedDecisionRequest(decision = testDecision))

        sendAssistanceNeedDecision(assistanceNeedDecision.id)
        decideAssistanceNeedDecision(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
        )
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(setOf(testAdult_4.email), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Päätös eVakassa / Beslut i eVaka / Decision on eVaka",
            getEmailFor(testAdult_4).content.subject
        )
        assertEquals(
            "Test email sender fi <testemail_fi@test.com>",
            getEmailFor(testAdult_4).fromAddress
        )
    }

    @Test
    fun `Decision PDF generation is successful`() {
        val pdf =
            assistanceNeedDecisionService.generatePdf(
                sentDate = LocalDate.now(),
                AssistanceNeedDecision(
                    validityPeriod = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1)),
                    status = AssistanceNeedDecisionStatus.ACCEPTED,
                    language = OfficialLanguage.FI,
                    decisionMade = LocalDate.of(2021, 12, 31),
                    sentForDecision = null,
                    selectedUnit =
                        UnitInfo(
                            id = testDaycare.id,
                            name = "Test",
                            streetAddress = "Mallilankatu 1",
                            postalCode = "00100",
                            postOffice = "Mallila"
                        ),
                    preparedBy1 =
                        AssistanceNeedDecisionEmployee(
                            employeeId = assistanceWorker.id,
                            title = "worker",
                            phoneNumber = "01020405060",
                            name = "Jaakko Jokunen"
                        ),
                    preparedBy2 = null,
                    decisionMaker =
                        AssistanceNeedDecisionMaker(
                            employeeId = decisionMaker.id,
                            title = "Decider of everything",
                            name = "Mikko Mallila"
                        ),
                    pedagogicalMotivation = "Pedagogical motivation",
                    structuralMotivationOptions =
                        StructuralMotivationOptions(
                            smallerGroup = false,
                            specialGroup = true,
                            smallGroup = false,
                            groupAssistant = false,
                            childAssistant = false,
                            additionalStaff = false
                        ),
                    structuralMotivationDescription = "Structural motivation description",
                    careMotivation = "Care motivation",
                    serviceOptions =
                        ServiceOptions(
                            consultationSpecialEd = false,
                            partTimeSpecialEd = false,
                            fullTimeSpecialEd = false,
                            interpretationAndAssistanceServices = false,
                            specialAides = true
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
                                details = "Lots of details"
                            )
                        ),
                    viewOfGuardians = "The view of the guardians",
                    otherRepresentativeHeard = false,
                    otherRepresentativeDetails = null,
                    assistanceLevels = setOf(AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME),
                    motivationForDecision = "Motivation for decision",
                    hasDocument = false,
                    id = AssistanceNeedDecisionId(UUID.randomUUID()),
                    child =
                        AssistanceNeedDecisionChild(
                            id = ChildId(UUID.randomUUID()),
                            name = "Test Example",
                            dateOfBirth = LocalDate.of(2012, 1, 4)
                        ),
                    annulmentReason = ""
                )
            )

        assertNotNull(pdf)

        val file = File.createTempFile("assistance_need_decision_", ".pdf")

        FileOutputStream(file).use { it.write(pdf) }

        logger.debug { "Generated assistance need decision PDF to ${file.absolutePath}" }
    }

    @Test
    fun `accepted assistance need decision can be annulled`() {
        val admin = AuthenticatedUser.Employee(testDecisionMaker_3.id, setOf(UserRole.ADMIN))
        val decision = createAssistanceNeedDecision(AssistanceNeedDecisionRequest(testDecision))

        // Cannot annul a draft
        assertThrows<BadRequest> {
            annulAssistanceNeedDecision(decision.id, "Hupsista keikkaa", admin)
        }

        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
        )
        annulAssistanceNeedDecision(decision.id, "Kaikki oli ihan väärin", decisionMaker)

        val decisionAfter = getAssistanceNeedDecision(decision.id)
        assertEquals(AssistanceNeedDecisionStatus.ANNULLED, decisionAfter.status)
    }

    @Test
    fun `decisions can be made out of order and end dates are automatically set`() {
        val startDate1 = LocalDate.of(2022, 1, 10)
        val startDate2 = LocalDate.of(2022, 1, 20)
        val startDate3 = LocalDate.of(2022, 1, 30)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate1,
                    endDate = startDate3.plusYears(1),
                )
            )
        }

        // note the order
        val decisionIds =
            listOf(startDate1, startDate3, startDate2).map { startDate ->
                val decision =
                    createAssistanceNeedDecision(
                        AssistanceNeedDecisionRequest(
                            testDecision.copy(
                                validityPeriod = DateRange(startDate, null),
                                selectedUnit = UnitIdInfo(testDaycare.id)
                            )
                        )
                    )
                sendAssistanceNeedDecision(decision.id)
                decideAssistanceNeedDecision(
                    decision.id,
                    AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                        status = AssistanceNeedDecisionStatus.ACCEPTED
                    ),
                    decisionMaker
                )
                decision.id
            }

        assertEquals(
            DateRange(startDate1, startDate2.minusDays(1)),
            getAssistanceNeedDecision(decisionIds[0]).validityPeriod
        )
        assertEquals(
            DateRange(startDate3, null),
            getAssistanceNeedDecision(decisionIds[1]).validityPeriod
        )
        assertEquals(
            DateRange(startDate2, startDate3.minusDays(1)),
            getAssistanceNeedDecision(decisionIds[2]).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should not set end date if already set`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(startDate, null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        val updatedEndDate = LocalDate.of(2022, 6, 30)
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                UPDATE assistance_need_decision 
                SET validity_period = ${bind(FiniteDateRange(startDate, updatedEndDate))}
                WHERE id = ${bind(decision.id)}
            """
                )
            }
        }

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, updatedEndDate),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should set end date if placement unit changes`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = LocalDate.of(2023, 1, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                )
            )
        }
        val decision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(startDate, null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should not set end date if placement is ending today`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2022, 12, 31)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(startDate, null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, null),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should set end date if placement ended yesterday`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2023, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(startDate, null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should set end date to the end of the last placement if placement type changes to preschool daycare`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
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
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(startDate, null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should set end date if placement type changes from daycare to preschool`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
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
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2023, 1, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                )
            )
        }
        val decision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(startDate, null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(startDate, endDate),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    @Test
    fun `endActiveDaycareAssistanceDecisions should not set end date if end date is before start date`() {
        val startDate = LocalDate.of(2022, 1, 1)
        val endDate = LocalDate.of(2022, 12, 31)
        val today = LocalDate.of(2024, 1, 1)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val decision =
            createAssistanceNeedDecision(
                AssistanceNeedDecisionRequest(
                    testDecision.copy(
                        validityPeriod = DateRange(endDate.plusDays(1), null),
                        selectedUnit = UnitIdInfo(testDaycare.id)
                    )
                )
            )
        sendAssistanceNeedDecision(decision.id)
        decideAssistanceNeedDecision(
            decision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker
        )

        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(today) }

        assertEquals(
            DateRange(endDate.plusDays(1), null),
            getAssistanceNeedDecision(decision.id).validityPeriod
        )
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }

    private fun createAssistanceNeedDecision(
        request: AssistanceNeedDecisionRequest
    ): AssistanceNeedDecision {
        return assistanceNeedDecisionController.createAssistanceNeedDecision(
            dbInstance(),
            assistanceWorker,
            clock,
            testChild_1.id,
            request
        )
    }

    private fun getAssistanceNeedDecision(
        id: AssistanceNeedDecisionId,
        user: AuthenticatedUser.Employee = assistanceWorker
    ): AssistanceNeedDecision {
        return assistanceNeedDecisionController
            .getAssistanceNeedDecision(dbInstance(), user, clock, id)
            .decision
    }

    private fun deleteAssistanceNeedDecision(id: AssistanceNeedDecisionId) {
        assistanceNeedDecisionController.deleteAssistanceNeedDecision(
            dbInstance(),
            assistanceWorker,
            clock,
            id
        )
    }

    private fun sendAssistanceNeedDecision(
        id: AssistanceNeedDecisionId,
    ) {
        assistanceNeedDecisionController.sendAssistanceNeedDecision(
            dbInstance(),
            assistanceWorker,
            clock,
            id
        )
    }

    private fun updateAssistanceNeedDecision(
        request: AssistanceNeedDecisionRequest,
        decisionId: AssistanceNeedDecisionId
    ) {
        assistanceNeedDecisionController.updateAssistanceNeedDecision(
            dbInstance(),
            assistanceWorker,
            clock,
            decisionId,
            request
        )
    }

    private fun markAssistanceNeedDecisionOpened(
        id: AssistanceNeedDecisionId,
        user: AuthenticatedUser.Employee
    ) {
        assistanceNeedDecisionController.markAssistanceNeedDecisionAsOpened(
            dbInstance(),
            user,
            clock,
            id
        )
    }

    private fun decideAssistanceNeedDecision(
        id: AssistanceNeedDecisionId,
        request: AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest,
        user: AuthenticatedUser.Employee,
    ) {
        assistanceNeedDecisionController.decideAssistanceNeedDecision(
            dbInstance(),
            user,
            clock,
            id,
            request
        )
    }

    private fun updateDecisionMakerForAssistanceNeedDecision(
        id: AssistanceNeedDecisionId,
        request:
            AssistanceNeedDecisionController.UpdateDecisionMakerForAssistanceNeedDecisionRequest,
        user: AuthenticatedUser.Employee,
    ) {
        assistanceNeedDecisionController.updateAssistanceNeedDecisionDecisionMaker(
            dbInstance(),
            user,
            clock,
            id,
            request
        )
    }

    private fun getDecisionMakerOptions(
        id: AssistanceNeedDecisionId,
        user: AuthenticatedUser.Employee = assistanceWorker,
    ): List<Employee> {
        return assistanceNeedDecisionController.getAssistanceDecisionMakerOptions(
            dbInstance(),
            user,
            clock,
            id
        )
    }

    private fun annulAssistanceNeedDecision(
        id: AssistanceNeedDecisionId,
        reason: String,
        user: AuthenticatedUser.Employee
    ) {
        assistanceNeedDecisionController.annulAssistanceNeedDecision(
            dbInstance(),
            user,
            clock,
            id,
            AssistanceNeedDecisionController.AnnulAssistanceNeedDecisionRequest(reason)
        )
    }
}
