// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createChildDailyNote,
  createChildStickyNote,
  createGroupNote,
  deleteChildDailyNote,
  deleteChildStickyNote,
  deleteGroupNote,
  getNotesByGroup,
  updateChildDailyNote,
  updateChildStickyNote,
  updateGroupNote
} from '../../../../generated/api-clients/note'

const q = new Queries()

export const notesByGroupQuery = q.query(getNotesByGroup)

export const createChildDailyNoteMutation = q.mutation(createChildDailyNote, [
  notesByGroupQuery.prefix
])

export const updateChildDailyNoteMutation = q.mutation(updateChildDailyNote, [
  notesByGroupQuery.prefix
])

export const deleteChildDailyNoteMutation = q.mutation(deleteChildDailyNote, [
  notesByGroupQuery.prefix
])

export const createGroupNoteMutation = q.mutation(createGroupNote, [
  notesByGroupQuery.prefix
])

export const updateGroupNoteMutation = q.mutation(updateGroupNote, [
  notesByGroupQuery.prefix
])

export const deleteGroupNoteMutation = q.mutation(deleteGroupNote, [
  notesByGroupQuery.prefix
])

export const createChildStickyNoteMutation = q.mutation(createChildStickyNote, [
  notesByGroupQuery.prefix
])

export const updateChildStickyNoteMutation = q.mutation(updateChildStickyNote, [
  notesByGroupQuery.prefix
])

export const deleteChildStickyNoteMutation = q.mutation(deleteChildStickyNote, [
  notesByGroupQuery.prefix
])
