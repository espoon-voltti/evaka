// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.todo

import evaka.core.FullApplicationTest
import evaka.core.shared.TodoItemId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TodoItemControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var todoItemController: TodoItemController

    private val clock = MockEvakaClock(2026, 9, 23, 12, 0)

    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(serviceWorker) }
    }

    @Test
    fun `to-do items can be created, listed and deleted`() {
        val itemOne = TodoItemRequest(description = "Soita huoltajalle", deadline = null)
        val itemTwo =
            TodoItemRequest(
                description = "Tarkista hakemukset",
                deadline = LocalDate.of(2026, 10, 1),
            )

        val itemOneId = createTodoItem(itemOne)
        val itemTwoId = createTodoItem(itemTwo)
        assertEquals(
            setOf(
                TodoItem(
                    id = itemOneId,
                    description = itemOne.description,
                    deadline = itemOne.deadline,
                    createdAt = clock.now(),
                ),
                TodoItem(
                    id = itemTwoId,
                    description = itemTwo.description,
                    deadline = itemTwo.deadline,
                    createdAt = clock.now(),
                ),
            ),
            getTodoItems().toSet(),
        )

        deleteTodoItem(itemOneId)
        assertEquals(
            listOf(
                TodoItem(
                    id = itemTwoId,
                    description = itemTwo.description,
                    deadline = itemTwo.deadline,
                    createdAt = clock.now(),
                )
            ),
            getTodoItems(),
        )
    }

    private fun getTodoItems(): List<TodoItem> =
        todoItemController.getTodoItems(dbInstance(), serviceWorker.user, clock)

    private fun createTodoItem(request: TodoItemRequest): TodoItemId =
        todoItemController.createTodoItem(dbInstance(), serviceWorker.user, clock, request)

    private fun deleteTodoItem(id: TodoItemId) =
        todoItemController.deleteTodoItem(dbInstance(), serviceWorker.user, clock, id)
}
