// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.div

fun locateGeneratedDirectory(): Path {
    // the working directory is expected to be the "service" directory
    val workingDir = Path("")
    val path = (workingDir / "../frontend/src/lib-common/generated").absolute().normalize()
    return path
}
