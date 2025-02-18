// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival.yleinen

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.XmlType

@XmlRootElement(
    name = "RecordMetadataInstance",
    namespace = "http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0",
)
@XmlAccessorType(XmlAccessType.FIELD)
data class RecordMetadataInstance(
    @field:XmlElement(name = "StandardMetadata", required = true)
    val standardMetadata: StandardMetadata,
    @field:XmlElement(name = "ExtendedMetadata", required = false)
    val extendedMetadata: ExtendedMetadata? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class StandardMetadata(
    @field:XmlElement(name = "virtualArchiveId", required = true) val virtualArchiveId: String,
    @field:XmlElement(name = "recordIdentifiers", required = true)
    val recordIdentifiers: RecordIdentifiers,
    @field:XmlElement(name = "documentDescription", required = true)
    val documentDescription: DocumentDescription,
    @field:XmlElement(name = "format", required = true) val format: Format,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class RecordIdentifiers(
    @field:XmlElement(name = "CaseIdentifier", required = false) val caseIdentifier: String? = null,
    @field:XmlElement(name = "RecordIdentifier", required = true) val recordIdentifier: String,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class DocumentDescription(
    @field:XmlElement(name = "title", required = true) val title: String,
    @field:XmlElement(name = "otherId", required = false) val otherId: String? = null,
    @field:XmlElement(name = "documentType", required = false) val documentType: String? = null,
    @field:XmlElement(name = "documentTypeSpecifier", required = false)
    val documentTypeSpecifier: String? = null,
    @field:XmlElement(name = "description", required = false) val description: String? = null,
    @field:XmlElement(name = "firstName", required = false) val firstName: String? = null,
    @field:XmlElement(name = "lastName", required = false) val lastName: String? = null,
    @field:XmlElement(name = "socialSecurityNumber", required = false)
    val socialSecurityNumber: String? = null,
    @field:XmlElement(name = "keywords", required = false) val keywords: String? = null,
    @field:XmlElement(name = "personalData", required = false)
    val personalData: PersonalDataType? = null,
    @field:XmlElement(name = "personalDataCollectionReason", required = false)
    val personalDataCollectionReason: String? = null,
    @field:XmlElement(name = "language", required = false) val language: String? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Format(
    @field:XmlElement(name = "recordType", required = true) val recordType: ResourceType,
    @field:XmlElement(name = "mimeType", required = false) val mimeType: String? = null,
    @field:XmlElement(name = "characterSet", required = false)
    val characterSet: CharacterSet? = null,
    @field:XmlElement(name = "fileFormat", required = false) val fileFormat: String? = null,
    @field:XmlElement(name = "formatVersion", required = false) val formatVersion: String? = null,
    @field:XmlElement(name = "payloadSize", required = false) val payloadSize: Int? = null,
    @field:XmlElement(name = "fileName", required = false) val fileName: String? = null,
)

@XmlType(name = "resourceTypeType")
enum class ResourceType {
    @XmlElement(name = "digital") DIGITAL,
    @XmlElement(name = "physical") PHYSICAL,
}

@XmlType(name = "personalDataType")
enum class PersonalDataType {
    @XmlElement(name = "noPersonalInformation") NO_PERSONAL_INFORMATION,
    @XmlElement(name = "containsPersonalInformation") CONTAINS_PERSONAL_INFORMATION,
    @XmlElement(name = "containsSensitivePersonalInformation")
    CONTAINS_SENSITIVE_PERSONAL_INFORMATION,
    @XmlElement(name = "containsInformationOnCriminalConvictionsAndOffenses")
    CONTAINS_INFORMATION_ON_CRIMINAL_CONVICTIONS_AND_OFFENSES,
}

@XmlType(name = "CharacterSetType")
enum class CharacterSet {
    @XmlElement(name = "UTF-8") UTF_8,
    @XmlElement(name = "ISO-8859-1") ISO_8859_1,
}

@XmlAccessorType(XmlAccessType.FIELD)
data class ExtendedMetadata(
    // This is a placeholder for any additional XML content
    val content: Any? = null
)
