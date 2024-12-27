// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun checkGeneratedActionEnumTypes(target: Path) {
    var errors = 0
    for (definition in generatedFiles) {
        val file = target / definition.name
        if (definition.generate() != file.readText()) {
            logger.error { "File is not up to date: $file" }
            errors += 1
        }
    }
    if (errors > 0) {
        logger.error { "$errors were not up to date" }
        exitProcess(1)
    } else {
        logger.info { "All files up to date" }
    }
}

fun generateActionEnumTypes(target: Path) {
    for (definition in generatedFiles) {
        val file = target / definition.name
        definition.generateTo(file)
        logger.info { "Generated $file" }
    }
}
