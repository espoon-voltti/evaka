// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import fi.espoo.evaka.shared.domain.HelsinkiDateTime

val fileHeader =
    """
// SPDX-FileCopyrightText: 2017-${HelsinkiDateTime.now().year} City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
""".trimStart()

fun String.skipFileHeader() = lineSequence().drop(fileHeader.lineSequence().count()).joinToString("\n")
