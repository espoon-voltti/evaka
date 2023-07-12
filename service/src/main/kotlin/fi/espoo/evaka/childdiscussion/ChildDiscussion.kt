// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.ChildId
import java.time.LocalDate

data class ChildDiscussion(
    val id: ChildDiscussionId,
    val childId: ChildId,
    val offeredDate: LocalDate?,
    val heldDate: LocalDate?,
    val counselingDate: LocalDate?
)

data class ChildDiscussionBody(
    val offeredDate: LocalDate?,
    val heldDate: LocalDate?,
    val counselingDate: LocalDate?
)
