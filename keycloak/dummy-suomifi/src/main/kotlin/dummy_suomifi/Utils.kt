// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dummy_suomifi

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.util.zip.InflaterOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun prettyPrintXml(node: Node): String {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    val output = StringWriter()
    transformer.transform(DOMSource(node), StreamResult(output))
    return output.toString()
}

fun ByteArray.parseXml(): Document = inputStream().use {
    DocumentBuilderFactory.newNSInstance().newDocumentBuilder().parse(it)
}

fun ByteArray.inflate(): ByteArray = ByteArrayOutputStream().also { output ->
    InflaterOutputStream(output).use { it.write(this) }
}.toByteArray()
