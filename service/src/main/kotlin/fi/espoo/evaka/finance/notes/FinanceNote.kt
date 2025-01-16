// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.finance.notes

import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.core.mapper.Nested

// TODO imports
// k

data class FinanceNote(
    val id: FinanceNoteId,
    val content: String,
    val createdAt: HelsinkiDateTime,
    @Nested("created_by") val createdBy: EvakaUser,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
    // TODO other fields? Message content and thread?
)
