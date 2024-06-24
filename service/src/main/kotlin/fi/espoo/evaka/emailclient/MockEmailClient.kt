// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MockEmailClient : EmailClient {
    companion object {
        private val data = mutableListOf<Email>()
        private val lock = ReentrantReadWriteLock()

        val emails: List<Email>
            get() = lock.read { data.toList() }

        fun clear() = lock.write { data.clear() }

        fun addEmail(email: Email) = lock.write { data.add(email) }

        fun getEmail(toAddress: String): Email? = lock.read { emails.find { email -> email.toAddress == toAddress } }
    }

    override fun send(email: Email) {
        logger.info {
            "Mock sending email (personId: ${email.traceId} toAddress: ${email.toAddress})"
        }
        addEmail(email)
    }
}
