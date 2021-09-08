// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum.cli

import evaka.codegen.actionenum.generatedFiles
import kotlin.io.path.div

fun generateActionEnumTypes() {
    val root = locateRoot()
    for (definition in generatedFiles) {
        val file = root / definition.name
        definition.generateTo(file)
        logger.info("Generated $file")
    }
}
