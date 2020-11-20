// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import mu.KLogger
import net.logstash.logback.argument.StructuredArguments

/**
 * Extensions to allow adding meta-fields (and technically any other) to app-misc logs
 * without requiring StructuredArguments wrapper and a dependency to net.logstash.logback.
 */

fun KLogger.trace(args: Map<String, Any?>, m: () -> Any?) {
    if (isTraceEnabled) trace(m.toStringSafe(), StructuredArguments.entries(args))
}

fun KLogger.debug(args: Map<String, Any?>, m: () -> Any?) {
    if (isDebugEnabled) debug(m.toStringSafe(), StructuredArguments.entries(args))
}

fun KLogger.info(args: Map<String, Any?>, m: () -> Any?) {
    if (isInfoEnabled) info(m.toStringSafe(), StructuredArguments.entries(args))
}

fun KLogger.warn(args: Map<String, Any?>, m: () -> Any?) {
    if (isWarnEnabled) warn(m.toStringSafe(), StructuredArguments.entries(args))
}

fun KLogger.error(args: Map<String, Any?>, m: () -> Any?) {
    if (isErrorEnabled) error(m.toStringSafe(), StructuredArguments.entries(args))
}
