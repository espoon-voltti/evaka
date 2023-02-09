// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  createNote,
  deleteNote,
  getApplicationNotes,
  updateNote
} from './applications'

const queryKeys = createQueryKeys('applications', {
  applicationNotes: (applicationId: UUID) => ['applicationNotes', applicationId]
})

export const applicationNotesQuery = query({
  api: getApplicationNotes,
  queryKey: queryKeys.applicationNotes
})

export const createApplicationNote = mutation({
  api: (arg: { applicationId: UUID; text: string }) =>
    createNote(arg.applicationId, arg.text),
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})

export const updateApplicationNote = mutation({
  api: (arg: { applicationId: UUID; id: UUID; text: string }) =>
    updateNote(arg.id, arg.text),
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})

export const deleteApplicationNote = mutation({
  api: (arg: { applicationId: UUID; id: UUID }) => deleteNote(arg.id),
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})
