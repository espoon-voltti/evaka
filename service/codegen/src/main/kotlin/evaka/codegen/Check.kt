// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import evaka.codegen.actionenum.checkGeneratedActionEnumTypes
import evaka.codegen.apitypes.checkGeneratedApiTypes
import org.slf4j.LoggerFactory
import kotlin.io.path.div

fun main() {
    (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("ROOT").level = Level.INFO
    val generatedPath = locateGeneratedDirectory()
    checkGeneratedActionEnumTypes(generatedPath)
    checkGeneratedApiTypes(generatedPath / "api-types")
    checkLanguages(generatedPath / "language.ts")
}
