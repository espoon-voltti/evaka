// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'

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
import { queries } from '../query'

const q = queries('notes')

export const groupNotesQuery = q.query(getGroupNotes, {
  staleTime: 5 * 60 * 1000
})

export const createGroupNoteMutation = q.mutation(createGroupNote, [
  ({ groupId }) => groupNotesQuery({ groupId })
])

export const updateGroupNoteMutation = q.parametricMutation<GroupId>()(
  updateGroupNote,
  [(groupId) => groupNotesQuery({ groupId })]
)

export const deleteGroupNoteMutation = q.parametricMutation<GroupId>()(
  deleteGroupNote,
  [(groupId) => groupNotesQuery({ groupId })]
)

export const createChildDailyNoteMutation = q.parametricMutation<DaycareId>()(
  createChildDailyNote,
  [(unitId) => childrenQuery(unitId)]
)

export const updateChildDailyNoteMutation = q.parametricMutation<DaycareId>()(
  updateChildDailyNote,
  [(unitId) => childrenQuery(unitId)]
)

export const deleteChildDailyNoteMutation = q.parametricMutation<DaycareId>()(
  deleteChildDailyNote,
  [(unitId) => childrenQuery(unitId)]
)

export const createChildStickyNoteMutation = q.parametricMutation<DaycareId>()(
  createChildStickyNote,
  [(unitId) => childrenQuery(unitId)]
)

export const updateChildStickyNoteMutation = q.parametricMutation<DaycareId>()(
  updateChildStickyNote,
  [(unitId) => childrenQuery(unitId)]
)

export const deleteChildStickyNoteMutation = q.parametricMutation<DaycareId>()(
  deleteChildStickyNote,
  [(unitId) => childrenQuery(unitId)]
)
