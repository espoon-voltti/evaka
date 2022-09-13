// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testDecisionMaker_3
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val logger = KotlinLogging.logger {}

class AssistanceNeedDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    lateinit var assistanceNeedDecisionService: AssistanceNeedDecisionService

    @Autowired
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val assistanceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val decisionMaker = AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.DIRECTOR))
    private val decisionMaker2 = AuthenticatedUser.Employee(testDecisionMaker_3.id, setOf(UserRole.DIRECTOR))

    private val testDecision = AssistanceNeedDecisionForm(
        validityPeriod = DateRange(LocalDate.of(2022, 1, 1), null),
        status = AssistanceNeedDecisionStatus.DRAFT,
        language = AssistanceNeedDecisionLanguage.FI,
        decisionMade = LocalDate.of(2021, 12, 31),
        sentForDecision = null,
        selectedUnit = UnitIdInfo(id = testDaycare.id),
        preparedBy1 = AssistanceNeedDecisionEmployeeForm(employeeId = assistanceWorker.id, title = "worker", phoneNumber = "01020405060"),
        preparedBy2 = null,
        decisionMaker = AssistanceNeedDecisionMakerForm(
            employeeId = decisionMaker.id,
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

        assistanceLevels = setOf(AssistanceLevel.ENHANCED_ASSISTANCE),
        motivationForDecision = "Motivation for decision"
    )

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
        }
    }

    @Test
    fun `post and get an assistance need decision`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

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
        assertEquals(testDecision.preparedBy1?.employeeId, assistanceNeedDecision.preparedBy1?.employeeId)
        assertEquals(testDecision.preparedBy1?.title, assistanceNeedDecision.preparedBy1?.title)
        assertEquals(testDecision.preparedBy1?.phoneNumber, assistanceNeedDecision.preparedBy1?.phoneNumber)
        assertEquals(assistanceNeedDecision.preparedBy1?.name, "${testDecisionMaker_1.firstName} ${testDecisionMaker_1.lastName}")
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

        assertEquals(testDecision.assistanceLevels, assistanceNeedDecision.assistanceLevels)
        assertEquals(testDecision.motivationForDecision, assistanceNeedDecision.motivationForDecision)
    }

    @Test
    fun `posting without guardians adds guardians before saving`() {
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
            assistanceNeedDecision.id
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

    @Test
    fun `Sending a decision marks the sent date and disables editing and re-sending`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.OK)

        val sentDecision = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)
        assertEquals(LocalDate.now(), sentDecision.sentForDecision)

        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.FORBIDDEN)
        whenPutAssistanceNeedDecisionThenExpectForbidden(
            AssistanceNeedDecisionRequest(
                decision = assistanceNeedDecision.copy(
                    pedagogicalMotivation = "Test"
                ).toForm()
            ),
            assistanceNeedDecision.id
        )
    }

    @Test
    fun `Sent for decision and status cannot be changed using PUT`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        whenPutAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = assistanceNeedDecision.copy(
                    sentForDecision = LocalDate.of(2019, 1, 4),
                    status = AssistanceNeedDecisionStatus.ACCEPTED
                ).toForm()
            ),
            assistanceNeedDecision.id
        )

        val updatedDecision = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)

        assertEquals(assistanceNeedDecision.sentForDecision, updatedDecision.sentForDecision)
        assertEquals(assistanceNeedDecision.status, updatedDecision.status)
    }

    @Test
    fun `Newly created decisions have a draft status and don't have a sent for decision date`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision.copy(
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
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.OK)
        whenMarkAssistanceNeedDecisionOpenedThenExpectStatus(assistanceNeedDecision.id, HttpStatus.FORBIDDEN, assistanceWorker)
        whenMarkAssistanceNeedDecisionOpenedThenExpectStatus(assistanceNeedDecision.id, HttpStatus.OK, decisionMaker)
    }

    @Test
    fun `Decision maker can make a decision`() {
        MockSfiMessagesClient.clearMessages()

        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        // must be sent before a decision can be made
        whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
            HttpStatus.FORBIDDEN
        )
        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.OK)
        // only the decision-maker can make the decision
        whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            assistanceWorker,
            HttpStatus.FORBIDDEN
        )
        // the decision cannot be DRAFT
        whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.DRAFT
            ),
            decisionMaker,
            HttpStatus.BAD_REQUEST
        )
        whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
            HttpStatus.OK
        )
        val decision = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)
        assertEquals(LocalDate.now(), decision.decisionMade)
        // decisions cannot be re-decided
        whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.REJECTED
            ),
            decisionMaker,
            HttpStatus.BAD_REQUEST
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val messages = MockSfiMessagesClient.getMessages()
        assertEquals(1, messages.size)
        assertContains(messages[0].first.messageContent, "päätös tuesta")
        assertNotNull(messages[0].second)
    }

    @Test
    fun `Decision maker can be changed`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        val request = AssistanceNeedDecisionController.UpdateDecisionMakerForAssistanceNeedDecisionRequest(
            title = "regional manager"
        )

        whenUpdateDecisionMakerForAssistanceNeedDecisionThenExpectStatus(
            assistanceNeedDecision.id,
            request,
            decisionMaker2,
            HttpStatus.FORBIDDEN
        )
        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.OK)
        whenUpdateDecisionMakerForAssistanceNeedDecisionThenExpectStatus(
            assistanceNeedDecision.id,
            request,
            decisionMaker2,
            HttpStatus.OK
        )

        val updatedDecision = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)

        assertEquals(decisionMaker2.id, updatedDecision.decisionMaker?.employeeId)
    }

    @Test
    fun `decision maker options returns 200 with employees`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        val decisionMakers = getDecisionMakerOptions(assistanceNeedDecision.id)

        assertThat(decisionMakers)
            .extracting({ it.id }, { it.lastName }, { it.firstName })
            .contains(
                Tuple(testDecisionMaker_1.id, testDecisionMaker_1.lastName, testDecisionMaker_1.firstName),
                Tuple(testDecisionMaker_2.id, testDecisionMaker_2.lastName, testDecisionMaker_2.firstName),
                Tuple(testDecisionMaker_3.id, testDecisionMaker_3.lastName, testDecisionMaker_3.firstName),
            )
    }

    @Test
    fun `decision maker options returns 404 when assistance decision doesn't exist`() {
        whenGetDecisionMakerOptionsThenExpectStatus(
            AssistanceNeedDecisionId(UUID.randomUUID()),
            assistanceWorker,
            HttpStatus.NOT_FOUND
        )
    }

    @Test
    fun `decision maker options returns 403 when user doesn't have access`() {
        whenGetDecisionMakerOptionsThenExpectStatus(
            AssistanceNeedDecisionId(UUID.randomUUID()),
            decisionMaker,
            HttpStatus.FORBIDDEN
        )
    }

    @Test
    fun `decision maker options returns employees with given roles`() {
        val directorId = db.transaction { tx ->
            tx.insertTestEmployee(
                DevEmployee(
                    id = EmployeeId(UUID.randomUUID()),
                    firstName = "Fia",
                    lastName = "Finance",
                    roles = setOf(UserRole.FINANCE_ADMIN),
                )
            )
            tx.insertTestEmployee(
                DevEmployee(
                    id = EmployeeId(UUID.randomUUID()),
                    firstName = "Dirk",
                    lastName = "Director",
                    roles = setOf(UserRole.DIRECTOR),
                )
            )
        }
        val assistanceDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        val decisionMakers = db.read { tx ->
            assistanceNeedDecisionService.getDecisionMakerOptions(
                tx,
                assistanceDecision.id,
                setOf(UserRole.DIRECTOR, UserRole.UNIT_SUPERVISOR)
            )
        }

        assertThat(decisionMakers)
            .extracting({ it.id }, { it.lastName }, { it.firstName })
            .containsExactly(
                Tuple(directorId, "Director", "Dirk"),
                Tuple(unitSupervisorOfTestDaycare.id, "Supervisor", "Sammy"),
            )
    }

    @Test
    fun `End date cannot be changed unless assistance services for time is selected`() {
        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        whenPutAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = assistanceNeedDecision.copy(
                    validityPeriod = testDecision.validityPeriod.copy(end = LocalDate.of(2024, 1, 2)),
                    assistanceLevels = setOf(AssistanceLevel.SPECIAL_ASSISTANCE)
                ).toForm()
            ),
            assistanceNeedDecision.id
        )

        val updatedDecision = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)

        assertNull(updatedDecision.validityPeriod.end)

        val end = LocalDate.of(2024, 1, 2)

        whenPutAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = assistanceNeedDecision.copy(
                    validityPeriod = testDecision.validityPeriod.copy(end = end),
                    assistanceLevels = setOf(AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME)
                ).toForm()
            ),
            assistanceNeedDecision.id
        )

        val updatedDecisionWithAssistanceServices = whenGetAssistanceNeedDecisionThenExpectSuccess(assistanceNeedDecision.id)
        assertEquals(updatedDecisionWithAssistanceServices.validityPeriod.end, end)

        whenPutAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = assistanceNeedDecision.copy(
                    validityPeriod = testDecision.validityPeriod.copy(end = null),
                    assistanceLevels = setOf(AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME)
                ).toForm()
            ),
            assistanceNeedDecision.id
        )

        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Assistance need decision is notified via email to guardians`() {
        db.transaction { it.insertGuardian(testAdult_4.id, testChild_1.id) }

        val assistanceNeedDecision = whenPostAssistanceNeedDecisionThenExpectSuccess(
            AssistanceNeedDecisionRequest(
                decision = testDecision
            )
        )

        whenSendAssistanceNeedDecisionThenExpectStatus(assistanceNeedDecision.id, HttpStatus.OK)
        whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
            assistanceNeedDecision.id,
            AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest(
                status = AssistanceNeedDecisionStatus.ACCEPTED
            ),
            decisionMaker,
            HttpStatus.OK
        )
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEquals(
            setOf(testAdult_4.email),
            MockEmailClient.emails.map { it.toAddress }.toSet()
        )
        assertEquals(
            "Päätös eVakassa / Beslut i eVaka / Decision on eVaka",
            getEmailFor(testAdult_4).subject
        )
        assertEquals("Test email sender fi <testemail_fi@test.com>", getEmailFor(testAdult_4).fromAddress)
    }

    private fun getEmailFor(person: DevPerson): MockEmail {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }

    @Test
    fun `Decision PDF generation is successful`() {
        val pdf = assistanceNeedDecisionService.generatePdf(
            sentDate = LocalDate.now(),
            AssistanceNeedDecision(
                validityPeriod = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1)),
                status = AssistanceNeedDecisionStatus.ACCEPTED,
                language = AssistanceNeedDecisionLanguage.FI,
                decisionMade = LocalDate.of(2021, 12, 31),
                sentForDecision = null,
                selectedUnit = UnitInfo(
                    id = testDaycare.id,
                    name = "Test",
                    streetAddress = "Mallilankatu 1",
                    postalCode = "00100",
                    postOffice = "Mallila"
                ),
                preparedBy1 = AssistanceNeedDecisionEmployee(
                    employeeId = assistanceWorker.id,
                    title = "worker",
                    phoneNumber = "01020405060",
                    name = "Jaakko Jokunen"
                ),
                preparedBy2 = null,
                decisionMaker = AssistanceNeedDecisionMaker(
                    employeeId = decisionMaker.id,
                    title = "Decider of everything",
                    name = "Mikko Mallila"
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

                assistanceLevels = setOf(AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME),
                motivationForDecision = "Motivation for decision",
                hasDocument = false,
                id = AssistanceNeedDecisionId(UUID.randomUUID()),
                child = AssistanceNeedDecisionChild(
                    id = ChildId(UUID.randomUUID()),
                    name = "Test Example",
                    dateOfBirth = LocalDate.of(2012, 1, 4)
                )
            )
        )

        assertNotNull(pdf)

        val file = File.createTempFile("assistance_need_decision_", ".pdf")

        FileOutputStream(file).use {
            it.write(pdf)
        }

        logger.debug { "Generated assistance need decision PDF to ${file.absolutePath}" }
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

    private fun whenSendAssistanceNeedDecisionThenExpectStatus(id: AssistanceNeedDecisionId?, statusCode: HttpStatus) {
        val (_, res) = http.post("/assistance-need-decision/$id/send")
            .asUser(assistanceWorker)
            .response()

        assertEquals(statusCode.value(), res.statusCode)
    }

    private fun whenPutAssistanceNeedDecisionThenExpectForbidden(request: AssistanceNeedDecisionRequest, decisionId: AssistanceNeedDecisionId) {
        val (_, res) = http.put("/assistance-need-decision/$decisionId")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(403, res.statusCode)
    }

    private fun whenMarkAssistanceNeedDecisionOpenedThenExpectStatus(
        id: AssistanceNeedDecisionId?,
        statusCode: HttpStatus,
        user: AuthenticatedUser
    ) {
        val (_, res) = http.post("/assistance-need-decision/$id/mark-as-opened")
            .asUser(user)
            .response()

        assertEquals(statusCode.value(), res.statusCode)
    }

    private fun whenDecideAssistanceNeedDecisionOpenedThenExpectStatus(
        id: AssistanceNeedDecisionId?,
        request: AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest,
        user: AuthenticatedUser,
        statusCode: HttpStatus
    ) {
        val (_, res) = http.post("/assistance-need-decision/$id/decide")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(user)
            .response()

        assertEquals(statusCode.value(), res.statusCode)
    }

    private fun whenUpdateDecisionMakerForAssistanceNeedDecisionThenExpectStatus(
        id: AssistanceNeedDecisionId?,
        request: AssistanceNeedDecisionController.UpdateDecisionMakerForAssistanceNeedDecisionRequest,
        user: AuthenticatedUser,
        statusCode: HttpStatus
    ) {
        val (_, res) = http.post("/assistance-need-decision/$id/update-decision-maker")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(user)
            .response()

        assertEquals(statusCode.value(), res.statusCode)
    }

    private fun getDecisionMakerOptions(id: AssistanceNeedDecisionId): List<Employee> {
        val (_, res, data) = http.get("/assistance-need-decision/$id/decision-maker-option")
            .asUser(assistanceWorker)
            .responseObject<List<Employee>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return data.get()
    }

    private fun whenGetDecisionMakerOptionsThenExpectStatus(
        id: AssistanceNeedDecisionId,
        user: AuthenticatedUser,
        statusCode: HttpStatus = HttpStatus.OK
    ) {
        val (_, res) = http.get("/assistance-need-decision/$id/decision-maker-option")
            .asUser(user)
            .response()

        assertEquals(statusCode.value(), res.statusCode)
    }
}
