// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalId
import java.time.Instant
import java.util.UUID

data class Employee(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    val created: Instant,
    val updated: Instant?,
    val pin: String?
)
