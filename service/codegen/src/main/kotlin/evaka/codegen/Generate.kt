// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import evaka.codegen.actionenum.generateActionEnumTypes
import evaka.codegen.apitypes.generateApiTypes
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.system.exitProcess
import mu.KotlinLogging
import org.slf4j.LoggerFactory

private val logger = KotlinLogging.logger {}

fun main() {
    (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("ROOT").level = Level.INFO
    val generatedPath = locateGeneratedDirectory()
    if (!generatedPath.exists()) {
        logger.error("Root path $generatedPath does not exist -> aborting")
        exitProcess(1)
    }
    generateActionEnumTypes(generatedPath)
    generateApiTypes(generatedPath / "api-types")
    generateLanguages(generatedPath / "language.ts")
}
