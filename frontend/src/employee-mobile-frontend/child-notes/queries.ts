// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDailyNoteBody,
  ChildStickyNoteBody,
  GroupNoteBody
} from 'lib-common/generated/api-types/note'
import { mutation } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { childrenQuery } from '../child-attendance/queries'

import {
  createChildDailyNote,
  createChildStickyNote,
  createGroupNote,
  deleteChildDailyNote,
  deleteChildStickyNote,
  deleteGroupNote,
  updateChildDailyNote,
  updateChildStickyNote,
  updateGroupNote
} from './api'

export const createGroupNoteMutation = mutation({
  api: ({
    groupId,
    body
  }: {
    unitId: UUID
    groupId: UUID
    body: GroupNoteBody
  }) => createGroupNote({ groupId, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateGroupNoteMutation = mutation({
  api: ({ id, body }: { unitId: UUID; id: UUID; body: GroupNoteBody }) =>
    updateGroupNote({ id, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteGroupNoteMutation = mutation({
  api: ({ id }: { unitId: UUID; id: UUID }) => deleteGroupNote(id),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const createChildDailyNoteMutation = mutation({
  api: ({
    childId,
    body
  }: {
    unitId: UUID
    childId: UUID
    body: ChildDailyNoteBody
  }) =>
    createChildDailyNote({
      childId,
      body
    }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateChildDailyNoteMutation = mutation({
  api: ({ id, body }: { unitId: UUID; id: UUID; body: ChildDailyNoteBody }) =>
    updateChildDailyNote({ id, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildDailyNoteMutation = mutation({
  api: ({ id }: { unitId: UUID; id: UUID }) => deleteChildDailyNote(id),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const createChildStickyNoteMutation = mutation({
  api: ({
    childId,
    body
  }: {
    unitId: UUID
    childId: UUID
    body: ChildStickyNoteBody
  }) => createChildStickyNote({ childId, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateChildStickyNoteMutation = mutation({
  api: ({ id, body }: { unitId: UUID; id: UUID; body: ChildStickyNoteBody }) =>
    updateChildStickyNote({ id, body }),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildStickyNoteMutation = mutation({
  api: ({ id }: { unitId: UUID; id: UUID }) => deleteChildStickyNote(id),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})
