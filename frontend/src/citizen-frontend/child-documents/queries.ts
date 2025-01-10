// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getDocument,
  getDocuments,
  getUnreadDocumentsCount,
  putDocumentRead
} from '../generated/api-clients/document'

const q = new Queries()

export const childDocumentSummariesQuery = q.query(getDocuments)
export const childDocumentDetailsQuery = q.query(getDocument)
export const unreadChildDocumentsCountQuery = q.query(getUnreadDocumentsCount)

export const childDocumentReadMutation = q.mutation(putDocumentRead, [
  unreadChildDocumentsCountQuery
])
