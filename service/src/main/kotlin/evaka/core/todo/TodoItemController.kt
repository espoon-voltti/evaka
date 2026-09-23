// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.todo

import evaka.core.shared.TodoItemId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TodoItemController(private val accessControl: AccessControl) {
    @GetMapping("/employee/todo-items")
    fun getTodoItems(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<TodoItem> {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.READ_TODO_ITEMS,
                )
                tx.getTodoItems(user.id)
            }
        }
    }

    @PostMapping("/employee/todo-items")
    fun createTodoItem(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: TodoItemRequest,
    ): TodoItemId {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.CREATE_TODO_ITEM,
                )
                tx.insertTodoItem(user.id, clock.now(), body)
            }
        }
    }

    @DeleteMapping("/employee/todo-items/{id}")
    fun deleteTodoItem(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: TodoItemId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.TodoItem.DELETE, id)
                tx.deleteTodoItem(id)
            }
        }
    }
}
