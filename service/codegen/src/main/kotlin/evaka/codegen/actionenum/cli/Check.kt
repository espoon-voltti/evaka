// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum.cli

import evaka.codegen.actionenum.generatedFiles
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.system.exitProcess

fun checkGeneratedActionEnumTypes() {
    val root = locateRoot()
    var errors = 0
    for (definition in generatedFiles) {
        val file = root / definition.name
        if (definition.generate() != file.readText()) {
            logger.error("File is not up to date: $file")
            errors += 1
        }
    }
    if (errors > 0) {
        logger.error("$errors were not up to date")
        exitProcess(1)
    } else {
        logger.info("All files up to date")
    }
}
