// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { mutation, parametricMutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

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

export const updateGroupNoteMutation = parametricMutation<GroupId>()({
  api: updateGroupNote,
  invalidateQueryKeys: (groupId) => [groupNotesQuery({ groupId }).queryKey]
})

export const deleteGroupNoteMutation = parametricMutation<GroupId>()({
  api: deleteGroupNote,
  invalidateQueryKeys: (groupId) => [groupNotesQuery({ groupId }).queryKey]
})

export const createChildDailyNoteMutation = parametricMutation<DaycareId>()({
  api: createChildDailyNote,
  invalidateQueryKeys: (unitId) => [childrenQuery(unitId).queryKey]
})

export const updateChildDailyNoteMutation = parametricMutation<DaycareId>()({
  api: updateChildDailyNote,
  invalidateQueryKeys: (unitId) => [childrenQuery(unitId).queryKey]
})

export const deleteChildDailyNoteMutation = parametricMutation<DaycareId>()({
  api: deleteChildDailyNote,
  invalidateQueryKeys: (unitId) => [childrenQuery(unitId).queryKey]
})

export const createChildStickyNoteMutation = parametricMutation<DaycareId>()({
  api: createChildStickyNote,
  invalidateQueryKeys: (unitId) => [childrenQuery(unitId).queryKey]
})

export const updateChildStickyNoteMutation = parametricMutation<DaycareId>()({
  api: updateChildStickyNote,
  invalidateQueryKeys: (unitId) => [childrenQuery(unitId).queryKey]
})

export const deleteChildStickyNoteMutation = parametricMutation<DaycareId>()({
  api: deleteChildStickyNote,
  invalidateQueryKeys: (unitId) => [childrenQuery(unitId).queryKey]
})
