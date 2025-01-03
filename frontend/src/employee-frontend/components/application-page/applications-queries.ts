// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationId } from 'lib-common/generated/api-types/shared'

import {
  createNote,
  deleteNote,
  getNotes,
  updateNote
} from '../../generated/api-clients/application'
import { getApplicationMetadata } from '../../generated/api-clients/process'
import { queries } from '../../query'

const q = queries('applications')

export const applicationNotesQuery = q.query(getNotes)

export const applicationMetadataQuery = q.query(getApplicationMetadata)

export const createApplicationNote = q.mutation(createNote, [
  ({ applicationId }) => applicationNotesQuery({ applicationId })
])

export const updateApplicationNote = q.parametricMutation<ApplicationId>()(
  updateNote,
  [(applicationId) => applicationNotesQuery({ applicationId })]
)

export const deleteApplicationNote = q.parametricMutation<ApplicationId>()(
  deleteNote,
  [(applicationId) => applicationNotesQuery({ applicationId })]
)
