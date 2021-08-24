// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.cli

import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.system.exitProcess

val logger = KotlinLogging.logger {}

fun locateRoot(): Path {
    // the working directory is expected to be the "service" directory
    val workingDir = Path("")
    val path = (workingDir / "../frontend/src/lib-common/generated").absolute().normalize()
    if (!path.exists()) {
        logger.error("Root path $path does not exist -> aborting")
        exitProcess(1)
    }
    return path
}
