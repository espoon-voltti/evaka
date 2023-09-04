// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import { getChildDocumentDetails, getChildDocumentSummaries } from './api'

const queryKeys = createQueryKeys('childDocuments', {
  summaries: () => ['summaries'],
  details: (id: UUID) => ['details', id]
})

export const childDocumentSummariesQuery = query({
  api: getChildDocumentSummaries,
  queryKey: queryKeys.summaries
})

export const childDocumentDetailsQuery = query({
  api: getChildDocumentDetails,
  queryKey: queryKeys.details
})
