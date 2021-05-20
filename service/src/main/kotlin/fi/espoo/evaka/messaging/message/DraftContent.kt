// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import java.util.UUID

data class DraftContent(
    val title: String? = null,
    val content: String? = null,
    val recipients: Set<UUID>? = null,
)
