// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.daycare.domain.Language
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

class MockEmailClient : IEmailClient {
    override fun sendApplicationEmail(personId: UUID, toAddress: String?, language: Language) {
        if (validateEmail(personId, toAddress)) {
            applicationEmails.add(MockApplicationEmail(personId, toAddress!!, language.name)) // toAddress can't be null after validation
            logger.info { "Mock sending application email (personId: $personId language: ${language.name})" }
        }
    }

    companion object {
        val applicationEmails = mutableListOf<MockApplicationEmail>()
    }
}

data class MockApplicationEmail(
    val personId: UUID,
    val toAddress: String,
    val language: String
)
