// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { TodoItemId } from './shared'

/**
* Generated from evaka.core.todo.TodoItem
*/
export interface TodoItem {
  createdAt: HelsinkiDateTime
  deadline: LocalDate | null
  description: string
  id: TodoItemId
}

/**
* Generated from evaka.core.todo.TodoItemRequest
*/
export interface TodoItemRequest {
  deadline: LocalDate | null
  description: string
}


export function deserializeJsonTodoItem(json: JsonOf<TodoItem>): TodoItem {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    deadline: (json.deadline != null) ? LocalDate.parseIso(json.deadline) : null
  }
}


export function deserializeJsonTodoItemRequest(json: JsonOf<TodoItemRequest>): TodoItemRequest {
  return {
    ...json,
    deadline: (json.deadline != null) ? LocalDate.parseIso(json.deadline) : null
  }
}
