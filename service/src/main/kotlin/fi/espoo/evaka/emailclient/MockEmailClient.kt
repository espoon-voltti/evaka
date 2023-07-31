// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MockEmailClient : IEmailClient {
    override val whitelist: List<Regex>? = null

    companion object {
        private val data = mutableListOf<MockEmail>()
        private val lock = ReentrantReadWriteLock()

        val emails: List<MockEmail>
            get() = lock.read { data.toList() }

        fun clear() = lock.write { data.clear() }

        fun addEmail(email: MockEmail) = lock.write { data.add(email) }

        fun getEmail(toAddress: String): MockEmail? =
            lock.read { emails.find { email -> email.toAddress == toAddress } }
    }

    override fun sendValidatedEmail(
        toAddress: String,
        fromAddress: String,
        content: EmailContent,
        traceId: String,
    ) {
        logger.info { "Mock sending email (personId: $traceId toAddress: $toAddress)" }
        addEmail(
            MockEmail(traceId, toAddress, fromAddress, content.subject, content.html, content.text)
        )
    }
}

data class MockEmail(
    val traceId: String,
    val toAddress: String,
    val fromAddress: String,
    val subject: String,
    val htmlBody: String,
    val textBody: String
)
