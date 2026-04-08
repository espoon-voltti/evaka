// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.db.AuditContext
import fi.espoo.voltti.logging.loggers.audit
import io.github.oshai.kotlinlogging.KotlinLogging

enum class AuditV2(
    private val securityEvent: Boolean = false,
    private val securityLevel: String = "low",
) {
    DecisionAccept;

    private val eventCode = name

    class UseNamedArguments private constructor()

    fun log(
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        returnedResultsCount: Int?,
        context: AuditContext,
        meta: Map<String, Any?> = emptyMap(),
    ) {
        logger.audit(
            mapOf(
                "eventCode" to eventCode,
                "returnedResultsCount" to returnedResultsCount,
                "context" to
                    context.context.mapValues { (_, ids) -> ids.map { it.raw.toString() } },
                "securityLevel" to securityLevel,
                "securityEvent" to securityEvent,
            ) + if (meta.isNotEmpty()) mapOf("meta" to meta) else emptyMap()
        ) {
            eventCode
        }
    }
}

private val logger = KotlinLogging.logger {}
