// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.childdocument.*
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.process.*
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
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
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
                    // Use namespaces in a similar way as the shared example xml files. TODO check
                    // if this
                    // is actually needed
                    setProperty(
                        "org.glassfish.jaxb.namespacePrefixMapper",
                        object : org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper() {
                            override fun getPreferredPrefix(
                                namespaceUri: String,
                                suggestion: String?,
                                requirePrefix: Boolean,
                            ): String {
                                return when (namespaceUri) {
                                    "http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0" ->
                                        "ns0"
                                    "http://www.avaintec.com/2004/records-schedule-fi/1.0" -> "at0"
                                    else -> suggestion ?: "ns"
                                }
                            }
                        },
                    )
                }
            return StringWriter().use { writer ->
                marshaller.marshal(metadata, writer)
                writer.toString()
            }
        }

        private fun createPolicyRule(
            timeSpan: Short,
            triggerEvent: String,
            action: PolicyConfiguration.Rules.Rule.Action,
        ): PolicyConfiguration.Rules.Rule {
            return PolicyConfiguration.Rules.Rule().apply {
                this.timeSpan = timeSpan
                this.triggerEvent = triggerEvent
                this.action = action
            }
        }

        private fun createPolicyAction(
            actionType: String,
            actionArgument: String? = null,
            actionAnnotation: String? = null,
        ): PolicyConfiguration.Rules.Rule.Action {
            return PolicyConfiguration.Rules.Rule.Action().apply {
                this.actionType = actionType
                if (actionArgument != null) {
                    actionArguments =
                        PolicyConfiguration.Rules.Rule.Action.ActionArguments().apply {
                            this.actionArgument = actionArgument
                        }
                }
                if (actionAnnotation != null) {
                    this.actionAnnotation = actionAnnotation
                }
            }
        }

        private fun createDisclosurePolicy(
            documentMetadata: DocumentMetadata
        ): DisclosurePolicyType {
            return DisclosurePolicyType().apply {
                policyConfiguration =
                    PolicyConfiguration().apply {
                        policyName = "DisclosurePolicy"
                        initialTargetState =
                            if (documentMetadata.confidential == true) "confidential" else "public"
                        rules =
                            PolicyConfiguration.Rules().apply {
                                rule =
                                    createPolicyRule(
                                        // according to Särmä specs, this should be set to 0 for
                                        // public and permanently confidential documents. If not
                                        // defined in metadata, we default to 100 years.
                                        timeSpan =
                                            if (documentMetadata.confidential == true)
                                                documentMetadata.confidentiality
                                                    ?.durationYears
                                                    ?.toShort() ?: 100
                                            else 0,
                                        // years from document creation (creation.created in xml)
                                        triggerEvent = "YearsFromRecordCreation",
                                        action =
                                            createPolicyAction(
                                                actionType = "SetTargetStateToArgument",
                                                actionArgument = "public",
                                                actionAnnotation =
                                                    documentMetadata.confidentiality?.basis
                                                        ?: "JulkL 24 § 1 mom. 32 k",
                                            ),
                                    )
                            }
                    }
            }
        }

        private fun createInformationSecurityPolicy(): InformationSecurityPolicyType {
            return InformationSecurityPolicyType().apply {
                policyConfiguration =
                    PolicyConfiguration().apply {
                        policyName = "InformationSecurityPolicy"
                        initialTargetState = "notSecurityClassified"
                        rules =
                            PolicyConfiguration.Rules().apply {
                                rule =
                                    createPolicyRule(
                                        timeSpan = 0,
                                        triggerEvent = "InPerpetuity",
                                        action =
                                            createPolicyAction(
                                                actionType = "SetTargetStateToArgument",
                                                actionArgument = "notSecurityClassified",
                                            ),
                                    )
                            }
                    }
            }
        }

        private fun createRetentionPolicy(): RetentionPolicyType {
            return RetentionPolicyType().apply {
                policyConfiguration =
                    PolicyConfiguration().apply {
                        policyName = "RetentionPolicy"
                        rules =
                            PolicyConfiguration.Rules().apply {
                                rule =
                                    createPolicyRule(
                                        timeSpan = 0,
                                        triggerEvent = "InPerpetuity",
                                        action =
                                            createPolicyAction(
                                                actionType = "AddTimeSpanToTarget",
                                                actionAnnotation = "KA/13089/07.01.01.03.01/2018",
                                            ),
                                    )
                            }
                    }
            }
        }

        private fun createProtectionPolicy(): ProtectionPolicyType {
            return ProtectionPolicyType().apply {
                policyConfiguration =
                    PolicyConfiguration().apply {
                        policyName = "ProtectionPolicy"
                        initialTargetState = "3"
                        rules =
                            PolicyConfiguration.Rules().apply {
                                rule =
                                    createPolicyRule(
                                        timeSpan = 0,
                                        triggerEvent = "InPerpetuity",
                                        action =
                                            createPolicyAction(
                                                actionType = "SetTargetStateToArgument",
                                                actionArgument = "3",
                                            ),
                                    )
                            }
                    }
            }
        }

        private fun createAgents(
            archivedProcess: ArchivedProcess?
        ): StandardMetadataType.DocumentDescription.Agents {
            return StandardMetadataType.DocumentDescription.Agents().apply {
                // Get unique agents by their ID to avoid duplicates
                archivedProcess
                    ?.history
                    ?.map { it.enteredBy }
                    ?.distinctBy { it.id }
                    ?.forEach { user ->
                        agent.add(
                            AgentType().apply {
                                corporateName = archivedProcess.organization
                                role = "Henkilökunta" // TODO pitäisikö tämän olla tarkempi rooli?
                                name = user.name
                            }
                        )
                    }
            }
        }

        private fun createDocumentDescription(
            document: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            archivedProcess: ArchivedProcess?,
            childIdentifier: ExternalIdentifier,
            childBirthDate: java.time.LocalDate,
        ): StandardMetadataType.DocumentDescription {
            return StandardMetadataType.DocumentDescription().apply {
                // Basic document info
                title = documentMetadata.name
                documentType = "Suunnitelma" // From Evaka_Särmä_metatietomääritykset.xlsx
                documentTypeSpecifier =
                    "Varhaiskasvatussuunnitelma" // From Evaka_Särmä_metatietomääritykset.xlsx
                personalData =
                    PersonalDataType.CONTAINS_PERSONAL_INFORMATION // TODO md_instance.xml:
                // containsPersonalInformation tai
                // containsSensitivePersonalInformation.
                // Mistä tämä tulisi päätellä?
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
                socialSecurityNumber =
                    if (childIdentifier is ExternalIdentifier.SSN) childIdentifier.ssn else null
                birthDate =
                    childBirthDate.let { date ->
                        DatatypeFactory.newInstance()
                            .newXMLGregorianCalendar(
                                date.year,
                                date.monthValue,
                                date.dayOfMonth,
                                javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                                javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                                javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                                javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                                javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                            )
                    }
                agents = createAgents(archivedProcess)
            }
        }

        private fun createFormat(filename: String): StandardMetadataType.Format {
            return StandardMetadataType.Format().apply {
                recordType = ResourceTypeType.DIGITAL
                mimeType = AcceptedMimeTypeType.APPLICATION_PDF
                this.fileName = filename
                fileFormat = AcceptedFileFormatType.NOT_APPLICABLE
            }
        }

        private fun createCreation(
            documentMetadata: DocumentMetadata
        ): StandardMetadataType.Creation {
            return StandardMetadataType.Creation().apply {
                created = documentMetadata.createdAt?.asXMLGregorianCalendar()
                originatingSystem = "Varhaiskasvatuksen toiminnanohjausjärjestelmä"
            }
        }

        private fun createPolicies(
            documentMetadata: DocumentMetadata
        ): StandardMetadataType.Policies {
            return StandardMetadataType.Policies().apply {
                // Salassapitosääntö
                disclosurePolicy = createDisclosurePolicy(documentMetadata)
                // Turvallisuussääntö
                informationSecurityPolicy = createInformationSecurityPolicy()
                // Säilytyssääntö
                retentionPolicy = createRetentionPolicy()
                // Suojeluluokkasääntö
                protectionPolicy = createProtectionPolicy()
            }
        }

        private fun createCaseFile(
            documentMetadata: DocumentMetadata,
            archivedProcess: ArchivedProcess?,
        ): CaseFileType {
            return CaseFileType().apply {
                caseCreated = documentMetadata.createdAt?.asXMLGregorianCalendar()
                caseFinished =
                    archivedProcess
                        ?.history
                        ?.find { it.state == ArchivedProcessState.COMPLETED }
                        ?.enteredAt
                        ?.asXMLGregorianCalendar()
            }
        }

        internal fun createDocumentMetadata(
            document: ChildDocumentDetails,
            documentMetadata: DocumentMetadata,
            archivedProcess: ArchivedProcess?,
            filename: String,
            childIdentifier: ExternalIdentifier,
            birthDate: java.time.LocalDate,
        ): RecordMetadataInstance {
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
                            createDocumentDescription(
                                document,
                                documentMetadata,
                                archivedProcess,
                                childIdentifier,
                                birthDate,
                            )
                        format = createFormat(filename)
                        creation = createCreation(documentMetadata)
                        policies = createPolicies(documentMetadata)
                        caseFile = createCaseFile(documentMetadata, archivedProcess)
                    }
            }
        }
    }

    fun uploadToArchive(db: Database.Connection, documentId: ChildDocumentId) {
        logger.info { "Starting archival process for document $documentId" }

        val document =
            db.read { tx ->
                tx.getChildDocument(documentId) ?: throw NotFound("document $documentId not found")
            }
        if (
            document.template.type !in
                setOf(
                    DocumentType.VASU,
                    DocumentType.LEOPS,
                    DocumentType.HOJKS,
                    DocumentType.MIGRATED_VASU,
                    DocumentType.MIGRATED_LEOPS,
                )
        ) {
            logger.warn {
                "Refusing to archive non-supported document type ${document.template.type} with id $documentId"
            }
            return
        }
        val childInfo =
            db.read { tx ->
                val childId = document.child.id
                tx.getPersonById(childId)
                    ?: throw IllegalStateException("No person found with $childId")
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
            "yleinen" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx specs. TODO
        // should be mapped from metadata
        val classId =
            "12.06.01.SL1.RT34" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx
        // specs. TODO should be mapped from metadata
        val virtualArchiveId =
            "YLEINEN" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx specs. TODO
        // should be mapped from metadata

        // Create metadata object and convert to XML
        val metadata =
            createDocumentMetadata(
                document,
                documentMetadata,
                archivedProcess,
                Paths.get(documentContent.name).fileName.toString(),
                childInfo.identity,
                childInfo.dateOfBirth,
            )
        val metadataXml = marshalMetadata(metadata)
        logger.info { "Generated metadata XML: $metadataXml" }

        val (responseCode, responseBody) =
            client.putDocument(documentContent, metadataXml, masterId, classId, virtualArchiveId)
        logger.info { "Response code: $responseCode" }
        logger.info { "Response body: $responseBody" }

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
