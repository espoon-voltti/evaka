// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { childrenQuery } from '../child-attendance/queries'
import {
  createChildDailyNote,
  createChildStickyNote,
  createGroupNote,
  deleteChildDailyNote,
  deleteChildStickyNote,
  deleteGroupNote,
  getGroupNotes,
  updateChildDailyNote,
  updateChildStickyNote,
  updateGroupNote
} from '../generated/api-clients/note'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('notes', {
  ofGroup: (groupId: UUID) => ['group', groupId]
})

export const groupNotesQuery = query({
  api: getGroupNotes,
  queryKey: ({ groupId }) => queryKeys.ofGroup(groupId),
  options: {
    staleTime: 5 * 60 * 1000
  }
})

export const createGroupNoteMutation = mutation({
  api: createGroupNote,
  invalidateQueryKeys: ({ groupId }) => [groupNotesQuery({ groupId }).queryKey]
})

export const updateGroupNoteMutation = mutation({
  api: (arg: Arg0<typeof updateGroupNote> & { groupId: UUID }) =>
    updateGroupNote(arg),
  invalidateQueryKeys: ({ groupId }) => [groupNotesQuery({ groupId }).queryKey]
})

export const deleteGroupNoteMutation = mutation({
  api: (arg: Arg0<typeof deleteGroupNote> & { groupId: UUID }) =>
    deleteGroupNote(arg),
  invalidateQueryKeys: ({ groupId }) => [groupNotesQuery({ groupId }).queryKey]
})

export const createChildDailyNoteMutation = mutation({
  api: (arg: Arg0<typeof createChildDailyNote> & { unitId: UUID }) =>
    createChildDailyNote(arg),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateChildDailyNoteMutation = mutation({
  api: (arg: Arg0<typeof updateChildDailyNote> & { unitId: UUID }) =>
    updateChildDailyNote(arg).then(() => undefined),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildDailyNoteMutation = mutation({
  api: (arg: Arg0<typeof deleteChildDailyNote> & { unitId: UUID }) =>
    deleteChildDailyNote(arg).then(() => undefined),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const createChildStickyNoteMutation = mutation({
  api: (arg: Arg0<typeof createChildStickyNote> & { unitId: UUID }) =>
    createChildStickyNote(arg),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const updateChildStickyNoteMutation = mutation({
  api: (arg: Arg0<typeof updateChildStickyNote> & { unitId: UUID }) =>
    updateChildStickyNote(arg),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})

export const deleteChildStickyNoteMutation = mutation({
  api: (arg: Arg0<typeof deleteChildStickyNote> & { unitId: UUID }) =>
    deleteChildStickyNote(arg),
  invalidateQueryKeys: ({ unitId }) => [childrenQuery(unitId).queryKey]
})
