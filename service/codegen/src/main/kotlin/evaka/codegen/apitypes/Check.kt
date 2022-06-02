// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import mu.KotlinLogging
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.system.exitProcess

val logger = KotlinLogging.logger {}

fun checkGeneratedApiTypes() {
    val root = locateRoot()
    getApiTypesInTypeScript(root).entries.forEach { (path, content) ->
        val currentContent = path.readText()
        val relativePath = path.relativeTo(root)
        if (content == currentContent) {
            logger.info("Generated api types up to date ($relativePath)")
        } else {
            logger.error("Generated api types were not up to date ($relativePath)")
            exitProcess(1)
        }
    }
}
