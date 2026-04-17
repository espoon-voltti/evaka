// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.archival

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.caseprocess.CaseProcess
import evaka.core.caseprocess.DocumentConfidentiality
import evaka.core.caseprocess.DocumentMetadata
import evaka.core.caseprocess.insertCaseProcess
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.getDecision
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.*
import evaka.core.invoicing.data.getFeeDecision
import evaka.core.invoicing.data.getVoucherValueDecision
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.pis.service.PersonDTO
import evaka.core.placement.PlacementType
import evaka.core.s3.Document
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentLocation
import evaka.core.s3.DocumentService
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DecisionId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.dev.*
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.UiLanguage
import evaka.core.toDaycareFormAdult
import evaka.core.toDaycareFormChild
import evaka.core.user.EvakaUser
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.ResponseEntity

class ArchivalServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private lateinit var clientSpy: ArchivalIntegrationClient
    private var documentService = TestDocumentService()
    private var testArchivalClient = TestArchivalClient()
    private val logAppender = TestAppender()

    private val applicationId = ApplicationId(UUID.randomUUID())
    private val decisionId = DecisionId(UUID.randomUUID())
    private val feeDecisionId = FeeDecisionId(UUID.randomUUID())
    private val voucherValueDecisionId = VoucherValueDecisionId(UUID.randomUUID())
    private val templateId = DocumentTemplateId(UUID.randomUUID())
    private val documentId = ChildDocumentId(UUID.randomUUID())
    private val childId = PersonId(UUID.randomUUID())
    private val now = HelsinkiDateTime.of(LocalDateTime.of(2023, 2, 1, 12, 0))
    private val clock = MockEvakaClock(now)

    private lateinit var archivalService: ArchivalService

    // Simple log appender to capture log events
    class TestAppender : AppenderBase<ILoggingEvent>() {
        val events = mutableListOf<ILoggingEvent>()

        override fun append(eventObject: ILoggingEvent) {
            events.add(eventObject)
        }

        fun getErrorMessages(): List<String> =
            events.filter { it.level == Level.ERROR }.map { it.message }

        fun clear() {
            events.clear()
        }
    }

    // Mock document service implementation
    class TestDocumentService : DocumentService {

        override fun locate(key: DocumentKey): DocumentLocation =
            DocumentLocation(bucket = "test-bucket", key = key.value)

        override fun get(location: DocumentLocation): Document =
            Document("test-document.pdf", byteArrayOf(1, 2, 3, 4), "application/pdf")

        override fun response(
            location: DocumentLocation,
            contentDisposition: ContentDisposition,
        ): ResponseEntity<Any> = throw NotImplementedError("Not used in this test")

        override fun upload(
            location: DocumentLocation,
            inputStream: InputStream,
            size: Long,
            contentType: String,
        ) {}

        override fun delete(location: DocumentLocation) {}
    }

    // Test implementation of ArchivalIntegrationClient that captures calls for verification
    class TestArchivalClient : ArchivalIntegrationClient {
        data class ChildDocumentCall(val documentId: ChildDocumentId, val documentContent: Document)

        val childDocumentCalls = mutableListOf<ChildDocumentCall>()

        fun clear() {
            childDocumentCalls.clear()
        }

        override fun uploadDecisionToArchive(
            caseProcess: CaseProcess,
            child: PersonDTO,
            decision: evaka.core.decision.Decision,
            document: Document,
            user: EvakaUser,
        ): String = "test-instance-id"

        override fun uploadFeeDecisionToArchive(
            caseProcess: CaseProcess,
            decision: FeeDecisionDetailed,
            document: Document,
            user: EvakaUser,
        ): String = "test-instance-id"

        override fun uploadVoucherValueDecisionToArchive(
            caseProcess: CaseProcess,
            decision: VoucherValueDecisionDetailed,
            document: Document,
            user: EvakaUser,
        ): String = "test-instance-id"

        override fun uploadChildDocumentToArchive(
            documentId: ChildDocumentId,
            caseProcess: CaseProcess?,
            childInfo: PersonDTO,
            childDocumentDetails: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            documentContent: Document,
            evakaUser: EvakaUser,
        ): String? {
            childDocumentCalls.add(ChildDocumentCall(documentId, documentContent))
            return "test-instance-id"
        }
    }

    @BeforeEach
    fun setUp() {
        testArchivalClient.clear()
        clientSpy = spy(testArchivalClient)
        archivalService = ArchivalService(null, clientSpy, documentService)

        // Setup the test appender
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logAppender.start()
        root.addAppender(logAppender)
        logAppender.clear()

        // Set up test data
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unit = DevDaycare(areaId = areaId)
            tx.insert(unit)

            // Create a guardian using DevPerson
            val guardian = DevPerson()
            tx.insert(guardian, DevPersonType.ADULT)

            // Create a child using DevPerson
            val personData =
                DevPerson(
                    id = childId,
                    firstName = "Testi",
                    lastName = "Testinen",
                    dateOfBirth = LocalDate.of(2016, 6, 16),
                    ssn = "160616A978U",
                )
            tx.insert(personData, DevPersonType.CHILD)

            // Create a document template
            val templateContent = DocumentTemplateContent(sections = emptyList())
            val template =
                DevDocumentTemplate(
                    id = templateId,
                    name = "VASU 2023-2024",
                    type = ChildDocumentType.VASU,
                    placementTypes = setOf(PlacementType.PRESCHOOL),
                    language = UiLanguage.FI,
                    confidentiality = DocumentConfidentiality(10, "JulkL 24 § 1 mom. 32 k"),
                    legalBasis = "EARLY_CHILDHOOD_EDUCATION",
                    validity = DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                    published = true,
                    processDefinitionNumber = "12.06.01.SL1.RT34",
                    archiveDurationMonths = 120,
                    content = templateContent,
                )
            tx.insert(template)

            // Create archived process
            val process =
                tx.insertCaseProcess(
                    processDefinitionNumber = "12.06.01.SL1.RT34",
                    year = 2023,
                    organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                    archiveDurationMonths = 120,
                )

            val emptyContent = DocumentContent(emptyList())
            val employee = DevEmployee()
            tx.insert(employee)

            // Create an application
            tx.insertTestApplication(
                id = applicationId,
                type = ApplicationType.DAYCARE,
                guardianId = guardian.id,
                childId = childId,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        guardian = guardian.toDaycareFormAdult(),
                        child = personData.toDaycareFormChild(),
                        apply = Apply(preferredUnits = listOf(unit.id)),
                        preferredStartDate = now.toLocalDate(),
                    ),
                processId = process.id,
            )
            tx.insertTestDecision(
                TestDecision(
                    id = decisionId,
                    createdBy = employee.evakaUserId,
                    unitId = unit.id,
                    applicationId = applicationId,
                    type = DecisionType.DAYCARE,
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now(),
                    status = DecisionStatus.ACCEPTED,
                    documentKey = "test-document-key",
                )
            )
            tx.insert(
                DevFeeDecision(
                    id = feeDecisionId,
                    status = FeeDecisionStatus.SENT,
                    validDuring = FiniteDateRange(LocalDate.now(), LocalDate.now()),
                    headOfFamilyId = guardian.id,
                    documentKey = "test-document-key",
                    processId = process.id,
                )
            )
            tx.insert(
                DevVoucherValueDecision(
                    id = voucherValueDecisionId,
                    status = VoucherValueDecisionStatus.SENT,
                    validFrom = LocalDate.now(),
                    validTo = LocalDate.now(),
                    headOfFamilyId = guardian.id,
                    childId = childId,
                    placementUnitId = unit.id,
                    documentKey = "test-document-key",
                    processId = process.id,
                )
            )

            val childDocument =
                DevChildDocument(
                    id = documentId,
                    childId = childId,
                    templateId = templateId,
                    status = DocumentStatus.COMPLETED,
                    content = emptyContent,
                    modifiedAt = now,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = now,
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    processId = process.id,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = now,
                                createdBy = employee.evakaUserId,
                                publishedContent = emptyContent,
                                documentKey = "test-document-key",
                            )
                        ),
                )
            tx.insert(childDocument)
        }
    }

    @AfterEach
    fun tearDown() {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.detachAppender(logAppender)
    }

    @Test
    fun `uploadDecisionToArchive marks document as archived in database when successful`() {
        doReturn("uploadDecisionToArchiveMock")
            .whenever(clientSpy)
            .uploadDecisionToArchive(any(), any(), any(), any(), any())

        // Execute the archive method
        archivalService.uploadDecisionToArchive(db, clock, AsyncJob.ArchiveDecision(decisionId))

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val decision = tx.getDecision(decisionId)
            assertNotNull(decision)
            assertEquals(clock.now(), decision.archivedAt)
        }
    }

    @Test
    fun `uploadDecisionToArchive does not mark document as archived when client throws`() {
        doThrow(RuntimeException("uploadDecisionToArchiveMock"))
            .whenever(clientSpy)
            .uploadDecisionToArchive(any(), any(), any(), any(), any())

        // Execute the archive method and expect exception
        val exception =
            assertThrows<RuntimeException> {
                archivalService.uploadDecisionToArchive(
                    db,
                    clock,
                    AsyncJob.ArchiveDecision(decisionId),
                )
            }
        assertEquals("uploadDecisionToArchiveMock", exception.message)

        // Verify the document was NOT marked as archived in the database
        db.read { tx ->
            val decision = tx.getDecision(decisionId)
            assertNotNull(decision)
            assertNull(decision.archivedAt)
        }
    }

    @Test
    fun `uploadFeeDecisionToArchive marks document as archived in database when successful`() {
        doReturn("uploadFeeDecisionToArchiveMock")
            .whenever(clientSpy)
            .uploadFeeDecisionToArchive(any(), any(), any(), any())

        // Execute the archive method
        archivalService.uploadFeeDecisionToArchive(
            db,
            clock,
            AsyncJob.ArchiveFeeDecision(feeDecisionId),
        )

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val decision = tx.getFeeDecision(feeDecisionId)
            assertNotNull(decision)
            assertEquals(clock.now(), decision.archivedAt)
        }
    }

    @Test
    fun `uploadFeeDecisionToArchive does not mark document as archived when client throws`() {
        doThrow(RuntimeException("uploadFeeDecisionToArchiveMock"))
            .whenever(clientSpy)
            .uploadFeeDecisionToArchive(any(), any(), any(), any())

        // Execute the archive method and expect exception
        val exception =
            assertThrows<RuntimeException> {
                archivalService.uploadFeeDecisionToArchive(
                    db,
                    clock,
                    AsyncJob.ArchiveFeeDecision(feeDecisionId),
                )
            }
        assertEquals("uploadFeeDecisionToArchiveMock", exception.message)

        // Verify the document was NOT marked as archived in the database
        db.read { tx ->
            val decision = tx.getFeeDecision(feeDecisionId)
            assertNotNull(decision)
            assertNull(decision.archivedAt)
        }
    }

    @Test
    fun `uploadVoucherValueDecisionToArchive marks document as archived in database when successful`() {
        doReturn("uploadVoucherValueDecisionToArchiveMock")
            .whenever(clientSpy)
            .uploadVoucherValueDecisionToArchive(any(), any(), any(), any())

        // Execute the archive method
        archivalService.uploadVoucherValueDecisionToArchive(
            db,
            clock,
            AsyncJob.ArchiveVoucherValueDecision(voucherValueDecisionId),
        )

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val decision = tx.getVoucherValueDecision(voucherValueDecisionId)
            assertNotNull(decision)
            assertEquals(clock.now(), decision.archivedAt)
        }
    }

    @Test
    fun `uploadVoucherValueDecisionToArchive does not mark document as archived when client throws`() {
        doThrow(RuntimeException("uploadVoucherValueDecisionToArchiveMock"))
            .whenever(clientSpy)
            .uploadVoucherValueDecisionToArchive(any(), any(), any(), any())

        // Execute the archive method and expect exception
        val exception =
            assertThrows<RuntimeException> {
                archivalService.uploadVoucherValueDecisionToArchive(
                    db,
                    clock,
                    AsyncJob.ArchiveVoucherValueDecision(voucherValueDecisionId),
                )
            }
        assertEquals("uploadVoucherValueDecisionToArchiveMock", exception.message)

        // Verify the document was NOT marked as archived in the database
        db.read { tx ->
            val decision = tx.getVoucherValueDecision(voucherValueDecisionId)
            assertNotNull(decision)
            assertNull(decision.archivedAt)
        }
    }

    @Test
    fun `uploadToArchive marks document as archived in database when successful`() {
        // Execute the archive method
        archivalService.uploadChildDocumentToArchive(db, AsyncJob.ArchiveChildDocument(documentId))

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNotNull(document.archivedAt)

            // Verify that our test client was used
            assertEquals(1, testArchivalClient.childDocumentCalls.size)
        }
    }

    @Test
    fun `uploadToArchive does not mark document as archived when client throws`() {
        doThrow(RuntimeException("Archive upload failed"))
            .whenever(clientSpy)
            .uploadChildDocumentToArchive(any(), any(), any(), any(), any(), any(), any())

        // Execute the archive method and expect exception
        assertThrows<RuntimeException> {
            archivalService.uploadChildDocumentToArchive(
                db,
                AsyncJob.ArchiveChildDocument(documentId),
            )
        }

        // Verify the document was NOT marked as archived in the database
        db.read { tx ->
            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNull(document.archivedAt)
        }
    }

    @Test
    fun `uploadToArchive uses latest version when multiple published versions exist`() {
        val documentId2 = ChildDocumentId(UUID.randomUUID())
        val emptyContent = DocumentContent(answers = emptyList())
        val olderContent =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "old")))
        val latestContent =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "new")))

        db.transaction { tx ->
            val employee = DevEmployee()
            tx.insert(employee)

            val process =
                tx.insertCaseProcess(
                    processDefinitionNumber = "12.06.01.SL1.RT34",
                    year = 2023,
                    organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                    archiveDurationMonths = 120,
                )

            tx.insert(
                DevChildDocument(
                    id = documentId2,
                    childId = childId,
                    status = DocumentStatus.COMPLETED,
                    templateId = templateId,
                    content = latestContent,
                    modifiedAt = now,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = now,
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    processId = process.id,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = now.minusDays(2),
                                createdBy = employee.evakaUserId,
                                publishedContent = emptyContent,
                                documentKey = "old-version-key",
                            ),
                            DevChildDocumentPublishedVersion(
                                versionNumber = 2,
                                createdAt = now.minusDays(1),
                                createdBy = employee.evakaUserId,
                                publishedContent = olderContent,
                                documentKey = "older-version-key",
                            ),
                            DevChildDocumentPublishedVersion(
                                versionNumber = 3,
                                createdAt = now,
                                createdBy = employee.evakaUserId,
                                publishedContent = latestContent,
                                documentKey = "latest-version-key",
                            ),
                        ),
                )
            )
        }

        archivalService.uploadChildDocumentToArchive(db, AsyncJob.ArchiveChildDocument(documentId2))

        // Verify archival was successful
        db.read { tx ->
            val document = tx.getChildDocument(documentId2)
            assertNotNull(document?.archivedAt)
        }

        // Verify the latest version's PDF was used for archival
        assertEquals(1, testArchivalClient.childDocumentCalls.size)
        val archivedDocument = testArchivalClient.childDocumentCalls.first().documentContent
        assertEquals("test-document.pdf", archivedDocument.name)
    }

    @Test
    fun `uploadToArchive fails when latest version has no PDF generated`() {
        val documentId3 = ChildDocumentId(UUID.randomUUID())
        val emptyContent = DocumentContent(answers = emptyList())
        val latestContent =
            DocumentContent(answers = listOf(AnsweredQuestion.TextAnswer("q1", "new")))

        db.transaction { tx ->
            val employee = DevEmployee()
            tx.insert(employee)

            val process =
                tx.insertCaseProcess(
                    processDefinitionNumber = "12.06.01.SL1.RT34",
                    year = 2023,
                    organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                    archiveDurationMonths = 120,
                )

            tx.insert(
                DevChildDocument(
                    id = documentId3,
                    childId = childId,
                    status = DocumentStatus.COMPLETED,
                    templateId = templateId,
                    content = latestContent,
                    modifiedAt = now,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = now,
                    contentLockedBy = null,
                    answeredAt = null,
                    answeredBy = null,
                    processId = process.id,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = now.minusDays(1),
                                createdBy = employee.evakaUserId,
                                publishedContent = emptyContent,
                                documentKey = "old-version-key",
                            ),
                            DevChildDocumentPublishedVersion(
                                versionNumber = 2,
                                createdAt = now,
                                createdBy = employee.evakaUserId,
                                publishedContent = latestContent,
                                documentKey = null, // PDF not generated yet for latest version
                            ),
                        ),
                )
            )
        }

        // Verify that archival fails when latest version has no PDF
        assertThrows<IllegalStateException> {
            archivalService.uploadChildDocumentToArchive(
                db,
                AsyncJob.ArchiveChildDocument(documentId3),
            )
        }

        // Verify the document was NOT marked as archived
        db.read { tx ->
            val document = tx.getChildDocument(documentId3)
            assertNull(document?.archivedAt)
        }
    }
}
