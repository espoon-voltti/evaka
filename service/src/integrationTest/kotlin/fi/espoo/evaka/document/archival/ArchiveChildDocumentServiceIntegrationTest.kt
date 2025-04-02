// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.childdocument.*
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.process.DocumentConfidentiality
import fi.espoo.evaka.process.insertProcess
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
import fi.espoo.evaka.shared.domain.OfficialLanguage
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.http.ContentDisposition
import org.springframework.http.ResponseEntity

class ArchiveChildDocumentServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private var documentService = TestDocumentService()

    private var särmäClient = TestSärmäClient()

    private val templateId = DocumentTemplateId(UUID.randomUUID())
    private val documentId = ChildDocumentId(UUID.randomUUID())
    private val childId = PersonId(UUID.randomUUID())
    private val now = HelsinkiDateTime.of(LocalDateTime.of(2023, 2, 1, 12, 0))

    // Mock document service implementation
    inner class TestDocumentService : DocumentService {

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

    @BeforeTest
    fun setUp() {
        // Reset and clear Särmä client for clean test state
        särmäClient.resetResponse()
        särmäClient.clearCalls()

        // Set up test data in database using DevApi
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

            // Create a document template using DevDocumentTemplate
            val templateContent = DocumentTemplateContent(sections = emptyList())
            val template =
                DevDocumentTemplate(
                    id = templateId,
                    name = "VASU 2023-2024",
                    type = DocumentType.VASU,
                    placementTypes = setOf(PlacementType.PRESCHOOL),
                    language = OfficialLanguage.FI,
                    confidentiality = DocumentConfidentiality(10, "JulkL 24 § 1 mom. 32 k"),
                    legalBasis = "EARLY_CHILDHOOD_EDUCATION",
                    validity = DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                    published = true,
                    processDefinitionNumber = "1234",
                    archiveDurationMonths = 120,
                    content = templateContent,
                )
            tx.insert(template)

            // Create a child document using DevChildDocument
            val emptyContent = DocumentContent(emptyList())
            val childDocument =
                DevChildDocument(
                    id = documentId,
                    childId = childId,
                    templateId = templateId,
                    status = DocumentStatus.COMPLETED,
                    content = emptyContent,
                    publishedContent = emptyContent,
                    modifiedAt = now,
                    contentModifiedAt = now,
                    contentModifiedBy = null,
                    publishedAt = now,
                    answeredAt = null,
                    answeredBy = null,
                )
            tx.insert(childDocument)

            // Set document key and link to archived process

            // Set document key
            tx.updateChildDocumentKey(documentId, "test-document-key")

            // Create archived process
            val process =
                tx.insertProcess(
                    processDefinitionNumber = "1234",
                    year = 2023,
                    organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                    archiveDurationMonths = 120,
                )

            // Connect document to process
            tx.setChildDocumentProcessId(documentId, process.id)
        }
    }

    @Test
    fun `uploadToArchive marks document as archived in database when successful`() {
        // Execute the archive method on the real service with our test dependencies
        uploadToArchive(db, documentId, särmäClient, documentService)

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
            uploadToArchive(db, documentId, särmäClient, documentService)
        }

        // Verify the document was NOT marked as archived in the database
        db.read { tx ->
            val document = tx.getChildDocument(documentId)
            assertNotNull(document)
            assertNull(document.archivedAt)

            // Verify that our test client was still called
            assertEquals(1, särmäClient.calls.size)
        }
    }
}
