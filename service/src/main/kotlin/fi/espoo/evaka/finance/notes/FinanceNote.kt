// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.finance.notes

import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class FinanceNote(
    val id: FinanceNoteId,
    val content: String,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUserId,
    val createdByName: String,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUserId,
    val modifiedByName: String,
)
