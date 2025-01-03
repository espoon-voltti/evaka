// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationId } from 'lib-common/generated/api-types/shared'
import { mutation, parametricMutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  createNote,
  deleteNote,
  getNotes,
  updateNote
} from '../../generated/api-clients/application'
import { getApplicationMetadata } from '../../generated/api-clients/process'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('applications', {
  applicationNotes: (applicationId: UUID) => [
    'applicationNotes',
    applicationId
  ],
  applicationMetadata: (applicationId: UUID) => [
    'applicationMetadata',
    applicationId
  ]
})

export const applicationNotesQuery = query({
  api: getNotes,
  queryKey: ({ applicationId }) => queryKeys.applicationNotes(applicationId)
})

export const applicationMetadataQuery = query({
  api: getApplicationMetadata,
  queryKey: ({ applicationId }) => queryKeys.applicationMetadata(applicationId)
})

export const createApplicationNote = mutation({
  api: createNote,
  invalidateQueryKeys: ({ applicationId }) => [
    queryKeys.applicationNotes(applicationId)
  ]
})

export const updateApplicationNote = parametricMutation<ApplicationId>()({
  api: updateNote,
  invalidateQueryKeys: (applicationId) => [
    queryKeys.applicationNotes(applicationId)
  ]
})

export const deleteApplicationNote = parametricMutation<ApplicationId>()({
  api: deleteNote,
  invalidateQueryKeys: (applicationId) => [
    queryKeys.applicationNotes(applicationId)
  ]
})
