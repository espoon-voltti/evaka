// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.net.URI

data class CitizenPushStoreFile(
    val personId: PersonId,
    val subscriptions: List<CitizenPushSubscriptionEntry>,
)

data class CitizenPushSubscriptionEntry(
    val endpoint: URI,
    val ecdhKey: List<Byte>,
    val authSecret: List<Byte>,
    val enabledCategories: Set<CitizenPushCategory>,
    val userAgent: String?,
    val createdAt: HelsinkiDateTime,
)
