// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import mu.KotlinLogging
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.system.exitProcess

val logger = KotlinLogging.logger {}

fun checkGeneratedApiTypes() {
    val file = locateRoot() / "api-types.d.ts"
    val expectedContent = getApiTypesInTypeScript()
    val currentContent = file.readText()
    if (expectedContent == currentContent) {
        logger.info("Generated api types up to date")
    } else {
        logger.error("Generated api types were not up to date")
        exitProcess(1)
    }
}
