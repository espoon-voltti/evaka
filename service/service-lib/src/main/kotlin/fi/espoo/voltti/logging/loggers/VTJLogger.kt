// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KMarkerFactory
import net.logstash.logback.argument.StructuredArguments

private val auditMarker = KMarkerFactory.getMarker("VTJ_EVENT")

fun KLogger.auditVTJ(m: () -> Any?) = auditVTJ(emptyMap(), m)

fun KLogger.auditVTJ(args: Map<String, Any?>, m: () -> Any?) =
    atWarn(auditMarker) {
        message = m.toStringSafe()
        arguments = arrayOf(StructuredArguments.entries(args))
    }

fun KLogger.auditVTJ(t: Throwable, m: () -> Any?) {
    auditVTJ(emptyMap(), t, m)
}

fun KLogger.auditVTJ(args: Map<String, Any?>, t: Throwable, m: () -> Any?) =
    atWarn(auditMarker) {
        message = m.toStringSafe()
        cause = t
        arguments = arrayOf(StructuredArguments.entries(args))
    }
