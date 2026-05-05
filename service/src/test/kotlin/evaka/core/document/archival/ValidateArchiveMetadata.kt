// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.archival

import evaka.instance.espoo.archival.marshalMetadata
import java.io.File
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

@Tag("schemaValidation")
class ValidateArchiveMetadata {

    @Test
    fun `generated metadata XML validates against XSD schema`() {
        val schemaFile =
            System.getProperty("sarma.schema.file")?.let(::File)
                ?: throw IllegalStateException(
                    "Set -Dsarma.schema.file=/path/to/top-level.xsd " +
                        "-Dsarma.main.namespace=<main-ns> -Dsarma.policy.namespace=<policy-ns> " +
                        "to run this test"
                )
        require(schemaFile.exists()) { "Schema file not found: ${schemaFile.absolutePath}" }

        val mainNamespace =
            System.getProperty("sarma.main.namespace")
                ?: throw IllegalStateException("Set -Dsarma.main.namespace=<main-ns>")
        val policyNamespace =
            System.getProperty("sarma.policy.namespace")
                ?: throw IllegalStateException("Set -Dsarma.policy.namespace=<policy-ns>")

        val metadata = createTestMetadataInstance()
        val xml = marshalMetadata(metadata, mainNamespace, policyNamespace)
        val prettyXml = prettyPrintXml(xml)

        val schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
        val schema = schemaFactory.newSchema(StreamSource(schemaFile))
        val validator = schema.newValidator()
        val errors = mutableListOf<SAXException>()
        validator.errorHandler =
            object : org.xml.sax.ErrorHandler {
                override fun warning(exception: SAXParseException) {
                    errors.add(exception)
                }

                override fun error(exception: SAXParseException) {
                    errors.add(exception)
                }

                override fun fatalError(exception: SAXParseException) {
                    errors.add(exception)
                }
            }
        try {
            validator.validate(StreamSource(prettyXml.reader()))
        } catch (e: SAXException) {
            errors.add(e)
        }

        if (errors.isNotEmpty()) {
            throw AssertionError(
                "XML validation errors:\n${errors.joinToString("\n") { it.message ?: it.toString() }}\n\nXML:\n$prettyXml"
            )
        }
    }

    private fun prettyPrintXml(xml: String): String {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer =
            transformerFactory.newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            }
        val source = StreamSource(xml.reader())
        val result = StringWriter()
        transformer.transform(source, StreamResult(result))
        return result.toString()
    }
}
