// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import mu.KLogger
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.MarkerFactory

private val auditMarker = MarkerFactory.getMarker("VTJ_EVENT")

fun KLogger.auditVTJ(m: () -> Any?) {
    auditVTJ(emptyMap(), m)
}

fun KLogger.auditVTJ(
    args: Map<String, Any?>,
    m: () -> Any?
) {
    if (isWarnEnabled) warn(auditMarker, m.toStringSafe(), StructuredArguments.entries(args))
}

fun KLogger.auditVTJ(
    t: Throwable,
    m: () -> Any?
) {
    auditVTJ(emptyMap(), t, m)
}

fun KLogger.auditVTJ(
    args: Map<String, Any?>,
    t: Throwable,
    m: () -> Any?
) {
    if (isWarnEnabled) warn(auditMarker, m.toStringSafe(), t, StructuredArguments.entries(args))
}
