// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.archival

import evaka.instance.espoo.archival.model.*
import java.io.StringWriter
import java.time.LocalDate
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

private const val PFX_MAIN = "ns0"
private const val PFX_POLICY = "at0"

fun marshalMetadata(
    metadata: RecordMetadataInstance,
    mainNamespace: String,
    policyNamespace: String,
): String {
    val writer = StringWriter()
    val xml = XMLOutputFactory.newInstance().createXMLStreamWriter(writer)
    MetadataWriter(xml, mainNamespace, policyNamespace).write(metadata)
    xml.close()
    return writer.toString()
}

private class MetadataWriter(
    private val xml: XMLStreamWriter,
    private val mainNs: String,
    private val policyNs: String,
) {
    fun write(metadata: RecordMetadataInstance) {
        xml.writeStartDocument("UTF-8", "1.0")
        xml.writeStartElement(PFX_MAIN, "RecordMetadataInstance", mainNs)
        xml.writeNamespace(PFX_MAIN, mainNs)
        xml.writeNamespace(PFX_POLICY, policyNs)

        val sm = metadata.standardMetadata
        mainElement("StandardMetadata") {
            mainElement("metadataMasterVersion") {
                mainElement("masterName") {
                    xml.writeCharacters(sm.metadataMasterVersion.masterName)
                }
                mainElement("versionNumber") {
                    xml.writeCharacters(sm.metadataMasterVersion.versionNumber)
                }
            }
            mainElement("virtualArchiveId") { xml.writeCharacters(sm.virtualArchiveId) }
            writeRecordIdentifiers(sm.recordIdentifiers)
            writeDocumentDescription(sm.documentDescription)
            writeFormat(sm.format)
            writeCreation(sm.creation)
            writePolicies(sm.policies)
            writeCaseFile(sm.caseFile)
        }

        xml.writeEndElement() // RecordMetadataInstance
        xml.writeEndDocument()
    }

    private fun mainElement(localName: String, block: () -> Unit) {
        xml.writeStartElement(PFX_MAIN, localName, mainNs)
        block()
        xml.writeEndElement()
    }

    private fun policyElement(localName: String, block: () -> Unit) {
        xml.writeStartElement(PFX_POLICY, localName, policyNs)
        block()
        xml.writeEndElement()
    }

    private fun mainTextElement(localName: String, value: String?) {
        if (value != null) {
            mainElement(localName) { xml.writeCharacters(value) }
        }
    }

    private fun policyTextElement(localName: String, value: String?) {
        if (value != null) {
            policyElement(localName) { xml.writeCharacters(value) }
        }
    }

    private fun mainDateElement(localName: String, value: LocalDate?) {
        if (value != null) {
            mainElement(localName) { xml.writeCharacters(value.toString()) }
        }
    }

    private fun writeRecordIdentifiers(ids: RecordIdentifiers) {
        mainElement("recordIdentifiers") {
            mainTextElement("CaseIdentifier", ids.caseIdentifier)
            mainElement("RecordIdentifier") { xml.writeCharacters(ids.recordIdentifier) }
        }
    }

    private fun writeDocumentDescription(desc: DocumentDescription) {
        mainElement("documentDescription") {
            mainElement("title") { xml.writeCharacters(desc.title) }
            mainTextElement("documentType", desc.documentType)
            mainTextElement("documentTypeSpecifier", desc.documentTypeSpecifier)
            mainTextElement("registerName", desc.registerName)
            mainTextElement("firstName", desc.firstName)
            mainTextElement("lastName", desc.lastName)
            mainTextElement("socialSecurityNumber", desc.socialSecurityNumber)
            mainDateElement("birthDate", desc.birthDate)
            mainTextElement("personalData", desc.personalData?.xmlValue)
            mainTextElement("personalDataCollectionReason", desc.personalDataCollectionReason)
            mainTextElement("language", desc.language)
            mainTextElement("dataManagement", desc.dataManagement)
            mainTextElement("dataSource", desc.dataSource)
            if (desc.agents.isNotEmpty()) {
                mainElement("Agents") {
                    desc.agents.forEach { agent ->
                        mainElement("Agent") {
                            mainTextElement("Role", agent.role)
                            mainTextElement("Name", agent.name)
                            mainTextElement("CorporateName", agent.corporateName)
                        }
                    }
                }
            }
        }
    }

    private fun writeFormat(format: Format) {
        mainElement("format") {
            mainElement("recordType") { xml.writeCharacters(format.recordType.xmlValue) }
            mainTextElement("mimeType", format.mimeType?.xmlValue)
            mainTextElement("fileFormat", format.fileFormat?.xmlValue)
            mainTextElement("fileName", format.fileName)
        }
    }

    private fun writeCreation(creation: Creation) {
        mainElement("creation") {
            mainTextElement("originatingSystem", creation.originatingSystem)
            mainDateElement("created", creation.created)
        }
    }

    private fun writePolicies(policies: Policies) {
        mainElement("policies") {
            mainElement("retentionPolicy") { writePolicyConfiguration(policies.retentionPolicy) }
            mainElement("disclosurePolicy") { writePolicyConfiguration(policies.disclosurePolicy) }
            mainElement("informationSecurityPolicy") {
                writePolicyConfiguration(policies.informationSecurityPolicy)
            }
            mainElement("protectionPolicy") { writePolicyConfiguration(policies.protectionPolicy) }
        }
    }

    private fun writePolicyConfiguration(config: PolicyConfiguration) {
        policyElement("PolicyConfiguration") {
            policyElement("PolicyName") { xml.writeCharacters(config.policyName) }
            policyTextElement("InitialTargetState", config.initialTargetState)
            policyElement("Rules") {
                policyElement("Rule") {
                    policyElement("TimeSpan") {
                        xml.writeCharacters(config.rule.timeSpan.toString())
                    }
                    policyElement("TriggerEvent") { xml.writeCharacters(config.rule.triggerEvent) }
                    policyElement("Action") {
                        policyElement("ActionType") {
                            xml.writeCharacters(config.rule.action.actionType)
                        }
                        if (config.rule.action.actionArgument != null) {
                            policyElement("ActionArguments") {
                                policyElement("ActionArgument") {
                                    xml.writeCharacters(config.rule.action.actionArgument)
                                }
                            }
                        }
                        policyTextElement("ActionAnnotation", config.rule.action.actionAnnotation)
                    }
                }
            }
        }
    }

    private fun writeCaseFile(caseFile: CaseFile) {
        mainElement("CaseFile") {
            mainDateElement("Case_Created", caseFile.caseCreated)
            mainDateElement("Case_Finished", caseFile.caseFinished)
        }
    }
}
