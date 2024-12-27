// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KMarkerFactory
import io.github.oshai.kotlinlogging.Marker
import net.logstash.logback.argument.StructuredArguments

val AUDIT_MARKER: Marker = KMarkerFactory.getMarker("AUDIT_EVENT")

fun KLogger.audit(m: () -> Any?) = audit(emptyMap(), m)

fun KLogger.audit(t: Throwable, m: () -> Any?) = warn(t, AUDIT_MARKER, m)

fun KLogger.audit(args: Map<String, Any?>, m: () -> Any?) =
    atWarn(AUDIT_MARKER) {
        message = m.toStringSafe()
        arguments = arrayOf(StructuredArguments.entries(args))
    }
