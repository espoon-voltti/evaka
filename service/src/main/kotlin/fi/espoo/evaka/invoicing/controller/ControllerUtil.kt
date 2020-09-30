// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.shared.domain.BadRequest
import java.util.UUID

data class Wrapper<T>(val data: T)

fun parseUUID(uuid: String): UUID = try {
    UUID.fromString(uuid)
} catch (e: IllegalArgumentException) {
    throw BadRequest("Given ID ($uuid) is not a valid UUID")
}
