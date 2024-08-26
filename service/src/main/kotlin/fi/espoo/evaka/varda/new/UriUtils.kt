// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import java.net.URI

fun URI.copy(host: String? = null, port: Int? = null, path: String? = null): URI =
    URI(
        this.scheme,
        this.userInfo,
        host ?: this.host,
        port ?: this.port,
        path ?: this.path,
        this.query,
        this.fragment,
    )

fun URI.ensureTrailingSlash(): URI =
    if (this.path.endsWith("/")) this else this.copy(path = this.path + "/")
