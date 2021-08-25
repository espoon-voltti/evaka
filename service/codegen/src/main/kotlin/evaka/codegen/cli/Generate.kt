// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.cli

import evaka.codegen.generatedFiles
import kotlin.io.path.div

fun main() {
    val root = locateRoot()
    for (definition in generatedFiles) {
        val file = root / definition.name
        definition.generateTo(file)
        logger.info("Generated $file")
    }
}
