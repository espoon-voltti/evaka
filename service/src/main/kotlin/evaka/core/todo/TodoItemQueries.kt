// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.todo

import evaka.core.shared.EmployeeId
import evaka.core.shared.TodoItemId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime

fun Database.Read.getTodoItems(employeeId: EmployeeId): List<TodoItem> = createQuery {
    sql(
        """
SELECT id, description, deadline, created_at
FROM todo_item
WHERE employee_id = ${bind(employeeId)}
ORDER BY deadline NULLS LAST, created_at
"""
    )
}
    .toList()

fun Database.Transaction.insertTodoItem(
    employeeId: EmployeeId,
    now: HelsinkiDateTime,
    request: TodoItemRequest,
): TodoItemId = createQuery {
    sql(
        """
INSERT INTO todo_item (created_at, employee_id, description, deadline)
VALUES (${bind(now)}, ${bind(employeeId)}, ${bind(request.description)}, ${bind(request.deadline)})
RETURNING id
"""
    )
}
    .exactlyOne()

fun Database.Transaction.deleteTodoItem(id: TodoItemId) {
    createUpdate { sql("DELETE FROM todo_item WHERE id = ${bind(id)}") }.updateExactlyOne()
}
