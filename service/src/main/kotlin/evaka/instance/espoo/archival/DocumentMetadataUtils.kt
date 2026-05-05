// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.archival

import evaka.core.caseprocess.CaseProcess
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.DocumentMetadata
import evaka.core.document.ChildDocumentType
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.identity.ExternalIdentifier
import evaka.instance.espoo.archival.model.*
import java.time.LocalDate

/**
 * Calculates the next July 31 date from the given date. If the date is before or on July 31 of the
 * current year, returns July 31 of the current year. Otherwise, returns July 31 of the next year.
 */
fun calculateNextJuly31(date: LocalDate): LocalDate {
    val currentYearJuly31 = LocalDate.of(date.year, 7, 31)
    val year = if (date.isAfter(currentYearJuly31)) date.year + 1 else date.year
    return LocalDate.of(year, 7, 31)
}

private fun createDisclosurePolicy(documentMetadata: DocumentMetadata): PolicyConfiguration {
    val timeSpan =
        if (documentMetadata.confidential == true)
            documentMetadata.confidentiality?.durationYears?.toShort() ?: 100
        else 0
    val actionAnnotation = documentMetadata.confidentiality?.basis ?: "JulkL 24 § 1 mom. 32 k"

    return PolicyConfiguration(
        policyName = "DisclosurePolicy",
        initialTargetState =
            if (documentMetadata.confidential == true) "confidential" else "public",
        rule =
            PolicyRule(
                timeSpan = timeSpan,
                triggerEvent = "YearsFromRecordCreation",
                action =
                    PolicyAction(
                        actionType = "SetTargetStateToArgument",
                        actionArgument = "public",
                        actionAnnotation = actionAnnotation,
                    ),
            ),
    )
}

private fun createInformationSecurityPolicy(): PolicyConfiguration {
    return PolicyConfiguration(
        policyName = "InformationSecurityPolicy",
        initialTargetState = "notSecurityClassified",
        rule =
            PolicyRule(
                timeSpan = 0,
                triggerEvent = "InPerpetuity",
                action =
                    PolicyAction(
                        actionType = "SetTargetStateToArgument",
                        actionArgument = "notSecurityClassified",
                        actionAnnotation = null,
                    ),
            ),
    )
}

fun createRetentionPolicy(documentType: ChildDocumentType): PolicyConfiguration {
    val rule =
        when (documentType) {
            ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
            ChildDocumentType.PEDAGOGICAL_REPORT ->
                PolicyRule(
                    timeSpan = 28,
                    triggerEvent = "YearsFromCustomerBirthDate",
                    action =
                        PolicyAction(
                            actionType = "AddTimeSpanToTarget",
                            actionArgument = null,
                            actionAnnotation = "Perusopetuslaki (628/1998) 16 a §",
                        ),
                )

            else ->
                PolicyRule(
                    timeSpan = 0,
                    triggerEvent = "InPerpetuity",
                    action =
                        PolicyAction(
                            actionType = "AddTimeSpanToTarget",
                            actionArgument = null,
                            actionAnnotation = "KA/13089/07.01.01.03.01/2018",
                        ),
                )
        }

    return PolicyConfiguration(
        policyName = "RetentionPolicy",
        initialTargetState = null,
        rule = rule,
    )
}

private fun createProtectionPolicy(): PolicyConfiguration {
    return PolicyConfiguration(
        policyName = "ProtectionPolicy",
        initialTargetState = "3",
        rule =
            PolicyRule(
                timeSpan = 0,
                triggerEvent = "InPerpetuity",
                action =
                    PolicyAction(
                        actionType = "SetTargetStateToArgument",
                        actionArgument = "3",
                        actionAnnotation = null,
                    ),
            ),
    )
}

private fun createAgents(caseProcess: CaseProcess?): List<Agent> {
    return caseProcess
        ?.history
        ?.map { it.enteredBy }
        ?.distinctBy { it.id }
        ?.map { user ->
            Agent(corporateName = caseProcess.organization, role = "Henkilökunta", name = user.name)
        } ?: emptyList()
}

fun createDocumentDescription(
    document: ChildDocumentDetails,
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    childIdentifier: ExternalIdentifier,
    childBirthDate: LocalDate,
): DocumentDescription {
    return DocumentDescription(
        title = documentMetadata.name,
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
            },
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
            },
        personalData = PersonalData.CONTAINS_SENSITIVE_PERSONAL_INFORMATION,
        language = document.template.language.isoLanguage.alpha2,
        dataManagement = "Palvelujen tiedonhallinta",
        dataSource = "Varhaiskasvatuksen tietovaranto",
        registerName = "Varhaiskasvatuksen asiakastietorekisteri",
        personalDataCollectionReason =
            "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen",
        firstName = document.child.firstName,
        lastName = document.child.lastName,
        socialSecurityNumber =
            if (childIdentifier is ExternalIdentifier.SSN) childIdentifier.ssn else null,
        birthDate = childBirthDate,
        agents = createAgents(caseProcess),
    )
}

private fun createFormat(filename: String): Format {
    return Format(
        recordType = ResourceType.DIGITAL,
        mimeType = MimeType.APPLICATION_PDF,
        fileName = filename,
        fileFormat = FileFormat.NOT_APPLICABLE,
    )
}

private fun getCreationDate(
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
): LocalDate? {
    return caseProcess
        ?.history
        ?.find { it.state == CaseProcessState.INITIAL }
        ?.enteredAt
        ?.toLocalDate() ?: documentMetadata.createdAtDate
}

fun createCreation(documentMetadata: DocumentMetadata, caseProcess: CaseProcess?): Creation {
    return Creation(
        created = getCreationDate(documentMetadata, caseProcess),
        originatingSystem = "Varhaiskasvatuksen toiminnanohjausjärjestelmä",
    )
}

private fun createPolicies(
    documentMetadata: DocumentMetadata,
    documentType: ChildDocumentType,
): Policies {
    return Policies(
        disclosurePolicy = createDisclosurePolicy(documentMetadata),
        informationSecurityPolicy = createInformationSecurityPolicy(),
        retentionPolicy = createRetentionPolicy(documentType),
        protectionPolicy = createProtectionPolicy(),
    )
}

private fun getCaseFinishDate(
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    document: ChildDocumentDetails,
): LocalDate? {
    return caseProcess
        ?.history
        ?.find { it.state == CaseProcessState.COMPLETED }
        ?.enteredAt
        ?.toLocalDate()
        ?: document.template.validity.end
        ?: getCreationDate(documentMetadata, caseProcess)?.let { createdDate ->
            calculateNextJuly31(createdDate)
        }
}

fun createCaseFile(
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    document: ChildDocumentDetails,
): CaseFile {
    return CaseFile(
        caseCreated = getCreationDate(documentMetadata, caseProcess),
        caseFinished = getCaseFinishDate(documentMetadata, caseProcess, document),
    )
}

fun createDocumentMetadata(
    document: ChildDocumentDetails,
    documentMetadata: DocumentMetadata,
    caseProcess: CaseProcess?,
    filename: String,
    childIdentifier: ExternalIdentifier,
    birthDate: LocalDate,
    masterId: String,
    virtualArchiveId: String,
): RecordMetadataInstance {
    val creation = createCreation(documentMetadata, caseProcess)
    val caseFinishDate = getCaseFinishDate(documentMetadata, caseProcess, document)

    return RecordMetadataInstance(
        standardMetadata =
            StandardMetadata(
                metadataMasterVersion =
                    MetadataMasterVersion(masterName = masterId, versionNumber = "1.0"),
                virtualArchiveId = virtualArchiveId,
                recordIdentifiers =
                    RecordIdentifiers(
                        recordIdentifier = document.id.toString(),
                        caseIdentifier = caseProcess?.caseIdentifier,
                    ),
                documentDescription =
                    createDocumentDescription(
                        document,
                        documentMetadata,
                        caseProcess,
                        childIdentifier,
                        birthDate,
                    ),
                format = createFormat(filename),
                creation = creation.copy(created = caseFinishDate),
                policies = createPolicies(documentMetadata, document.template.type),
                caseFile = createCaseFile(documentMetadata, caseProcess, document),
            )
    )
}
