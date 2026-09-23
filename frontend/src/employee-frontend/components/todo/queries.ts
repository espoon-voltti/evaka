// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createTodoItem,
  deleteTodoItem,
  getTodoItems
} from '../../generated/api-clients/todo'

const q = new Queries()

export const todoItemsQuery = q.query(getTodoItems)

export const createTodoItemMutation = q.mutation(createTodoItem, [
  todoItemsQuery
])

export const deleteTodoItemMutation = q.mutation(deleteTodoItem, [
  todoItemsQuery
])
