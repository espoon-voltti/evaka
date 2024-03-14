// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  getChildVasuSummaries,
  getDocument,
  getGuardianUnreadVasuCount,
  givePermissionToShare
} from '../../../generated/api-clients/vasu'
import { createQueryKeys } from '../../../query'

const queryKeys = createQueryKeys('vasuAndLeops', {
  summariesByChild: (childId: UUID) => ['summaries', childId],
  document: (documentId: UUID) => ['documents', documentId],
  unreadCount: () => ['unreadCount']
})

export const vasuDocumentQuery = query({
  api: getDocument,
  queryKey: ({ id }) => queryKeys.document(id)
})

export const unreadVasuDocumentsCountQuery = query({
  api: getGuardianUnreadVasuCount,
  queryKey: queryKeys.unreadCount
})

export const givePermissionToShareVasuMutation = mutation({
  api: givePermissionToShare,
  invalidateQueryKeys: ({ id }) => [
    vasuDocumentQuery({ id }).queryKey,
    unreadVasuDocumentsCountQuery().queryKey
  ]
})

export const childVasuSummariesQuery = query({
  api: getChildVasuSummaries,
  queryKey: ({ childId }) => queryKeys.summariesByChild(childId)
})
