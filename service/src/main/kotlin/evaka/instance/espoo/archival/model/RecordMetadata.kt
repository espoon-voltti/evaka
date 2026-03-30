// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.archival.model

import java.time.LocalDate

data class RecordMetadataInstance(val standardMetadata: StandardMetadata)

data class StandardMetadata(
    val metadataMasterVersion: MetadataMasterVersion,
    val virtualArchiveId: String,
    val recordIdentifiers: RecordIdentifiers,
    val documentDescription: DocumentDescription,
    val format: Format,
    val creation: Creation,
    val policies: Policies,
    val caseFile: CaseFile,
)

data class MetadataMasterVersion(val masterName: String, val versionNumber: String)

data class RecordIdentifiers(val caseIdentifier: String?, val recordIdentifier: String)

data class DocumentDescription(
    val title: String,
    val documentType: String?,
    val documentTypeSpecifier: String?,
    val registerName: String?,
    val firstName: String?,
    val lastName: String?,
    val socialSecurityNumber: String?,
    val birthDate: LocalDate?,
    val personalData: PersonalData?,
    val personalDataCollectionReason: String?,
    val language: String?,
    val dataManagement: String?,
    val dataSource: String?,
    val agents: List<Agent>,
)

enum class PersonalData(val xmlValue: String) {
    NO_PERSONAL_INFORMATION("noPersonalInformation"),
    CONTAINS_PERSONAL_INFORMATION("containsPersonalInformation"),
    CONTAINS_SENSITIVE_PERSONAL_INFORMATION("containsSensitivePersonalInformation"),
}

data class Agent(val role: String?, val name: String?, val corporateName: String?)

data class Format(
    val recordType: ResourceType,
    val mimeType: MimeType?,
    val fileFormat: FileFormat?,
    val fileName: String?,
)

enum class ResourceType(val xmlValue: String) {
    DIGITAL("digital"),
    PHYSICAL("physical"),
}

enum class MimeType(val xmlValue: String) {
    APPLICATION_PDF("application/pdf")
}

enum class FileFormat(val xmlValue: String) {
    NOT_APPLICABLE("not applicable")
}

data class Creation(val originatingSystem: String?, val created: LocalDate?)

data class Policies(
    val retentionPolicy: PolicyConfiguration,
    val disclosurePolicy: PolicyConfiguration,
    val informationSecurityPolicy: PolicyConfiguration,
    val protectionPolicy: PolicyConfiguration,
)

data class PolicyConfiguration(
    val policyName: String,
    val initialTargetState: String?,
    val rule: PolicyRule,
)

data class PolicyRule(val timeSpan: Short, val triggerEvent: String, val action: PolicyAction)

data class PolicyAction(
    val actionType: String,
    val actionArgument: String?,
    val actionAnnotation: String?,
)

data class CaseFile(val caseCreated: LocalDate?, val caseFinished: LocalDate?)
