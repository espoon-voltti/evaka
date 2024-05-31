// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.CheckboxGroupQuestionOption
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.RadioButtonGroupQuestionOption
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.process.ProcessMetadataController
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var controller: ChildDocumentController
    @Autowired lateinit var metadataController: ProcessMetadataController

    lateinit var areaId: AreaId
    lateinit var employeeUser: AuthenticatedUser.Employee
    lateinit var unitSupervisorUser: AuthenticatedUser.Employee

    final val clock = MockEvakaClock(2022, 1, 1, 15, 0)

    final val templateIdPed = DocumentTemplateId(UUID.randomUUID())
    final val templateIdPedagogicalReport = DocumentTemplateId(UUID.randomUUID())
    final val templateIdHojks = DocumentTemplateId(UUID.randomUUID())

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
                                        )
                                ),
                                Question.RadioButtonGroupQuestion(
                                    id = "q4",
                                    label = "kysymys 4",
                                    options =
                                        listOf(
                                            RadioButtonGroupQuestionOption("a", "eka"),
                                            RadioButtonGroupQuestionOption("b", "toka"),
                                            RadioButtonGroupQuestionOption("c", "kolmas"),
                                        )
                                ),
                                Question.StaticTextDisplayQuestion(
                                    id = "q5",
                                    label = "tekstikappale",
                                    text = "lorem ipsum"
                                ),
                                Question.DateQuestion(id = "q6", label = "päiväys"),
                                Question.GroupedTextFieldsQuestion(
                                    id = "q7",
                                    label = "vastuullinen",
                                    fieldLabels = listOf("etunimi", "sukunimi"),
                                    allowMultipleRows = false
                                ),
                                Question.GroupedTextFieldsQuestion(
                                    id = "q8",
                                    label = "huoltajat",
                                    fieldLabels = listOf("etunimi", "sukunimi"),
                                    allowMultipleRows = true
                                ),
                            )
                    )
                )
        )

    val devTemplatePed =
        DevDocumentTemplate(
            id = templateIdPed,
            type = DocumentType.PEDAGOGICAL_ASSESSMENT,
            name = "Pedagoginen arvio 2023",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent
        )

    val devTemplatePedagogicalReport =
        DevDocumentTemplate(
            id = templateIdPedagogicalReport,
            type = DocumentType.PEDAGOGICAL_REPORT,
            name = "Pedagoginen selvitys 2023",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent
        )

    val devTemplateHojks =
        DevDocumentTemplate(
            id = templateIdHojks,
            type = DocumentType.HOJKS,
            name = "HOJKS",
            validity = DateRange(clock.today(), clock.today()),
            content = templateContent,
            processDefinitionNumber = "123.456.789",
            archiveDurationMonths = 120
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            employeeUser =
                tx.insert(DevEmployee()).let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.ADMIN))
                }
            areaId = tx.insert(testArea)
            val unitId =
                tx.insert(
                    testDaycare.copy(
                        language = Language.sv,
                        enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC)
                    )
                )
            val unitSupervisorId = tx.insert(DevEmployee())
            unitSupervisorUser =
                unitSupervisorId.let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.UNIT_SUPERVISOR))
                }
            tx.insertDaycareAclRow(
                daycareId = unitId,
                employeeId = unitSupervisorId,
                role = UserRole.UNIT_SUPERVISOR
            )
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(5)
                )
            )
            tx.insert(devTemplatePed)
            tx.insert(devTemplatePedagogicalReport)
            tx.insert(devTemplateHojks)
        }
    }

    @Test
    fun `creating new document and fetching it`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed)
            )

        val document = controller.getDocument(dbInstance(), employeeUser, clock, documentId)
        assertEquals(
            ChildDocumentWithPermittedActions(
                data =
                    ChildDocumentDetails(
                        id = documentId,
                        status = DocumentStatus.DRAFT,
                        publishedAt = null,
                        content = DocumentContent(answers = emptyList()),
                        publishedContent = null,
                        child =
                            ChildBasics(
                                id = testChild_1.id,
                                firstName = testChild_1.firstName,
                                lastName = testChild_1.lastName,
                                dateOfBirth = testChild_1.dateOfBirth
                            ),
                        template =
                            DocumentTemplate(
                                id = templateIdPed,
                                name = devTemplatePed.name,
                                type = devTemplatePed.type,
                                language = devTemplatePed.language,
                                confidential = devTemplatePed.confidential,
                                legalBasis = devTemplatePed.legalBasis,
                                validity = devTemplatePed.validity,
                                published = devTemplatePed.published,
                                processDefinitionNumber = null,
                                archiveDurationMonths = null,
                                content = templateContent
                            )
                    ),
                permittedActions =
                    setOf(
                        Action.ChildDocument.DELETE,
                        Action.ChildDocument.PUBLISH,
                        Action.ChildDocument.READ,
                        Action.ChildDocument.UPDATE,
                        Action.ChildDocument.NEXT_STATUS,
                        Action.ChildDocument.PREV_STATUS,
                        Action.ChildDocument.READ_METADATA
                    )
            ),
            document
        )

        val summaries = controller.getDocuments(dbInstance(), employeeUser, clock, testChild_1.id)
        assertEquals(
            listOf(
                ChildDocumentSummary(
                    id = documentId,
                    status = DocumentStatus.DRAFT,
                    type = devTemplatePed.type,
                    templateId = devTemplatePed.id,
                    templateName = devTemplatePed.name,
                    modifiedAt = clock.now(),
                    publishedAt = null
                )
            ),
            summaries.map { it.data }
        )
    }

    @Test
    fun `creating new document may start a metadata process`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdHojks)
            )
        val metadata = getChildDocumentMetadata(documentId).data
        assertNotNull(metadata)
        metadata.also {
            assertEquals("1/123.456.789/2022", it.process.processNumber)
            assertEquals("Espoon kaupungin varhaiskasvatus", it.process.organization)
            assertEquals("HOJKS", it.documentName)
            assertEquals(true, it.confidentialDocument)
            assertNotNull(it.documentCreatedAt)
            assertEquals(employeeUser.id, it.documentCreatedBy?.id)
            assertEquals(120, it.archiveDurationMonths)
        }
    }

    @Test
    fun `creating new document not allowed for expired document`() {
        val template2 =
            db.transaction {
                it.insert(
                    DevDocumentTemplate(
                        validity =
                            DateRange(clock.today().minusDays(9), clock.today().minusDays(1)),
                        content = templateContent
                    )
                )
            }
        assertThrows<BadRequest> {
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, template2)
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
                        content = templateContent
                    )
                )
            }
        assertThrows<BadRequest> {
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, template2)
            )
        }
    }

    @Test
    fun `publishing document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        assertNull(db.read { it.getChildDocumentKey(documentId) })

        controller.publishDocument(dbInstance(), employeeUser, clock, documentId)
        assertEquals(
            clock.now(),
            controller.getDocument(dbInstance(), employeeUser, clock, documentId).data.publishedAt
        )

        asyncJobRunner.runPendingJobsSync(clock)
        assertNotNull(db.read { it.getChildDocumentKey(documentId) })

        // republishing after edits regenerates pdf
        controller.updateDocumentContent(
            dbInstance(),
            employeeUser,
            clock,
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        )
        controller.publishDocument(dbInstance(), employeeUser, clock, documentId)
        assertNull(db.read { it.getChildDocumentKey(documentId) })
        asyncJobRunner.runPendingJobsSync(clock)
        assertNotNull(db.read { it.getChildDocumentKey(documentId) })
    }

    @Test
    fun `deleting draft document`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        controller.deleteDraftDocument(dbInstance(), employeeUser, clock, documentId)
        assertThrows<NotFound> {
            controller.getDocument(dbInstance(), employeeUser, clock, documentId)
        }
    }

    @Test
    fun `updating content with all answers`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.TextAnswer("q1", "hello"),
                        AnsweredQuestion.CheckboxAnswer("q2", true),
                        AnsweredQuestion.CheckboxGroupAnswer(
                            "q3",
                            listOf(CheckboxGroupAnswerContent("a"), CheckboxGroupAnswerContent("c"))
                        ),
                        AnsweredQuestion.RadioButtonGroupAnswer("q4", "b"),
                        AnsweredQuestion.StaticTextDisplayAnswer("q5", null),
                        AnsweredQuestion.DateAnswer("q6", LocalDate.of(2022, 1, 7)),
                        AnsweredQuestion.GroupedTextFieldsAnswer(
                            "q7",
                            listOf(listOf("testi", "testaaja"))
                        ),
                        AnsweredQuestion.GroupedTextFieldsAnswer(
                            "q8",
                            listOf(listOf("donald", "duck"), listOf("mickey", "mouse"))
                        )
                    )
            )
        controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser, clock, documentId).data.content
        )
    }

    @Test
    fun `updating content with partial but valid answers is ok`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        assertEquals(
            content,
            controller.getDocument(dbInstance(), employeeUser, clock, documentId).data.content
        )
    }

    @Test
    fun `updating content of completed document fails`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser,
            clock,
            documentId,
            ChildDocumentController.StatusChangeRequest(DocumentStatus.COMPLETED)
        )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering nonexistent question`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q999", "hello")))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering question with wrong type`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content = DocumentContent(answers = listOf(AnsweredQuestion.CheckboxAnswer("q1", true)))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering checkbox group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.CheckboxGroupAnswer(
                            "q3",
                            listOf(CheckboxGroupAnswerContent("a"), CheckboxGroupAnswerContent("d"))
                        )
                    )
            )
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        }
    }

    @Test
    fun `updating content fails when answering radio button group question with unknown option`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
            )
        val content =
            DocumentContent(answers = listOf(AnsweredQuestion.RadioButtonGroupAnswer("q3", "d")))
        assertThrows<BadRequest> {
            controller.updateDocumentContent(dbInstance(), employeeUser, clock, documentId, content)
        }
    }

    @Test
    fun `hojks status flow`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdHojks)
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
    }

    @Test
    fun `pedagogical doc status flow`() {
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(testChild_1.id, templateIdPed)
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
    }

    @Test
    fun `User 1 takes a lock and updates, user 2 cannot do that for 15 minutes`() {
        // user 1 creates at 10:00
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                MockEvakaClock(2022, 1, 1, 10, 0),
                ChildDocumentCreateRequest(childId = testChild_1.id, templateId = templateIdPed)
            )

        // user 1 takes a lock at 11:00
        var lock =
            controller.takeDocumentWriteLock(
                dbInstance(),
                employeeUser,
                MockEvakaClock(2022, 1, 1, 11, 0),
                documentId
            )
        assertTrue(lock.lockTakenSuccessfully)
        assertEquals(employeeUser.id, lock.currentLock.modifiedBy)
        assertEquals(
            HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 5)),
            lock.currentLock.opensAt
        )

        // user 1 updates document and re-takes a lock at 11:02
        controller.updateDocumentContent(
            dbInstance(),
            employeeUser,
            MockEvakaClock(2022, 1, 1, 11, 2),
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello")))
        )

        // user 1 updates document at 11:20, which extends the expired lock as no one else has taken
        // it in between
        controller.updateDocumentContent(
            dbInstance(),
            employeeUser,
            MockEvakaClock(2022, 1, 1, 11, 20),
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello2")))
        )

        // user 2 tries to take a lock at 11:22, which is not yet possible
        lock =
            controller.takeDocumentWriteLock(
                dbInstance(),
                unitSupervisorUser,
                MockEvakaClock(2022, 1, 1, 11, 22),
                documentId
            )
        assertFalse(lock.lockTakenSuccessfully)
        assertEquals(employeeUser.id, lock.currentLock.modifiedBy)
        assertEquals(
            HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 25)),
            lock.currentLock.opensAt
        )

        // user 2 tries to update the document at 11:22 without owning the lock, which fails
        assertThrows<Conflict> {
            controller.updateDocumentContent(
                dbInstance(),
                unitSupervisorUser,
                MockEvakaClock(2022, 1, 1, 11, 22),
                documentId,
                DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello3")))
            )
        }

        // user 2 takes a lock at 11:27
        lock =
            controller.takeDocumentWriteLock(
                dbInstance(),
                unitSupervisorUser,
                MockEvakaClock(2022, 1, 1, 11, 27),
                documentId
            )
        assertTrue(lock.lockTakenSuccessfully)
        assertEquals(unitSupervisorUser.id, lock.currentLock.modifiedBy)
        assertEquals(
            HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(11, 32)),
            lock.currentLock.opensAt
        )

        // user 2 updates the document at 11:28
        controller.updateDocumentContent(
            dbInstance(),
            unitSupervisorUser,
            MockEvakaClock(2022, 1, 1, 11, 28),
            documentId,
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello4")))
        )

        // user 1 cannot update the document at 11:30
        assertThrows<Conflict> {
            controller.updateDocumentContent(
                dbInstance(),
                employeeUser,
                MockEvakaClock(2022, 1, 1, 11, 30),
                documentId,
                DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "hello5")))
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
                        endDate = clock.today().plusDays(5)
                    )
                )
                childId
            }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(duplicateId, templateIdPed)
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
                        endDate = clock.today().plusDays(5)
                    )
                )
                childId
            }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(duplicateId, templateIdPedagogicalReport)
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
                            enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC)
                        )
                    )
                val childId =
                    tx.insert(DevPerson().copy(duplicateOf = testChild_1.id), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(5)
                    )
                )
                childId
            }
        val documentId =
            controller.createDocument(
                dbInstance(),
                employeeUser,
                clock,
                ChildDocumentCreateRequest(duplicateId, templateIdHojks)
            )
        assertNotNull(controller.getDocument(dbInstance(), unitSupervisorUser, clock, documentId))
    }

    private fun getDocument(id: ChildDocumentId) =
        controller.getDocument(dbInstance(), employeeUser, clock, id).data

    private fun nextState(id: ChildDocumentId, status: DocumentStatus) =
        controller.nextDocumentStatus(
            dbInstance(),
            employeeUser,
            clock,
            id,
            ChildDocumentController.StatusChangeRequest(status)
        )

    private fun prevState(id: ChildDocumentId, status: DocumentStatus) =
        controller.prevDocumentStatus(
            dbInstance(),
            employeeUser,
            clock,
            id,
            ChildDocumentController.StatusChangeRequest(status)
        )

    private fun getChildDocumentMetadata(id: ChildDocumentId) =
        metadataController.getChildDocumentMetadata(dbInstance(), employeeUser, clock, id)
}
