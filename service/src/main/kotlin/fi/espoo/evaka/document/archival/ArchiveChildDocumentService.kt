// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.document.childdocument.getChildDocument
import fi.espoo.evaka.document.childdocument.getChildDocumentKey
import fi.espoo.evaka.document.childdocument.markDocumentAsArchived
import fi.espoo.evaka.process.ArchivedProcess
import fi.espoo.evaka.process.ProcessMetadataController
import fi.espoo.evaka.process.getArchiveProcessByChildDocumentId
import fi.espoo.evaka.process.getChildDocumentMetadata
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.sarma.model.*
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import java.io.StringWriter
import java.time.ZoneId
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import org.springframework.stereotype.Service
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

@Service
class ArchiveChildDocumentService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val documentClient: DocumentService,
    private val client: SärmäClientInterface,
) {

    init {
        asyncJobRunner.registerHandler<AsyncJob.ArchiveChildDocument> { db, _, msg ->
            uploadToArchive(db, msg.documentId)
        }
    }

    companion object {
        internal fun marshalMetadata(metadata: RecordMetadataInstance): String {
            val context = JAXBContext.newInstance(RecordMetadataInstance::class.java)
            val marshaller =
                context.createMarshaller().apply {
                    setProperty(Marshaller.JAXB_FRAGMENT, true) // Remove XML declaration
                }
            return StringWriter().use { writer ->
                marshaller.marshal(metadata, writer)
                writer.toString()
            }
        }

        internal fun createDocumentMetadata(
            document: ChildDocumentDetails,
            documentMetadata: ProcessMetadataController.DocumentMetadata,
            archivedProcess: ArchivedProcess?,
            filename: String,
        ): RecordMetadataInstance {
            // TODO TOS numero + vuosi koodi täytyis laittaa johonkin
            return RecordMetadataInstance().apply {
                standardMetadata =
                    StandardMetadataType().apply {
                        metadataMasterVersion =
                            MetadataMasterVersionType().apply {
                                masterName = "yleinen"
                                versionNumber = "1.0"
                            }
                        virtualArchiveId = "YLEINEN"
                        recordIdentifiers =
                            StandardMetadataType.RecordIdentifiers().apply {
                                recordIdentifier = document.id.toString()
                            }
                        documentDescription =
                            StandardMetadataType.DocumentDescription().apply {
                                // Basic document info
                                title = documentMetadata.name
                                documentType =
                                    "Suunnitelma" // From Evaka_Särmä_metatietomääritykset.xlsx
                                documentTypeSpecifier =
                                    "Varhaiskasvatussuunnitelma" // From
                                                                 // Evaka_Särmä_metatietomääritykset.xlsx
                                personalData = PersonalDataType.CONTAINS_PERSONAL_INFORMATION
                                language = document.template.language.isoLanguage.alpha2

                                // Data management fields
                                dataManagement = "Palvelujen tiedonhallinta"
                                dataSource = "Varhaiskasvatuksen tietovaranto"
                                registerName = "Varhaiskasvatuksen asiakastietorekisteri"
                                personalDataCollectionReason =
                                    "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen"

                                // Child's information
                                firstName = document.child.firstName
                                lastName = document.child.lastName
                                socialSecurityNumber =
                                    "Kovakoodattu 010101-123A" // TODO read from document.child
                                // TODO add bd once schema field is known
                                // birthDate?.let { birthDate = it.toString() }
                            }
                        format =
                            StandardMetadataType.Format().apply {
                                recordType = ResourceTypeType.DIGITAL
                                mimeType = AcceptedMimeTypeType.APPLICATION_PDF
                                fileName = filename
                                fileFormat = AcceptedFileFormatType.PDF
                            }

                        // Creation and handling information
                        creation =
                            StandardMetadataType.Creation().apply {
                                created = documentMetadata.createdAt?.toLocalDateTime()
                                    ?.atZone(ZoneId.of("Europe/Helsinki"))
                                    ?.let { zdt ->
                                        DatatypeFactory.newInstance().newXMLGregorianCalendar(
                                            zdt.year,
                                            zdt.monthValue,
                                            zdt.dayOfMonth,
                                            zdt.hour,
                                            zdt.minute,
                                            zdt.second,
                                            zdt.nano / 1000000,
                                            zdt.offset.totalSeconds / 60,
                                        )
                                    }
                                originatingSystem = "Varhaiskasvatuksen toiminnanohjausjärjestelmä"
                            }

                        // Document handlers/agents
                        // TODO map from archivedProcess?.history
                        //                        agents =
                        //                            StandardMetadataType.Agents().apply {
                        //                                agent.add(
                        //
                        // StandardMetadataType.Agents.Agent().apply {
                        //                                        corporateName = "Suomenkielisen
                        // varhaiskasvatuksen tulosyksikkö"
                        //                                        role =
                        // documentMetadata.handlerRole
                        //                                        name =
                        // documentMetadata.handlerName
                        //                                    }
                        //                                )
                        //                            }

                        // Security and retention policies
                        //                        disclosurePolicy =
                        //                            StandardMetadataType.DisclosurePolicy().apply
                        // {
                        //                                value = if
                        // (documentMetadata.isConfidential == true) "confidential" else "public"
                        //                                if (documentMetadata.isConfidential ==
                        // true) {
                        //                                    retentionPeriod = "100"
                        //                                    basis = "JulkL 24 § 1 mom. 32 k"
                        //                                    retentionReason =
                        // "YearsFromRecordCreation"
                        //                                }
                        //                            }
                        //
                        //                        informationSecurityPolicy =
                        //
                        // StandardMetadataType.InformationSecurityPolicy().apply {
                        //                                value = "Ei turvallisuusluokiteltu"
                        //                            }
                        //
                        //                        retentionPolicy =
                        //                            StandardMetadataType.RetentionPolicy().apply {
                        //                                period = "0"
                        //                                basis = "KA/13089/07.01.01.03.01/2018"
                        //                                retentionReason = "InPerpetuity"
                        //                            }
                        //
                        //                        protectionPolicy =
                        //                            StandardMetadataType.ProtectionPolicy().apply
                        // {
                        //                                value = "3"
                        //                            }
                    }
            }
        }
    }

    fun uploadToArchive(db: Database.Connection, documentId: ChildDocumentId) {
        logger.info { "Starting archival process for document $documentId" }

        val document = db.read { tx ->tx.getChildDocument(documentId) ?: throw NotFound("document $documentId not found") }
        val archivedProcess = db.read { tx -> tx.getArchiveProcessByChildDocumentId(documentId) }
        val documentMetadata = db.read { tx -> tx.getChildDocumentMetadata(documentId) }

        val documentKey =
            db.read { tx ->
                tx.getChildDocumentKey(documentId)
                    ?: throw NotFound("Document key not found for document $documentId")
            }

        // Get the document from the original location
        val originalLocation = documentClient.locate(DocumentKey.ChildDocument(documentKey))
        val documentContent = documentClient.get(originalLocation)

        val masterId = "yleinen"
        val classId = "12.01.SL1.RT34"
        val yleinenVirtualArchiveId = "YLEINEN"

        // Create metadata object and convert to XML
        val metadata =
            createDocumentMetadata(
                document,
                documentMetadata,
                archivedProcess,
                Paths.get(documentContent.name).fileName.toString(),
            )
        val metadataXml = marshalMetadata(metadata)
        logger.info { "Generated metadata XML: $metadataXml" }

        val (responseCode, responseBody) =
            client.putDocument(
                documentContent,
                metadataXml,
                masterId,
                classId,
                yleinenVirtualArchiveId,
            )
        logger.info { "Response code: $responseCode" }

        if (responseCode == 200) {
            db.transaction { tx -> tx.markDocumentAsArchived(documentId, HelsinkiDateTime.now()) }
            logger.info { "Successfully archived document $documentId" }
        } else {
            logger.error {
                "Failed to archive document $documentId. Response code: $responseCode, Response body: ${responseBody ?: "No response body"}"
            }
            throw RuntimeException("Failed to archive document $documentId")
        }
    }
}
