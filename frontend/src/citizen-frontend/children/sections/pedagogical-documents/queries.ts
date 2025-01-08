// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  getPedagogicalDocumentsForChild,
  getUnreadPedagogicalDocumentCount,
  markPedagogicalDocumentRead
} from '../../../generated/api-clients/pedagogicaldocument'

const q = new Queries()

export const unreadPedagogicalDocumentsCountQuery = q.query(
  getUnreadPedagogicalDocumentCount
)

export const pedagogicalDocumentsQuery = q.query(
  getPedagogicalDocumentsForChild
)

export const markPedagogicalDocumentAsReadMutation = q.parametricMutation<{
  childId: ChildId
}>()(markPedagogicalDocumentRead, [
  ({ childId }) => pedagogicalDocumentsQuery({ childId }),
  () => unreadPedagogicalDocumentsCountQuery()
])
