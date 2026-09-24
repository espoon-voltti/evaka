// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.todo

import evaka.core.shared.TodoItemId
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class TodoItem(
    val id: TodoItemId,
    val description: String,
    val deadline: LocalDate?,
    val createdAt: HelsinkiDateTime,
)

data class TodoItemRequest(val description: String, val deadline: LocalDate?)
