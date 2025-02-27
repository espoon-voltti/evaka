// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.childdocument.*
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
import java.nio.file.Paths
import java.time.ZoneId
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

private fun HelsinkiDateTime.asXMLGregorianCalendar(): XMLGregorianCalendar {
    val zdt = this.toLocalDateTime().atZone(ZoneId.of("Europe/Helsinki"))
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(
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
            document: ChildDocumentDetailsWithSsn,
            documentMetadata: ProcessMetadataController.DocumentMetadata,
            archivedProcess: ArchivedProcess?,
            filename: String,
        ): RecordMetadataInstance {

            // TODO TOS numero + vuosi koodi täytyis laittaa johonkin
            // TODO tulisiko tieto documentMetadata.organization eli "Espoon kaupungin esiopetus ja
            // varhaiskasvatus" laittaa johonkin?
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
                                documentTypeSpecifier = "Varhaiskasvatussuunnitelma" // From
                                // Evaka_Särmä_metatietomääritykset.xlsx
                                personalData = PersonalDataType.CONTAINS_PERSONAL_INFORMATION
                                language = document.template.language.isoLanguage.alpha2

                                // From Evaka_Särmä_metatietomääritykset.xlsx
                                dataManagement = "Palvelujen tiedonhallinta"
                                dataSource = "Varhaiskasvatuksen tietovaranto"
                                registerName = "Varhaiskasvatuksen asiakastietorekisteri"
                                personalDataCollectionReason =
                                    "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen"

                                // Child's information
                                firstName = document.child.firstName
                                lastName = document.child.lastName
                                socialSecurityNumber = document.child.socialSecurityNumber
                                // TODO add birthDate once schema field is known
                                // birthDate?.let { birthDate = it.toString() }
                                agents =
                                    StandardMetadataType.DocumentDescription.Agents().apply {
                                        // Get unique agents by their ID to avoid duplicates
                                        archivedProcess
                                            ?.history
                                            ?.map { it.enteredBy }
                                            ?.distinctBy { it.id }
                                            ?.forEach { user ->
                                                agent.add(
                                                    AgentType().apply {
                                                        corporateName =
                                                            "Suomenkielisen varhaiskasvatuksen tulosyksikkö" // TODO Mistä tieto suomi / ruotsi yksiköstä
                                                        role =
                                                            "Henkilökunta" // TODO pitäisikö tämän
                                                        // olla tarkempi rooli?
                                                        name = user.name
                                                    }
                                                )
                                            }
                                    }
                            }
                        format =
                            StandardMetadataType.Format().apply {
                                recordType = ResourceTypeType.DIGITAL
                                mimeType = AcceptedMimeTypeType.APPLICATION_PDF
                                fileName = filename
                                fileFormat =
                                    AcceptedFileFormatType
                                        .PDF // TODO Evaka_Särmä_metatietomääritykset.xlsx
                                // ohjeistetaan: Asetetaan arvo "not applicable"
                            }

                        // Creation and handling information
                        creation =
                            StandardMetadataType.Creation().apply {
                                created = documentMetadata.createdAt?.asXMLGregorianCalendar()
                                originatingSystem = "Varhaiskasvatuksen toiminnanohjausjärjestelmä"
                            }
                        // TODO miten eventAgent saa asetettua niin, että se menee schema
                        // validoinnista läpi?
                        //                        history =
                        //                            StandardMetadataType.History().apply {
                        //                                event.addAll(
                        //                                    archivedProcess?.history?.map {
                        // archiveProcessHistoryEntry ->
                        //                                        HistoryEventType().apply {
                        //                                            eventDateTime =
                        //
                        // archiveProcessHistoryEntry.enteredAt
                        //
                        // .asXMLGregorianCalendar()
                        //                                            eventType =
                        // archiveProcessHistoryEntry.state.name
                        //                                            eventTxId =
                        // "rowIndex_${archiveProcessHistoryEntry.rowIndex}"
                        //                                            eventAgent =
                        // HistoryEventType.EventAgent().apply {
                        //                                                any = JAXBElement(
                        //                                                    QName("", "text"),
                        //                                                    String::class.java,
                        //
                        // archiveProcessHistoryEntry.enteredBy.name
                        //                                                )
                        //                                            }
                        //                                        }
                        //                                    } ?: emptyList()
                        //                                )
                        //                            }

                        // Security and retention policies
                        policies =
                            StandardMetadataType.Policies().apply {
                                disclosurePolicy =
                                    DisclosurePolicyType().apply {
                                        disclosureLevel =
                                            if (documentMetadata.confidential == true)
                                                DisclosureLevelType.CONFIDENTIAL
                                            else DisclosureLevelType.PUBLIC
                                    }
                                // TODO Evaka_Särmä_metatietomääritykset.xlsx sanotaan, että nämä
                                // tulisi olla "Ei turvallisuusluokiteltu". Tarkista että
                                // InformationSecurityLevelType.UNCLASSIFIED on OK
                                informationSecurityPolicy =
                                    InformationSecurityPolicyType().apply {
                                        securityLevel = InformationSecurityLevelType.UNCLASSIFIED
                                    }
                                // TODO tarvitaanko näitä XML puolelle?
                                // Evaka_Särmä_metatietomääritykset.xlsx sanotaan, että nämä tulisi
                                // tulla PUT sanoman headereissa
                                retentionPolicy =
                                    RetentionPolicyType().apply {
                                        retentionPeriod = "100"
                                        retentionReason = "InPerpetuity"
                                    }
                                // TODO Evaka_Särmä_metatietomääritykset.xlsx sanoo, että tämä tulee
                                // olla "3", mutta scheman mukaan tähän voi antaa vain Basic,
                                // Enhanced tai High
                                protectionPolicy =
                                    ProtectionPolicyType().apply {
                                        protectionLevel = ProtectionLevelType.HIGH
                                    }
                            }
                    }
            }
        }
    }

    fun uploadToArchive(db: Database.Connection, documentId: ChildDocumentId) {
        logger.info { "Starting archival process for document $documentId" }

        val document =
            db.read { tx ->
                tx.getChildDocumentWithSsn(documentId)
                    ?: throw NotFound("document $documentId not found")
            }
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
        val masterId =
            "yleinen" // TODO ei vielä varmuudella tiedossa. Muissa Särmä integraatioissa käytetty
        // esim. "taloushallinto" tai "paatoksenteko"
        val classId =
            "12.01.SL1.RT34" // Arvo perustuu Evaka_Särmä_metatietomääritykset.xlsx -tiedostoon
        val virtualArchiveId =
            "YLEINEN" // Arvo perustuu Evaka_Särmä_metatietomääritykset.xlsx -tiedostoon

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
                virtualArchiveId,
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
