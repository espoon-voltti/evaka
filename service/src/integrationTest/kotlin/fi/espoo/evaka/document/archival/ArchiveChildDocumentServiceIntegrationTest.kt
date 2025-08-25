// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.caseprocess.DocumentConfidentiality
import fi.espoo.evaka.caseprocess.insertCaseProcess
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.childdocument.*
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentLocation
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.UiLanguage
import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.ResponseEntity

class ArchiveChildDocumentServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private var documentService = TestDocumentService()
    private var särmäClient = TestSärmäClient()
    private val logger = KotlinLogging.logger {}
    private val logAppender = TestAppender()

    private val templateId = DocumentTemplateId(UUID.randomUUID())
    private val documentId = ChildDocumentId(UUID.randomUUID())
    private val childId = PersonId(UUID.randomUUID())
    private val now = HelsinkiDateTime.of(LocalDateTime.of(2023, 2, 1, 12, 0))

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

    // Client for Särmä archival system that only captures calls for verification
    class TestSärmäClient : SärmäClientInterface {
        data class Call(
            val documentContent: Document,
            val metadataXml: String,
            val masterId: String,
            val classId: String,
            val virtualArchiveId: String,
        )

        val calls = mutableListOf<Call>()
        private var responseCode = 200
        private var responseString =
            "status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&instance_ids=354319&"

        fun setResponse(code: Int, response: String) {
            responseCode = code
            responseString = response
        }

        fun resetResponse() {
            responseCode = 200
            responseString =
                "status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&instance_ids=354319&"
        }

        fun clearCalls() {
            calls.clear()
        }

        override fun putDocument(
            documentContent: Document,
            metadataXml: String,
            masterId: String,
            classId: String,
            virtualArchiveId: String,
        ): Pair<Int, String?> {
            calls.add(Call(documentContent, metadataXml, masterId, classId, virtualArchiveId))
            return Pair(responseCode, responseString)
        }
    }

    @BeforeEach
    fun setUp() {
        // Setup the test appender
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logAppender.start()
        root.addAppender(logAppender)
        logAppender.clear()

        // Reset and clear Särmä client for clean test state
        särmäClient.resetResponse()
        särmäClient.clearCalls()

        // Set up test data
        db.transaction { tx ->
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
            val childDocument =
                DevChildDocument(
                    id = documentId,
                    childId = childId,
                    templateId = templateId,
                    status = DocumentStatus.COMPLETED,
                    content = emptyContent,
                    publishedContent = emptyContent,
                    modifiedAt = now,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = now,
                    contentLockedBy = null,
                    publishedAt = now,
                    publishedBy = employee.evakaUserId,
                    answeredAt = null,
                    answeredBy = null,
                    processId = process.id,
                )
            tx.insert(childDocument)

            // Set document key
            tx.updateChildDocumentKey(documentId, "test-document-key")
        }
    }

    @AfterEach
    fun tearDown() {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.detachAppender(logAppender)
    }

    @Test
    fun `uploadToArchive marks document as archived in database when successful`() {
        // Execute the archive method
        uploadToArchive(db, documentId, särmäClient, documentService, logger)

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNotNull(document.archivedAt)

            // Verify that our test client was used correctly
            assertEquals(1, särmäClient.calls.size)
            assertEquals("yleinen", särmäClient.calls[0].masterId)
            assertEquals("12.06.01.SL1.RT34", särmäClient.calls[0].classId)
            assertEquals("YLEINEN", särmäClient.calls[0].virtualArchiveId)
        }
    }

    @Test
    fun `uploadToArchive does not mark document as archived when Särmä returns error`() {
        // Configure Särmä client to return an validation error response
        särmäClient.setResponse(
            200,
            "status_message=No message associated with the status code.&transaction_id=2868078&protocol_version=1.0&status_code=-2176&",
        )

        // Execute the archive method and expect exception
        assertThrows<RuntimeException> {
            uploadToArchive(db, documentId, särmäClient, documentService, logger)
        }

        // Verify the document was NOT marked as archived in the database
        db.read { tx ->
            // Verify that our test client was called
            assertEquals(1, särmäClient.calls.size)

            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNull(document.archivedAt)
        }
    }

    @Test
    fun `uploadToArchive extracts and logs instance ID from successful response`() {
        // Configure Särmä client with response including instance_ids
        val instanceId = "354319"
        särmäClient.setResponse(
            200,
            "status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&instance_ids=$instanceId&",
        )

        // Execute the archive method
        uploadToArchive(db, documentId, särmäClient, documentService, logger)

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNotNull(document.archivedAt)
        }

        val logContainsInstanceId =
            logAppender.events.any { event ->
                event.argumentArray?.any { arg ->
                    arg.toString().contains("instanceId=$instanceId")
                } ?: false
            }

        assert(logContainsInstanceId) { "Logs should contain the instance ID in its metadata" }
    }

    @Test
    fun `uploadToArchive handles missing instance ID in response`() {
        // Configure Särmä client with response without instance_ids
        särmäClient.setResponse(
            200,
            "status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&",
        )

        // Execute the archive method
        uploadToArchive(db, documentId, särmäClient, documentService, logger)

        // Verify the document was marked as archived in the database
        db.read { tx ->
            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNotNull(document.archivedAt)
        }

        // Verify that an error was logged about the missing instance ID
        val errorMessages =
            logAppender.getErrorMessages().filter { it.contains("No instance ID found") }
        assertEquals(
            1,
            errorMessages.size,
            "Error log message with ERROR level should be created when instance ID is missing",
        )
    }
}
