// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../../query'

import {
  getChildVasuSummaries,
  getUnreadVasuDocumentsCount,
  getVasuDocument,
  givePermissionToShareVasu
} from './api'

const queryKeys = createQueryKeys('vasuAndLeops', {
  summariesByChild: (childId: UUID) => ['summaries', childId],
  document: (documentId: UUID) => ['documents', documentId],
  unreadCount: () => ['unreadCount']
})

export const vasuDocumentQuery = query({
  api: getVasuDocument,
  queryKey: queryKeys.document
})

export const unreadVasuDocumentsCountQuery = query({
  api: getUnreadVasuDocumentsCount,
  queryKey: queryKeys.unreadCount
})

export const givePermissionToShareVasuMutation = mutation({
  api: givePermissionToShareVasu,
  invalidateQueryKeys: (documentId) => [
    vasuDocumentQuery(documentId).queryKey,
    unreadVasuDocumentsCountQuery().queryKey
  ]
})

export const childVasuSummariesQuery = query({
  api: getChildVasuSummaries,
  queryKey: queryKeys.summariesByChild
})
