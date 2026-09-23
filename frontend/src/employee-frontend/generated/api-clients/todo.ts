// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { TodoItem } from 'lib-common/generated/api-types/todo'
import type { TodoItemId } from 'lib-common/generated/api-types/shared'
import type { TodoItemRequest } from 'lib-common/generated/api-types/todo'
import { client } from '../../api/client'
import { deserializeJsonTodoItem } from 'lib-common/generated/api-types/todo'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.todo.TodoItemController.createTodoItem
*/
export async function createTodoItem(
  request: {
    body: TodoItemRequest
  }
): Promise<TodoItemId> {
  const { data: json } = await client.request<JsonOf<TodoItemId>>({
    url: uri`/employee/todo-items`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<TodoItemRequest>
  })
  return json
}


/**
* Generated from evaka.core.todo.TodoItemController.deleteTodoItem
*/
export async function deleteTodoItem(
  request: {
    id: TodoItemId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/todo-items/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.todo.TodoItemController.getTodoItems
*/
export async function getTodoItems(): Promise<TodoItem[]> {
  const { data: json } = await client.request<JsonOf<TodoItem[]>>({
    url: uri`/employee/todo-items`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonTodoItem(e))
}
