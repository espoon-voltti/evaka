// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.msg.controllers.PdfSendMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [JacksonAutoConfiguration::class])
class PdfSendProcessorTest {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `Parse pdf message is successful`() {
        val pdfMessage = parsePdfMessageFromJson()
        assertThat(pdfMessage.ssn).isEqualTo("101010-1010")
    }

    @Test
    fun `toString does not contain person information`() {
        val pdfMessage = parsePdfMessageFromJson()
        val str = pdfMessage.toString()
        assertThat(str).contains("documentUri")
        assertThat(str).doesNotContain("ssn", "firstName", "lastName", "streetAddress")
    }

    @Test
    fun `print pdf message does not contain person information`() {
        val pdfMessage = parsePdfMessageFromJson()
        val str = "Decoded message $pdfMessage."
        assertThat(str).doesNotContain("ssn", "firstName", "lastName", "streetAddress")
    }

    private fun parsePdfMessageFromJson(): PdfSendMessage {
        val json = createWrappedPdfMessage()
        return objectMapper.readValue(json, PdfSendMessage::class.java)
    }

    private fun createWrappedPdfMessage() =
        """{
            "documentUri": "s3://evaka-clubdecisions-dev/fe23ad56-4eff-11e9-be2b-b3aff839cc60",
            "documentId": "fe23ad56-4eff-11e9-be2b-b3aff839cc60",
            "documentDisplayName": "Kerhopäätös.pdf",
            "ssn": "101010-1010",
            "firstName": "Erkki Etunimi",
            "lastName": "Sukuminen",
            "language": "fi",
            "streetAddress": "Espoonkatu 1 A",
            "postalCode": "00280",
            "postOffice": "Espoo",
            "countryCode": "FI",
            "messageHeader": "Testiviestin otsikko",
            "messageContent": "Testiviestin sisältö"
        }"""
}
