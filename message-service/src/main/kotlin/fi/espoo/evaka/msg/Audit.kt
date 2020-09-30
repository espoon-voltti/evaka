// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg

import fi.espoo.voltti.logging.loggers.audit
import mu.KotlinLogging

enum class Audit(
    private val eventCode: String,
    private val securityEvent: Boolean = false,
    private val securityLevel: String = "low"
) {
    MessageSendSfi("evaka.message.send_sfi");

    fun log(targetId: Any? = null, objectId: Any? = null) {
        logger.audit(
            mapOf(
                "eventCode" to eventCode,
                "targetId" to targetId,
                "objectId" to objectId,
                "securityLevel" to securityLevel,
                "securityEvent" to securityEvent
            )
        ) { eventCode }
    }
}

private val logger = KotlinLogging.logger {}
