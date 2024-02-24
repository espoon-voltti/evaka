// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  createNote,
  deleteNote,
  getNotes,
  updateNote
} from '../../generated/api-clients/application'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('applications', {
  applicationNotes: (applicationId: UUID) => ['applicationNotes', applicationId]
})

export const applicationNotesQuery = query({
  api: getNotes,
  queryKey: ({ applicationId }) => queryKeys.applicationNotes(applicationId)
})

export const createApplicationNote = mutation({
  api: createNote,
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})

export const updateApplicationNote = mutation({
  api: (arg: Arg0<typeof updateNote> & { applicationId: UUID }) =>
    updateNote(arg),
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})

export const deleteApplicationNote = mutation({
  api: (arg: Arg0<typeof deleteNote> & { applicationId: UUID }) =>
    deleteNote(arg),
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})
