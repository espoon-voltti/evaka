// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.archival

import evaka.instance.espoo.archival.marshalMetadata
import evaka.instance.espoo.archival.model.*
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

const val TEST_MAIN_NAMESPACE = "urn:example:record-metadata"
const val TEST_POLICY_NAMESPACE = "urn:example:records-schedule"
const val TEST_MASTER_ID = "placeholder"
const val TEST_VIRTUAL_ARCHIVE_ID = "PLACEHOLDER"

fun createTestMetadataInstance(): RecordMetadataInstance =
    RecordMetadataInstance(
        standardMetadata =
            StandardMetadata(
                metadataMasterVersion =
                    MetadataMasterVersion(masterName = TEST_MASTER_ID, versionNumber = "1.0"),
                virtualArchiveId = TEST_VIRTUAL_ARCHIVE_ID,
                recordIdentifiers =
                    RecordIdentifiers(
                        caseIdentifier = "1/1234/2023",
                        recordIdentifier = "c3cc95f8-f045-11ef-9114-87ea771c5c89",
                    ),
                documentDescription =
                    DocumentDescription(
                        title = "VASU 2022-2023",
                        documentType = "Suunnitelma",
                        documentTypeSpecifier = "Varhaiskasvatussuunnitelma",
                        registerName = "Varhaiskasvatuksen asiakastietorekisteri",
                        firstName = "Kaarina",
                        lastName = "Karhula",
                        socialSecurityNumber = "160616A978U",
                        birthDate = LocalDate.of(2016, 6, 6),
                        personalData = PersonalData.CONTAINS_SENSITIVE_PERSONAL_INFORMATION,
                        personalDataCollectionReason =
                            "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen",
                        language = "fi",
                        dataManagement = "Palvelujen tiedonhallinta",
                        dataSource = "Varhaiskasvatuksen tietovaranto",
                        agents =
                            listOf(
                                Agent(
                                    role = "Henkilökunta",
                                    name = "Testi Testaaja",
                                    corporateName = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                                )
                            ),
                    ),
                format =
                    Format(
                        recordType = ResourceType.DIGITAL,
                        mimeType = MimeType.APPLICATION_PDF,
                        fileFormat = FileFormat.NOT_APPLICABLE,
                        fileName = "child_document_c3cc95f8-f045-11ef-9114-87ea771c5c89.pdf",
                    ),
                creation =
                    Creation(
                        originatingSystem = "Varhaiskasvatuksen toiminnanohjausjärjestelmä",
                        created = LocalDate.of(2023, 2, 1),
                    ),
                policies =
                    Policies(
                        retentionPolicy =
                            PolicyConfiguration(
                                policyName = "RetentionPolicy",
                                initialTargetState = null,
                                rule =
                                    PolicyRule(
                                        timeSpan = 0,
                                        triggerEvent = "InPerpetuity",
                                        action =
                                            PolicyAction(
                                                actionType = "AddTimeSpanToTarget",
                                                actionArgument = null,
                                                actionAnnotation = "KA/13089/07.01.01.03.01/2018",
                                            ),
                                    ),
                            ),
                        disclosurePolicy =
                            PolicyConfiguration(
                                policyName = "DisclosurePolicy",
                                initialTargetState = "confidential",
                                rule =
                                    PolicyRule(
                                        timeSpan = 10,
                                        triggerEvent = "YearsFromRecordCreation",
                                        action =
                                            PolicyAction(
                                                actionType = "SetTargetStateToArgument",
                                                actionArgument = "public",
                                                actionAnnotation = "Laki § 123",
                                            ),
                                    ),
                            ),
                        informationSecurityPolicy =
                            PolicyConfiguration(
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
                            ),
                        protectionPolicy =
                            PolicyConfiguration(
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
                            ),
                    ),
                caseFile =
                    CaseFile(
                        caseCreated = LocalDate.of(2023, 2, 1),
                        caseFinished = LocalDate.of(2023, 2, 1),
                    ),
            )
    )

class RecordMetadataXmlTest {

    @Test
    fun `marshalMetadata produces expected XML`() {
        val metadata = createTestMetadataInstance()
        val xml = marshalMetadata(metadata, TEST_MAIN_NAMESPACE, TEST_POLICY_NAMESPACE)
        val expected =
            this::class
                .java
                .classLoader
                .getResource("archival/expected-metadata.xml")!!
                .readText()
                .lines()
                .first()
        assertEquals(expected, xml)
    }
}
