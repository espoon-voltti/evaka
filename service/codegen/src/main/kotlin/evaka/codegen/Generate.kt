// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory

fun main() {
    (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("ROOT").level = Level.INFO
    generate()
}
