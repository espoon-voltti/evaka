// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.caseprocess.CaseProcess
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.DocumentMetadata
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.sarma.model.*
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import jakarta.xml.bind.JAXBContext
import java.io.StringWriter
import java.time.LocalDate
import java.time.ZoneId
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun HelsinkiDateTime.asXMLGregorianCalendar(): XMLGregorianCalendar {
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

/** Converts LocalDate to XMLGregorianCalendar with only date fields. */
fun LocalDate.toXMLGregorianCalendar(): XMLGregorianCalendar {
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(
            this.year,
            this.monthValue,
            this.dayOfMonth,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
            javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
        )
}

/**
 * Calculates the next July 31 date from the given date. If the date is before or on July 31 of the
 * current year, returns July 31 of the current year. Otherwise, returns July 31 of the next year.
 */
fun calculateNextJuly31(date: LocalDate): LocalDate {
    val currentYearJuly31 = LocalDate.of(date.year, 7, 31)
    val year = if (date.isAfter(currentYearJuly31)) date.year + 1 else date.year
    return LocalDate.of(year, 7, 31)
}

fun marshalMetadata(metadata: RecordMetadataInstance): String {
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

private fun createDisclosurePolicy(documentMetadata: DocumentMetadata): DisclosurePolicyType {
    return DisclosurePolicyType().apply {
        policyConfiguration =
            PolicyConfiguration().apply {
                policyName = "DisclosurePolicy"
                initialTargetState =
                    if (documentMetadata.confidential == true) "confidential" else "public"
                rules =
                    PolicyConfiguration.Rules().apply {
                        rule =
                            PolicyConfiguration.Rules.Rule().apply {
                                // according to Särmä specs, this should be set to 0 for
                                // public and permanently confidential documents. If not
                                // defined in metadata, we default to 100 years.
                                timeSpan =
                                    if (documentMetadata.confidential == true)
                                        documentMetadata.confidentiality?.durationYears?.toShort()
                                            ?: 100
                                    else 0
                                // years from document creation (creation.created in xml)
                                triggerEvent = "YearsFromRecordCreation"
                                action = run {
                                    val actionAnnotation =
                                        documentMetadata.confidentiality?.basis
                                            ?: "JulkL 24 § 1 mom. 32 k"
                                    PolicyConfiguration.Rules.Rule.Action().apply {
                                        actionType = "SetTargetStateToArgument"
                                        actionArguments =
                                            PolicyConfiguration.Rules.Rule.Action.ActionArguments()
                                                .apply { actionArgument = "public" }
                                        this.actionAnnotation = actionAnnotation
                                    }
                                }
                            }
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
                            PolicyConfiguration.Rules.Rule().apply {
                                timeSpan = 0
                                triggerEvent = "InPerpetuity"
                                action =
                                    PolicyConfiguration.Rules.Rule.Action().apply {
                                        actionType = "SetTargetStateToArgument"
                                        actionArguments =
                                            PolicyConfiguration.Rules.Rule.Action.ActionArguments()
                                                .apply { actionArgument = "notSecurityClassified" }
                                    }
                            }
                    }
            }
    }
}

fun createRetentionPolicy(documentType: ChildDocumentType): RetentionPolicyType {
    return RetentionPolicyType().apply {
        policyConfiguration =
            PolicyConfiguration().apply {
                policyName = "RetentionPolicy"
                rules =
                    PolicyConfiguration.Rules().apply {
                        rule =
                            when (documentType) {
                                ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                                ChildDocumentType.PEDAGOGICAL_REPORT ->
                                    PolicyConfiguration.Rules.Rule().apply {
                                        timeSpan = 28
                                        triggerEvent = "YearsFromCustomerBirthDate"
                                        action =
                                            PolicyConfiguration.Rules.Rule.Action().apply {
                                                actionType = "AddTimeSpanToTarget"
                                                actionAnnotation =
                                                    "Perusopetuslaki (628/1998) 16 a §"
                                            }
                                    }
                                else ->
                                    PolicyConfiguration.Rules.Rule().apply {
                                        timeSpan = 0
                                        triggerEvent = "InPerpetuity"
                                        action =
                                            PolicyConfiguration.Rules.Rule.Action().apply {
                                                actionType = "AddTimeSpanToTarget"
                                                actionAnnotation = "KA/13089/07.01.01.03.01/2018"
                                            }
                                    }
                            }
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
                            PolicyConfiguration.Rules.Rule().apply {
                                timeSpan = 0
                                triggerEvent = "InPerpetuity"
                                action =
                                    PolicyConfiguration.Rules.Rule.Action().apply {
                                        actionType = "SetTargetStateToArgument"
                                        actionArguments =
                                            PolicyConfiguration.Rules.Rule.Action.ActionArguments()
                                                .apply { actionArgument = "3" }
                                    }
                            }
                    }
            }
    }
}

private fun createAgents(
    caseProcess: CaseProcess?
): StandardMetadataType.DocumentDescription.Agents {
    return StandardMetadataType.DocumentDescription.Agents().apply {
        // Get unique agents by their ID to avoid duplicates
        caseProcess
            ?.history
            ?.map { it.enteredBy }
            ?.distinctBy { it.id }
            ?.forEach { user ->
                agent.add(
                    AgentType().apply {
                        corporateName = caseProcess.organization
                        role = "Henkilökunta"
                        name = user.name
                    }
                )
            }
    }
}

fun createDocumentDescription(
    document: ChildDocumentDetails,
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    childIdentifier: ExternalIdentifier,
    childBirthDate: java.time.LocalDate,
): StandardMetadataType.DocumentDescription {
    return StandardMetadataType.DocumentDescription().apply {
        // Basic document info
        title = documentMetadata.name
        documentType =
            when (document.template.type) {
                ChildDocumentType.VASU,
                ChildDocumentType.MIGRATED_VASU,
                ChildDocumentType.LEOPS,
                ChildDocumentType.MIGRATED_LEOPS,
                ChildDocumentType.HOJKS -> "Suunnitelma"
                ChildDocumentType.PEDAGOGICAL_ASSESSMENT -> "Arvio"
                ChildDocumentType.PEDAGOGICAL_REPORT -> "Selvitys"
                else -> null
            }
        documentTypeSpecifier =
            when (document.template.type) {
                ChildDocumentType.VASU,
                ChildDocumentType.MIGRATED_VASU -> "Varhaiskasvatussuunnitelma"
                ChildDocumentType.LEOPS,
                ChildDocumentType.MIGRATED_LEOPS -> "Lapsen esiopetuksen oppimissuunnitelma LEOPS"
                ChildDocumentType.PEDAGOGICAL_ASSESSMENT -> "Pedagoginen arvio"
                ChildDocumentType.PEDAGOGICAL_REPORT -> "Pedagoginen selvitys"
                ChildDocumentType.HOJKS ->
                    "Henkilökohtaisen opetuksen järjestämistä koskeva suunnitelma HOJKS"
                else -> null
            }
        personalData = PersonalDataType.CONTAINS_SENSITIVE_PERSONAL_INFORMATION
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
        birthDate = childBirthDate.toXMLGregorianCalendar()
        agents = createAgents(caseProcess)
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

private fun createCreation(documentMetadata: DocumentMetadata): StandardMetadataType.Creation {
    return StandardMetadataType.Creation().apply {
        created = documentMetadata.createdAt?.asXMLGregorianCalendar()
        originatingSystem = "Varhaiskasvatuksen toiminnanohjausjärjestelmä"
    }
}

private fun createPolicies(
    documentMetadata: DocumentMetadata,
    documentType: ChildDocumentType,
): StandardMetadataType.Policies {
    return StandardMetadataType.Policies().apply {
        // Salassapitosääntö
        disclosurePolicy = createDisclosurePolicy(documentMetadata)
        // Turvallisuussääntö
        informationSecurityPolicy = createInformationSecurityPolicy()
        // Säilytyssääntö
        retentionPolicy = createRetentionPolicy(documentType)
        // Suojeluluokkasääntö
        protectionPolicy = createProtectionPolicy()
    }
}

private fun getCaseFinishDate(
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    document: ChildDocumentDetails,
): XMLGregorianCalendar? {
    // Determine the case finished date with fallback logic
    return caseProcess
        ?.history
        ?.find { it.state == CaseProcessState.COMPLETED }
        ?.enteredAt
        ?.asXMLGregorianCalendar()
        ?: document.template.validity.end?.toXMLGregorianCalendar()
        ?: documentMetadata.createdAt?.let {
            val createdDate = it.toLocalDateTime().toLocalDate()
            calculateNextJuly31(createdDate).toXMLGregorianCalendar()
        }
}

fun createCaseFile(
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    document: ChildDocumentDetails,
): CaseFileType {
    return CaseFileType().apply {
        caseCreated = documentMetadata.createdAt?.asXMLGregorianCalendar()

        caseFinished = getCaseFinishDate(documentMetadata, caseProcess, document)
    }
}

fun createDocumentMetadata(
    document: ChildDocumentDetails,
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
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
                        caseIdentifier = caseProcess?.caseIdentifier
                    }

                documentDescription =
                    createDocumentDescription(
                        document,
                        documentMetadata,
                        caseProcess,
                        childIdentifier,
                        birthDate,
                    )
                format = createFormat(filename)
                creation = createCreation(documentMetadata)
                policies = createPolicies(documentMetadata, document.template.type)
                caseFile = createCaseFile(documentMetadata, caseProcess, document)
                creation.created = getCaseFinishDate(documentMetadata, caseProcess, document)
            }
    }
}
