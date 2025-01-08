// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

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

const q = new Queries()

export const groupNotesQuery = q.query(getGroupNotes, {
  staleTime: 5 * 60 * 1000
})

export const createGroupNoteMutation = q.mutation(createGroupNote, [
  ({ groupId }) => groupNotesQuery({ groupId })
])

export const updateGroupNoteMutation = q.parametricMutation<{
  groupId: GroupId
}>()(updateGroupNote, [({ groupId }) => groupNotesQuery({ groupId })])

export const deleteGroupNoteMutation = q.parametricMutation<{
  groupId: GroupId
}>()(deleteGroupNote, [({ groupId }) => groupNotesQuery({ groupId })])

export const createChildDailyNoteMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(createChildDailyNote, [({ unitId }) => childrenQuery(unitId)])

export const updateChildDailyNoteMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(updateChildDailyNote, [({ unitId }) => childrenQuery(unitId)])

export const deleteChildDailyNoteMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(deleteChildDailyNote, [({ unitId }) => childrenQuery(unitId)])

export const createChildStickyNoteMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(createChildStickyNote, [({ unitId }) => childrenQuery(unitId)])

export const updateChildStickyNoteMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(updateChildStickyNote, [({ unitId }) => childrenQuery(unitId)])

export const deleteChildStickyNoteMutation = q.parametricMutation<{
  unitId: DaycareId
}>()(deleteChildStickyNote, [({ unitId }) => childrenQuery(unitId)])
