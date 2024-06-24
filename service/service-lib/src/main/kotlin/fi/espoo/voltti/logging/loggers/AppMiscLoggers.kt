// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import mu.KLogger
import net.logstash.logback.argument.StructuredArguments

/**
 * Extensions to allow adding meta-fields to app-misc logs without requiring StructuredArguments wrapper
 * and an unnecessary dependency to net.logstash.logback.
 */

fun KLogger.trace(
    meta: Map<String, Any?>,
    m: () -> Any?
) {
    if (isTraceEnabled) trace(m.toStringSafe(), metaToLoggerArgs(meta))
}

fun KLogger.debug(
    meta: Map<String, Any?>,
    m: () -> Any?
) {
    if (isDebugEnabled) debug(m.toStringSafe(), metaToLoggerArgs(meta))
}

fun KLogger.info(
    meta: Map<String, Any?>,
    m: () -> Any?
) {
    if (isInfoEnabled) info(m.toStringSafe(), metaToLoggerArgs(meta))
}

fun KLogger.warn(
    meta: Map<String, Any?>,
    m: () -> Any?
) {
    if (isWarnEnabled) warn(m.toStringSafe(), metaToLoggerArgs(meta))
}

fun KLogger.error(
    meta: Map<String, Any?>,
    m: () -> Any?
) {
    if (isErrorEnabled) error(m.toStringSafe(), metaToLoggerArgs(meta))
}

fun KLogger.error(
    error: Any,
    meta: Map<String, Any?>,
    m: () -> Any?
) {
    if (isErrorEnabled) error(m.toStringSafe(), metaToLoggerArgs(meta), error)
}

private fun metaToLoggerArgs(meta: Map<String, Any?>) = StructuredArguments.entries(mapOf("meta" to meta))
