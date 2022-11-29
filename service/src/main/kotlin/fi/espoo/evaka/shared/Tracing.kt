// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import io.opentracing.tag.StringTag

object Tracing {
    val enduserIdHash = StringTag("enduser.idhash")
}
