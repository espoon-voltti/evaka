// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.CheckboxGroupQuestionOption
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentTemplateController
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.RadioButtonGroupQuestionOption
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.DocumentConfidentiality
import fi.espoo.evaka.process.ProcessMetadataController
import fi.espoo.evaka.process.SfiMethod
import fi.espoo.evaka.process.getProcess
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.sficlient.rest.EventType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevSfiMessageEvent
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var controller: ChildDocumentController
    @Autowired lateinit var templateController: DocumentTemplateController
    @Autowired lateinit var metadataController: ProcessMetadataController

    lateinit var areaId: AreaId
    val employeeUser = DevEmployee(roles = setOf(UserRole.ADMIN))
    lateinit var unitSupervisorUser: AuthenticatedUser.Employee

    final val clock = MockEvakaClock(2022, 1, 1, 15, 0)

    final val templateIdPed = DocumentTemplateId(UUID.randomUUID())
    final val templateIdPedagogicalReport = DocumentTemplateId(UUID.randomUUID())
    final val templateIdHojks = DocumentTemplateId(UUID.randomUUID())
    final val templateIdAssistanceDecision = DocumentTemplateId(UUID.randomUUID())

    final val templateContent =
        DocumentTemplateContent(
            sections =
                listOf(
                    Section(
                        id = "s1",
                        label = "Eka",
                        questions =
                            listOf(
                                Question.TextQuestion(id = "q1", label = "kysymys 1"),
                                Question.CheckboxQuestion(id = "q2", label = "kysymys 2"),
                                Question.CheckboxGroupQuestion(
                                    id = "q3",
                                    label = "kysymys 3",
                                    options =
                                        listOf(
                                            CheckboxGroupQuestionOption("a", "eka"),
                                            CheckboxGroupQuestionOption("b", "toka"),
                                            CheckboxGroupQuestionOption("c", "kolmas"),
                                        ),
                                ),
                                Question.RadioButtonGroupQuestion(
                                    id = "q4",
                                    label = "kysymys 4",
                                    options =
                                        listOf(
                                            RadioButtonGroupQuestionOption("a", "eka"),
                                            RadioButtonGroupQuestionOption("b", "toka"),
                                            RadioButtonGroupQuestionOption("c", "kolmas"),
                                        ),
                                ),
                                Question.StaticTextDisplayQuestion(
                                    id = "q5",
                                    label = "tekstikappale",
                                    text = "lorem ipsum",
                                ),
                                Question.DateQuestion(id = "q6", label = "päiväys"),
                                Question.GroupedTextFieldsQuestion(
                                    id = "q7",
                                    label = "vastuullinen",
                                    fieldLabels = listOf("etunimi", "sukunimi"),
                                    allowMultipleRows = false,
                                ),
                                Question.GroupedTextFieldsQuestion(
                                    id = "q8",
                                    label = "huoltajat",
                                    fieldLabels = listOf("etunimi", "sukunimi"),
                                    allowMultipleRows = true,
                                ),
                            ),
                    )
                )
        )

    val devTemplatePed =
        DevDocumentTemplate(
            id = templateIdPed,
            type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
            name = "Pedagoginen arvio 2023",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent,
        )

    val devTemplatePedagogicalReport =
        DevDocumentTemplate(
            id = templateIdPedagogicalReport,
            type = ChildDocumentType.PEDAGOGICAL_REPORT,
            name = "Pedagoginen selvitys 2023",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent,
        )

    val devTemplateHojks =
        DevDocumentTemplate(
            id = templateIdHojks,
            type = ChildDocumentType.HOJKS,
            name = "HOJKS",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent,
            processDefinitionNumber = "123.456.789",
            archiveDurationMonths = 120,
            confidentiality = DocumentConfidentiality(100, "JulkL 24.1 § 25 ja 30 kohdat"),
            archiveExternally = true,
        )

    val devTemplateAssistanceDecision =
        DevDocumentTemplate(
            id = templateIdAssistanceDecision,
            type = ChildDocumentType.OTHER_DECISION,
            name = "Tuenpäätös",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent,
            processDefinitionNumber = "123.456.000",
            archiveDurationMonths = 120,
            confidentiality = DocumentConfidentiality(100, "JulkL 24.1 § 25 ja 30 kohdat"),
        )

    @BeforeEach
    internal fun setUp() {
        MockSfiMessagesClient.reset()
        db.transaction { tx ->
            areaId = tx.insert(testArea)
            val unitId =
                tx.insert(
                    testDaycare.copy(
                        language = Language.sv,
                        enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
                    )
                )
            tx.insert(employeeUser)
            val unitSupervisorId = tx.insert(DevEmployee())
            unitSupervisorUser =
                unitSupervisorId.let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.UNIT_SUPERVISOR))
                }
            tx.insertDaycareAclRow(
                daycareId = unitId,
                employeeId = unitSupervisorId,
                role = UserRole.UNIT_SUPERVISOR,
            )
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5),
                )
            )
            tx.insert(devTemplatePed)
            tx.insert(devTemplatePedagogicalReport)
            tx.insert(devTemplateHojks)
            tx.insert(devTemplateAssistanceDecision)
        }
    }

    @Test
    fun `creating new document and fetching it`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed),
            )

        val document = controller.getDocument(dbInstance(), employeeUser.user, clock, documentId)
        assertEquals(
            ChildDocumentWithPermittedActions(
                data =
                    ChildDocumentDetails(
                        id = documentId,
                        status = DocumentStatus.DRAFT,
                        publishedAt = null,
                        pdfAvailable = false,
                        content = DocumentContent(answers = emptyList()),
                        publishedContent = null,
                        child =
                            ChildBasics(
                                id = testChild_1.id,
                                firstName = testChild_1.firstName,
                                lastName = testChild_1.lastName,
                                dateOfBirth = testChild_1.dateOfBirth,
                            ),
                        template =
                            DocumentTemplate(
                                id = templateIdPed,
                                name = devTemplatePed.name,
                                type = devTemplatePed.type,
                                placementTypes = devTemplatePed.placementTypes,
                                language = devTemplatePed.language,
                                confidentiality = devTemplatePed.confidentiality,
                                legalBasis = devTemplatePed.legalBasis,
                                validity = devTemplatePed.validity,
                                published = devTemplatePed.published,
                                processDefinitionNumber = null,
                                archiveDurationMonths = null,
                                content = templateContent,
                                archiveExternally = false,
                            ),
                        archivedAt = null,
                    ),
                permittedActions =
                    setOf(
                        Action.ChildDocument.DELETE,
                        Action.ChildDocument.PUBLISH,
                        Action.ChildDocument.READ,
                        Action.ChildDocument.UPDATE,
                        Action.ChildDocument.NEXT_STATUS,
                        Action.ChildDocument.PREV_STATUS,
                        Action.ChildDocument.READ_METADATA,
                        Action.ChildDocument.DOWNLOAD,
                        Action.ChildDocument.ARCHIVE,
                        Action.ChildDocument.PROPOSE_DECISION,
                        Action.ChildDocument.ANNUL_DECISION,
                    ),
            ),
            document,
        )

        val summaries =
            controller.getDocuments(dbInstance(), employeeUser.user, clock, testChild_1.id)
        assertEquals(
            listOf(
                ChildDocumentSummary(
                    id = documentId,
                    status = DocumentStatus.DRAFT,
                    type = devTemplatePed.type,
                    templateId = devTemplatePed.id,
                    templateName = devTemplatePed.name,
                    childFirstName = testChild_1.firstName,
                    childLastName = testChild_1.lastName,
                    modifiedAt = clock.now(),
                    publishedAt = null,
                    answeredAt = null,
                    answeredBy = null,
                )
            ),
            summaries.map { it.data },
        )
    }

    @Test
    fun `VEO cannot see own document for child no longer placed in her unit`() {
        val veoInPlacementUnit = DevEmployee()
        db.transaction {
            it.insert(
                veoInPlacementUnit,
                unitRoles = mapOf(testDaycare.id to UserRole.SPECIAL_EDUCATION_TEACHER),
            )
        }

        val documentId =
            controller.createDocument(
                dbInstance(),
                veoInPlacementUnit.user,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed),
            )

        assertEquals(
            documentId,
            controller.getDocument(dbInstance(), veoInPlacementUnit.user, clock, documentId).data.id,
        )
        assertEquals(
            1,
            controller
                .getDocuments(dbInstance(), veoInPlacementUnit.user, clock, testChild_1.id)
                .size,
        )
        // remove child placement so child is not in VEO's unit so no document should be visible
        db.transaction { tx ->
                tx.createUpdate {
                    sql("DELETE FROM placement WHERE child_id = ${bind(testChild_1.id)}")
                }
            }
            .execute()

        assertThrows<Forbidden> {
            controller.getDocument(dbInstance(), veoInPlacementUnit.user, clock, documentId)
        }

        assertThrows<Forbidden> {
            controller.getDocuments(dbInstance(), veoInPlacementUnit.user, clock, testChild_1.id)
        }
    }

    @Test
    fun `creating new document may start a metadata process`() {
        val now1 = clock.now()
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdHojks),
            )

        val document = getDocument(documentId)
        assertEquals(
            DocumentConfidentiality(durationYears = 100, basis = "JulkL 24.1 § 25 ja 30 kohdat"),
            document.template.confidentiality,
        )

        val metadata = getChildDocumentMetadata(documentId).data
        assertNotNull(metadata)
        metadata.also {
            assertEquals("1/123.456.789/2022", it.process.processNumber)
            assertEquals("Espoon kaupungin esiopetus ja varhaiskasvatus", it.process.organization)
            assertEquals(120, it.process.archiveDurationMonths)
            assertEquals("HOJKS", it.primaryDocument.name)
            assertEquals(
                devTemplateHojks.confidentiality?.durationYears,
                it.primaryDocument.confidentiality?.durationYears,
            )
            assertEquals(
                devTemplateHojks.confidentiality?.basis,
                it.primaryDocument.confidentiality?.basis,
            )
            assertNotNull(it.primaryDocument.createdAt)
            assertEquals(employeeUser.evakaUserId, it.primaryDocument.createdBy?.id)
            it.process.history.also { history ->
                assertEquals(1, history.size)
                assertEquals(ArchivedProcessState.INITIAL, history.first().state)
                assertEquals(now1, history.first().enteredAt)
                assertEquals(employeeUser.evakaUserId, history.first().enteredBy.id)
            }
        }

        val clock2 = MockEvakaClock(clock.now().plusHours(1))
        nextState(documentId, DocumentStatus.PREPARED, clock2)
        assertEquals(2, db.read { it.getProcess(metadata.process.id)!!.history }.size)

        val clock3 = MockEvakaClock(clock2.now().plusHours(1))
        nextState(documentId, DocumentStatus.COMPLETED, clock3)
        assertEquals(3, db.read { it.getProcess(metadata.process.id)!!.history }.size)

        val clock4 = MockEvakaClock(clock3.now().plusHours(1))
        prevState(documentId, DocumentStatus.PREPARED, clock4)
        assertEquals(2, db.read { it.getProcess(metadata.process.id)!!.history }.size)

        val clock5 = MockEvakaClock(clock4.now().plusHours(1))
        prevState(documentId, DocumentStatus.DRAFT, clock5)
        val history = db.read { it.getProcess(metadata.process.id)!!.history }
        assertEquals(1, history.size)
        assertEquals(clock.now(), history[0].enteredAt)
        assertEquals(ArchivedProcessState.INITIAL, history[0].state)

        controller.deleteDraftDocument(dbInstance(), employeeUser.user, clock5, documentId)
        assertNull(db.read { it.getProcess(metadata.process.id) })
    }

    @Test
    fun `force unpublishing a template with documents and metadata`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdHojks),
            )
        assertNotNull(getChildDocumentMetadata(documentId).data)

        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser.user,
            clock,
            documentId,
            ChildDocumentController.StatusChangeRequest(DocumentStatus.PREPARED),
        )

        templateController.forceUnpublishTemplate(
            dbInstance(),
            employeeUser.user,
            clock,
            templateIdHojks,
        )

        assertFalse(
            templateController
                .getTemplate(dbInstance(), employeeUser.user, clock, templateIdHojks)
                .published
        )
        assertEquals(
            0,
            controller.getDocuments(dbInstance(), employeeUser.user, clock, testChild_1.id).size,
        )
        assertEquals(
            0,
            db.read {
                it.createQuery { sql("SELECT count(*) FROM archived_process") }.exactlyOne<Int>()
            },
        )
        assertEquals(
            0,
            db.read {
                it.createQuery { sql("SELECT count(*) FROM archived_process_history") }
                    .exactlyOne<Int>()
            },
        )
    }

    @Test
    fun `creating new document not allowed for expired document`() {
        val template2 =
            db.transaction {
                it.insert(
                    DevDocumentTemplate(
                        validity =
                            DateRange(clock.today().minusDays(9), clock.today().minusDays(1)),
                        content = templateContent,
                    )
                )
            }
        assertThrows<BadRequest> {
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, template2),
            )
        }
    }

    @Test
    fun `creating new document not allowed for unpublished document`() {
        val template2 =
            db.transaction {
                it.insert(
                    DevDocumentTemplate(
                        validity = DateRange(clock.today(), clock.today()),
                        published = false,
                        content = templateContent,
                    )
                )
            }
        assertThrows<BadRequest> {
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, template2),
            )
        }
    }

    @Test
    fun `publishing document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        assertNull(db.read { it.getChildDocumentKey(documentId) })

        controller.publishDocument(dbInstance(), employeeUser.user, clock, documentId)
        assertEquals(
            clock.now(),
            controller
                .getDocument(dbInstance(), employeeUser.user, clock, documentId)
                .data
                .publishedAt,
        )

        asyncJobRunner.runPendingJobsSync(clock)
        assertNotNull(db.read { it.getChildDocumentKey(documentId) })

        // republishing after edits regenerates pdf
        updateDocumentContent(
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello"))),
        )
        controller.publishDocument(dbInstance(), employeeUser.user, clock, documentId)
        assertNull(db.read { it.getChildDocumentKey(documentId) })
        asyncJobRunner.runPendingJobsSync(clock)
        assertNotNull(db.read { it.getChildDocumentKey(documentId) })
    }

    @Test
    fun `deleting draft document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        controller.deleteDraftDocument(dbInstance(), employeeUser.user, clock, documentId)
        assertThrows<NotFound> {
            controller.getDocument(dbInstance(), employeeUser.user, clock, documentId)
        }
    }

    @Test
    fun `updating content with all answers`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.TextAnswer("q1", "hello"),
                        AnsweredQuestion.CheckboxAnswer("q2", true),
                        AnsweredQuestion.CheckboxGroupAnswer(
                            "q3",
                            listOf(CheckboxGroupAnswerContent("a"), CheckboxGroupAnswerContent("c")),
                        ),
                        AnsweredQuestion.RadioButtonGroupAnswer("q4", "b"),
                        AnsweredQuestion.StaticTextDisplayAnswer("q5", null),
                        AnsweredQuestion.DateAnswer("q6", LocalDate.of(2022, 1, 7)),
                        AnsweredQuestion.GroupedTextFieldsAnswer(
                            "q7",
                            listOf(listOf("testi", "testaaja")),
                        ),
                        AnsweredQuestion.GroupedTextFieldsAnswer(
                            "q8",
                            listOf(listOf("donald", "duck"), listOf("mickey", "mouse")),
                        ),
                    )
            )
        updateDocumentContent(documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser.user, clock, documentId).data.content,
        )
    }

    @Test
    fun `updating content with partial but valid answers is ok`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        updateDocumentContent(documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser.user, clock, documentId).data.content,
        )
    }

    @Test
    fun `updating content of completed document fails`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser.user,
            clock,
            documentId,
            ChildDocumentController.StatusChangeRequest(DocumentStatus.COMPLETED),
        )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        assertThrows<BadRequest> { updateDocumentContent(documentId, content) }
    }

    @Test
    fun `updating content fails when answering nonexistent question`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q999", "hello")))
        assertThrows<BadRequest> { updateDocumentContent(documentId, content) }
    }

    @Test
    fun `updating content fails when answering question with wrong type`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.CheckboxAnswer("q1", true)))
        assertThrows<BadRequest> { updateDocumentContent(documentId, content) }
    }

    @Test
    fun `updating content fails when answering checkbox group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.CheckboxGroupAnswer(
                            "q3",
                            listOf(CheckboxGroupAnswerContent("a"), CheckboxGroupAnswerContent("d")),
                        )
                    )
            )
        assertThrows<BadRequest> { updateDocumentContent(documentId, content) }
    }

    @Test
    fun `updating content fails when answering radio button group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.RadioButtonGroupAnswer("q3", "d")))
        assertThrows<BadRequest> { updateDocumentContent(documentId, content) }
    }

    @Test
    fun `hojks status flow`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdHojks),
            )
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNull(getDocument(documentId).publishedAt)

        // cannot skip states
        assertThrows<Conflict> { nextState(documentId, DocumentStatus.COMPLETED) }

        nextState(documentId, DocumentStatus.PREPARED)
        assertEquals(DocumentStatus.PREPARED, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)
        nextState(documentId, DocumentStatus.COMPLETED)
        assertEquals(DocumentStatus.COMPLETED, getDocument(documentId).status)
        prevState(documentId, DocumentStatus.PREPARED)
        assertEquals(DocumentStatus.PREPARED, getDocument(documentId).status)
        prevState(documentId, DocumentStatus.DRAFT)
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)

        // no sfi messages are sent
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(0, MockSfiMessagesClient.getMessages().size)
    }

    @Test
    fun `pedagogical doc status flow`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed),
            )
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNull(getDocument(documentId).publishedAt)

        assertThrows<Conflict> { nextState(documentId, DocumentStatus.PREPARED) }

        nextState(documentId, DocumentStatus.COMPLETED)
        assertEquals(DocumentStatus.COMPLETED, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)

        assertThrows<Conflict> { prevState(documentId, DocumentStatus.PREPARED) }

        prevState(documentId, DocumentStatus.DRAFT)
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNotNull(getDocument(documentId).publishedAt)

        // no sfi messages are sent
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(0, MockSfiMessagesClient.getMessages().size)
    }

    @Test
    fun `decision status flow - accept and annul`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdAssistanceDecision),
            )
        assertEquals(DocumentStatus.DRAFT, getDocument(documentId).status)
        assertNull(getDocument(documentId).publishedAt)

        assertThrows<Conflict> { nextState(documentId, DocumentStatus.PREPARED) }
        assertThrows<Conflict> { nextState(documentId, DocumentStatus.COMPLETED) }
        assertThrows<BadRequest> { nextState(documentId, DocumentStatus.DECISION_PROPOSAL) }

        proposeChildDocumentDecision(documentId, unitSupervisorUser.id)
        assertEquals(DocumentStatus.DECISION_PROPOSAL, getDocument(documentId).status)
        assertEquals(unitSupervisorUser.id, getDocument(documentId).decisionMaker)
        assertNull(getDocument(documentId).publishedAt)

        assertThrows<BadRequest> { nextState(documentId, DocumentStatus.COMPLETED) }
        val validity = DateRange(clock.today().plusDays(2), clock.today().plusDays(5))
        // not the assigned decision maker
        assertThrows<Forbidden> {
            acceptChildDocumentDecision(documentId, validity, employeeUser.user)
        }
        acceptChildDocumentDecision(documentId, validity, user = unitSupervisorUser)
        getDocument(documentId).also { doc ->
            assertEquals(DocumentStatus.COMPLETED, doc.status)
            assertEquals(ChildDocumentDecisionStatus.ACCEPTED, doc.decision?.status)
            assertEquals(10_000, doc.decision?.decisionNumber)
            assertNotNull(doc.publishedAt)
        }

        // sfi message was sent
        asyncJobRunner.runPendingJobsSync(clock)
        assertThat(MockSfiMessagesClient.getMessages())
            .extracting({ it.ssn }, { it.messageHeader })
            .containsExactly(
                Tuple(testAdult_1.ssn, "Espoon varhaiskasvatukseen liittyvät päätökset")
            )

        // cannot cancel decision
        assertThrows<BadRequest> { prevState(documentId, DocumentStatus.DECISION_PROPOSAL) }

        annulChildDocumentDecision(documentId, user = unitSupervisorUser)
        getDocument(documentId).also { doc ->
            assertEquals(DocumentStatus.COMPLETED, doc.status)
            assertEquals(ChildDocumentDecisionStatus.ANNULLED, doc.decision?.status)
        }

        val metadata = getChildDocumentMetadata(documentId).data!!
        assertThat(metadata.process.history)
            .extracting({ it.state }, { it.enteredBy.id })
            .containsExactly(
                Tuple(ArchivedProcessState.INITIAL, employeeUser.id),
                Tuple(ArchivedProcessState.PREPARATION, employeeUser.id),
                Tuple(ArchivedProcessState.DECIDING, unitSupervisorUser.id),
                Tuple(
                    ArchivedProcessState.COMPLETED,
                    AuthenticatedUser.SystemInternalUser.evakaUserId,
                ),
            )

        assertThat(metadata.primaryDocument.sfiDeliveries)
            .extracting({ it.recipientName }, { it.method })
            .containsExactly(
                Tuple("${testAdult_1.lastName} ${testAdult_1.firstName}", SfiMethod.PENDING)
            )
        // mock sfi event
        db.transaction { tx ->
            tx.insert(
                DevSfiMessageEvent(
                    messageId = MockSfiMessagesClient.getMessages().first().messageId,
                    eventType = EventType.ELECTRONIC_MESSAGE_CREATED,
                )
            )
        }
        assertThat(getChildDocumentMetadata(documentId).data!!.primaryDocument.sfiDeliveries)
            .extracting({ it.recipientName }, { it.method })
            .containsExactly(
                Tuple("${testAdult_1.lastName} ${testAdult_1.firstName}", SfiMethod.ELECTRONIC)
            )
    }

    @Test
    fun `decision status flow - reject`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdAssistanceDecision),
            )

        proposeChildDocumentDecision(documentId, unitSupervisorUser.id)

        // not the assigned decision maker
        assertThrows<Forbidden> {
            rejectChildDocumentDecision(documentId, user = employeeUser.user)
        }

        rejectChildDocumentDecision(documentId, user = unitSupervisorUser)
        getDocument(documentId).also { doc ->
            assertEquals(DocumentStatus.COMPLETED, doc.status)
            assertEquals(ChildDocumentDecisionStatus.REJECTED, doc.decision?.status)
            assertEquals(10_000, doc.decision?.decisionNumber)
            assertNotNull(doc.publishedAt)
        }

        // sfi message was sent
        asyncJobRunner.runPendingJobsSync(clock)
        assertThat(MockSfiMessagesClient.getMessages())
            .extracting({ it.ssn }, { it.messageHeader })
            .containsExactly(
                Tuple(testAdult_1.ssn, "Espoon varhaiskasvatukseen liittyvät päätökset")
            )

        // cannot cancel decision
        assertThrows<BadRequest> { prevState(documentId, DocumentStatus.DECISION_PROPOSAL) }

        // cannot annul rejected decision
        assertThrows<BadRequest> {
            annulChildDocumentDecision(documentId, user = unitSupervisorUser)
        }
    }

    @Test
    fun `User 1 takes a lock and updates, user 2 cannot do that for 15 minutes`() {
        // user 1 creates at 10:00
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                MockEvakaClock(2022, 1, 1, 10, 0),
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed),
            )

        // user 1 takes a lock at 11:00
        var lock =
            controller.takeDocumentWriteLock(
                dbInstance(),
                employeeUser.user,
                MockEvakaClock(2022, 1, 1, 11, 0),
                documentId,
            )
        assertTrue(lock.lockTakenSuccessfully)
        assertEquals(employeeUser.evakaUserId, lock.currentLock.modifiedBy)
        assertEquals(
            HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 5)),
            lock.currentLock.opensAt,
        )

        // user 1 updates document and re-takes a lock at 11:02
        updateDocumentContent(
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello"))),
            now = MockEvakaClock(2022, 1, 1, 11, 2),
        )

        // user 1 updates document at 11:20, which extends the expired lock as no one else has taken
        // it in between
        updateDocumentContent(
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello2"))),
            now = MockEvakaClock(2022, 1, 1, 11, 20),
        )

        // user 2 tries to take a lock at 11:22, which is not yet possible
        lock =
            controller.takeDocumentWriteLock(
                dbInstance(),
                unitSupervisorUser,
                MockEvakaClock(2022, 1, 1, 11, 22),
                documentId,
            )
        assertFalse(lock.lockTakenSuccessfully)
        assertEquals(employeeUser.evakaUserId, lock.currentLock.modifiedBy)
        assertEquals(
            HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 25)),
            lock.currentLock.opensAt,
        )

        // user 2 tries to update the document at 11:22 without owning the lock, which fails
        assertThrows<Conflict> {
            updateDocumentContent(
                documentId,
                DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello3"))),
                now = MockEvakaClock(2022, 1, 1, 11, 22),
                user = unitSupervisorUser,
            )
        }

        // user 2 takes a lock at 11:27
        lock =
            controller.takeDocumentWriteLock(
                dbInstance(),
                unitSupervisorUser,
                MockEvakaClock(2022, 1, 1, 11, 27),
                documentId,
            )
        assertTrue(lock.lockTakenSuccessfully)
        assertEquals(unitSupervisorUser.evakaUserId, lock.currentLock.modifiedBy)
        assertEquals(
            HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 32)),
            lock.currentLock.opensAt,
        )

        // user 2 updates the document at 11:28
        updateDocumentContent(
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello4"))),
            user = unitSupervisorUser,
            now = MockEvakaClock(2022, 1, 1, 11, 28),
        )

        // user 1 cannot update the document at 11:30
        assertThrows<Conflict> {
            updateDocumentContent(
                documentId,
                DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello5"))),
                now = MockEvakaClock(2022, 1, 1, 11, 30),
            )
        }
    }

    @Test
    fun `unit supervisor doesn't see pedagogical assessment document from duplicate`() {
        val duplicateId =
            db.transaction { tx ->
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val childId =
                    tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5),
                    )
                )
                childId
            }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(duplicateId, templateIdPed),
            )
        assertThrows<Forbidden> {
            controller.getDocument(dbInstance(), unitSupervisorUser, clock, documentId)
        }
    }

    @Test
    fun `unit supervisor doesn't see pedagogical report document from duplicate`() {
        val duplicateId =
            db.transaction { tx ->
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val childId =
                    tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5),
                    )
                )
                childId
            }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(duplicateId, templateIdPedagogicalReport),
            )
        assertThrows<Forbidden> {
            controller.getDocument(dbInstance(), unitSupervisorUser, clock, documentId)
        }
    }

    @Test
    fun `unit supervisor sees hojks document from duplicate`() {
        val duplicateId =
            db.transaction { tx ->
                val unitId =
                    tx.insert(
                        DevDaycare(
                            areaId = areaId,
                            enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
                        )
                    )
                val childId =
                    tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5),
                    )
                )
                childId
            }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(duplicateId, templateIdHojks),
            )
        assertNotNull(controller.getDocument(dbInstance(), unitSupervisorUser, clock, documentId))
    }

    @Test
    fun `archiving document with template not marked for external archiving fails`() {
        // Create document with a template not marked for external archiving (Ped template)
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed),
            )

        // Trying to archive should fail with BadRequest
        assertThrows<BadRequest> {
            controller.planArchiveChildDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                documentId,
                särmäEnabled = true,
            )
        }

        // Create document with a template marked for external archiving (HOJKS template)
        val documentId2 =
            controller.createDocument(
                dbInstance(),
                employeeUser.user,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdHojks),
            )

        // This should not throw an exception
        controller.planArchiveChildDocument(
            dbInstance(),
            employeeUser.user,
            clock,
            documentId2,
            särmäEnabled = true,
        )
    }

    private fun getDocument(id: ChildDocumentId) =
        controller.getDocument(dbInstance(), employeeUser.user, clock, id).data

    private fun updateDocumentContent(
        id: ChildDocumentId,
        content: DocumentContent,
        now: MockEvakaClock = clock,
        user: AuthenticatedUser.Employee = employeeUser.user,
    ) = controller.updateDocumentContent(dbInstance(), user, now, id, content)

    private fun nextState(
        id: ChildDocumentId,
        status: DocumentStatus,
        clockOverride: MockEvakaClock = clock,
    ) =
        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser.user,
            clockOverride,
            id,
            ChildDocumentController.StatusChangeRequest(status),
        )

    private fun prevState(
        id: ChildDocumentId,
        status: DocumentStatus,
        clockOverride: MockEvakaClock = clock,
        user: AuthenticatedUser.Employee = employeeUser.user,
    ) =
        controller.prevDocumentStatus(
            dbInstance(),
            user,
            clockOverride,
            id,
            ChildDocumentController.StatusChangeRequest(status),
        )

    private fun proposeChildDocumentDecision(
        id: ChildDocumentId,
        decisionMaker: EmployeeId,
        clockOverride: MockEvakaClock = clock,
    ) =
        controller.proposeChildDocumentDecision(
            dbInstance(),
            employeeUser.user,
            clockOverride,
            id,
            ChildDocumentController.ProposeChildDocumentDecisionRequest(decisionMaker),
        )

    private fun acceptChildDocumentDecision(
        id: ChildDocumentId,
        validity: DateRange,
        user: AuthenticatedUser.Employee = employeeUser.user,
        clockOverride: MockEvakaClock = clock,
    ) =
        controller.acceptChildDocumentDecision(
            dbInstance(),
            user,
            clockOverride,
            id,
            ChildDocumentController.AcceptChildDocumentDecisionRequest(validity),
        )

    private fun rejectChildDocumentDecision(
        id: ChildDocumentId,
        user: AuthenticatedUser.Employee = employeeUser.user,
        clockOverride: MockEvakaClock = clock,
    ) = controller.rejectChildDocumentDecision(dbInstance(), user, clockOverride, id)

    private fun annulChildDocumentDecision(
        id: ChildDocumentId,
        user: AuthenticatedUser.Employee = employeeUser.user,
        clockOverride: MockEvakaClock = clock,
    ) = controller.annulChildDocumentDecision(dbInstance(), user, clockOverride, id)

    private fun getChildDocumentMetadata(id: ChildDocumentId) =
        metadataController.getChildDocumentMetadata(dbInstance(), employeeUser.user, clock, id)
}
