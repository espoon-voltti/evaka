// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenpasskey

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class CitizenPasskeyStoreFile(
    val personId: PersonId,
    val credentials: List<CitizenPasskeyCredential>,
)

data class CitizenPasskeyCredential(
    val credentialId: String,
    val publicKey: String,
    val signCounter: Long,
    val transports: List<String>,
    val createdAt: HelsinkiDateTime,
    val lastUsedAt: HelsinkiDateTime?,
    val label: String,
    val deviceHint: String?,
)
