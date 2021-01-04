// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.daycare.domain.Language
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val EMAIL_PATTERN = "^([\\w.%+-]+)@([\\w-]+\\.)+([\\w]{2,})\$".toRegex()

interface IEmailClient {
    fun sendApplicationEmail(personId: UUID, toAddress: String?, language: Language)

    fun validateEmail(personId: UUID, toAddress: String?): Boolean {
        if (toAddress != null && toAddress.matches(EMAIL_PATTERN)) {
            return true
        }

        logger.info { "Won't send application email due to unsuccessful email validation (personId: $personId)" }
        return false
    }
}
