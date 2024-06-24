// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import mu.KLogger
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.Marker
import org.slf4j.MarkerFactory

val AUDIT_MARKER: Marker = MarkerFactory.getMarker("AUDIT_EVENT")

fun KLogger.audit(m: () -> Any?) {
    audit(emptyMap(), m)
}

fun KLogger.audit(
    t: Throwable,
    m: () -> Any?
) {
    if (isWarnEnabled) warn(AUDIT_MARKER, m.toStringSafe(), t)
}

fun KLogger.audit(
    args: Map<String, Any?>,
    m: () -> Any?
) {
    if (isWarnEnabled) warn(AUDIT_MARKER, m.toStringSafe(), StructuredArguments.entries(args))
}
