//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.updateEnabledEmailTypes
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.testAdult_1
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailClientTest : PureJdbiTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertTestPerson(testAdult_1) }
    }

    @Test
    fun `messages are not sent to invalid email addresses`() {
        val client = TestEmailClient()
        listOf("test", "test@example", "test@example.", "test@example.c").forEach { address ->
            sendEmail(client, toAddress = address)
        }
        assertEquals(emptyList(), client.sentEmails)
    }

    @Test
    fun `whitelisting works`() {
        val clientWithWhitelist = TestEmailClient(listOf(".*@espoo\\.fi".toRegex()))
        listOf("test@espoo.fi", "test@example.com").forEach { address ->
            sendEmail(clientWithWhitelist, toAddress = address)
        }
        assertEquals(listOf("test@espoo.fi"), clientWithWhitelist.sentEmails)

        val clientWithoutWhitelist = TestEmailClient()
        listOf("test@espoo.fi", "test@example.com").forEach { address ->
            sendEmail(clientWithoutWhitelist, toAddress = address)
        }
        assertEquals(listOf("test@espoo.fi", "test@example.com"), clientWithoutWhitelist.sentEmails)
    }

    @Test
    fun `receiver's enabled email notification types are respected`() {
        // Not set -> all messages are sent
        var client = TestEmailClient()
        EmailMessageType.values().forEach { type -> sendEmail(client, emailType = type) }
        assertEquals(EmailMessageType.values().size, client.sentEmails.size)

        // Only some notification types are enabled
        client = TestEmailClient()
        db.transaction { tx ->
            tx.updateEnabledEmailTypes(
                testAdult_1.id,
                listOf(
                    EmailMessageType.TRANSACTIONAL,
                    EmailMessageType.BULLETIN_NOTIFICATION,
                    EmailMessageType.DOCUMENT_NOTIFICATION
                )
            )
        }
        EmailMessageType.values().forEach { type ->
            sendEmail(client, emailType = type, toAddress = "$type@example.com")
        }
        assertEquals(
            listOf(
                "TRANSACTIONAL@example.com",
                "BULLETIN_NOTIFICATION@example.com",
                "DOCUMENT_NOTIFICATION@example.com"
            ),
            client.sentEmails
        )
    }

    private fun sendEmail(
        client: IEmailClient,
        emailType: EmailMessageType = EmailMessageType.TRANSACTIONAL,
        toAddress: String = "test@example.com"
    ) {
        client.sendEmail(
            db,
            testAdult_1.id,
            emailType,
            toAddress,
            "sender@example.com",
            testContent,
            "traceid"
        )
    }
}

private class TestEmailClient(override val whitelist: List<Regex>? = null) : IEmailClient {
    var sentEmails = mutableListOf<String>()

    override fun sendValidatedEmail(
        toAddress: String,
        fromAddress: String,
        content: EmailContent,
        traceId: String,
    ) {
        sentEmails.add(toAddress)
    }
}

private val testContent = EmailContent(subject = "subject", html = "html", text = "text")
