// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.process.ArchivedProcess
import fi.espoo.evaka.process.ArchivedProcessHistoryRow
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.ProcessMetadataController.DocumentMetadata
import fi.espoo.evaka.sarma.model.AcceptedFileFormatType
import fi.espoo.evaka.sarma.model.AcceptedMimeTypeType
import fi.espoo.evaka.sarma.model.PersonalDataType
import fi.espoo.evaka.sarma.model.ResourceTypeType
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ArchiveChildDocumentServiceTest {

    private val testDocumentId = ChildDocumentId(UUID.randomUUID())
    private val testDocumentMetadata =
        DocumentMetadata(
            name = "Test Child Document",
            confidential = false,
            createdAt = HelsinkiDateTime.of(LocalDateTime.of(2024, 1, 1, 12, 0)),
            createdBy = DevEmployee().evakaUser,
            downloadPath = "test-document.pdf",
        )
    private val testArchivedProcess =
        ArchivedProcess(
            id = ArchivedProcessId(UUID.randomUUID()),
            history =
                listOf(
                    ArchivedProcessHistoryRow(
                        rowIndex = 0,
                        state = ArchivedProcessState.INITIAL,
                        enteredAt = HelsinkiDateTime.of(LocalDateTime.of(2024, 1, 1, 12, 0)),
                        enteredBy = DevEmployee().evakaUser,
                    ),
                    ArchivedProcessHistoryRow(
                        rowIndex = 0,
                        state = ArchivedProcessState.COMPLETED,
                        enteredAt = HelsinkiDateTime.of(LocalDateTime.of(2024, 1, 2, 12, 0)),
                        enteredBy = DevEmployee().evakaUser,
                    ),
                ),
            processDefinitionNumber = "123",
            year = 2024,
            number = 1,
            organization = "Test Organization",
            archiveDurationMonths = 1200,
        )
    private val testFilename = "test-document.pdf"

    @Test
    fun `createDocumentMetadata creates correct metadata structure`() {
        val metadata =
            ArchiveChildDocumentService.createDocumentMetadata(
                testDocumentId,
                testDocumentMetadata,
                testArchivedProcess,
                testFilename,
            )

        with(metadata) {
            with(standardMetadata) {
                assertEquals("yleinen", metadataMasterVersion.masterName)
                assertEquals("1.0", metadataMasterVersion.versionNumber)
                assertEquals("YLEINEN", virtualArchiveId)
                assertEquals(testDocumentId.toString(), recordIdentifiers.recordIdentifier)

                with(documentDescription) {
                    assertEquals("Test Child Document", title)
                    assertEquals("Suunnitelma", documentType)
                    assertEquals("Varhaiskasvatussuunnitelma", documentTypeSpecifier)
                    assertEquals(PersonalDataType.CONTAINS_PERSONAL_INFORMATION, personalData)
                    assertEquals("fi", language)
                    assertEquals("Palvelujen tiedonhallinta", dataManagement)
                    assertEquals("Varhaiskasvatuksen tietovaranto", dataSource)
                    assertEquals("Varhaiskasvatuksen asiakastietorekisteri", registerName)
                    assertEquals(
                        "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen",
                        personalDataCollectionReason,
                    )

                    assertEquals("Kovakoodattu Erkki", firstName)
                    assertEquals("Kovakoodattu Esimerkki", lastName)
                    assertEquals("Kovakoodattu 010101-123A", socialSecurityNumber)
                }

                with(format) {
                    assertEquals(ResourceTypeType.DIGITAL, recordType)
                    assertEquals(AcceptedMimeTypeType.APPLICATION_PDF, mimeType)
                    assertEquals(testFilename, fileName)
                    assertEquals(AcceptedFileFormatType.PDF, fileFormat)
                }

                with(creation) {
                    assertEquals("Varhaiskasvatuksen toiminnanohjausjärjestelmä", originatingSystem)
                }
            }
        }
    }

    @Test
    fun `marshalMetadata generates correct XML without declaration`() {
        val metadata =
            ArchiveChildDocumentService.createDocumentMetadata(
                testDocumentId,
                testDocumentMetadata,
                testArchivedProcess,
                testFilename,
            )
        val xml = ArchiveChildDocumentService.marshalMetadata(metadata)

        // Verify XML doesn't start with declaration
        assertTrue(!xml.startsWith("<?xml"))

        // Verify namespaces are present with correct prefixes
        assertTrue(xml.contains("ns0:RecordMetadataInstance"))
        assertTrue(
            xml.contains(
                "xmlns:ns0=\"http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0\""
            )
        )
        assertTrue(
            xml.contains("xmlns:ns1=\"http://www.avaintec.com/2004/records-schedule-fi/1.0\"")
        )

        // Verify content
        assertTrue(xml.contains("<ns0:RecordIdentifier>${testDocumentId}</ns0:RecordIdentifier>"))
        assertTrue(xml.contains("<ns0:title>Test Child Document</ns0:title>"))
        assertTrue(xml.contains("<ns0:firstName>Kovakoodattu Erkki</ns0:firstName>"))
        assertTrue(xml.contains("<ns0:lastName>Kovakoodattu Esimerkki</ns0:lastName>"))
    }
}
