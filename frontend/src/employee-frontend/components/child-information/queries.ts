// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDocumentCreateRequest,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  deleteChildDocument,
  getChildDocument,
  getChildDocuments,
  postChildDocument,
  putChildDocumentContent,
  putChildDocumentPublish
} from '../../api/child/child-documents'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('childInformation', {
  childDocuments: (childId: UUID) => ['childDocuments', childId],
  childDocument: (id: UUID) => ['childDocument', id]
})

export const childDocumentsQuery = query({
  api: getChildDocuments,
  queryKey: queryKeys.childDocuments
})

export const childDocumentQuery = query({
  api: getChildDocument,
  queryKey: queryKeys.childDocument
})

export const createChildDocumentMutation = mutation({
  api: (arg: ChildDocumentCreateRequest) => postChildDocument(arg),
  invalidateQueryKeys: (arg) => [queryKeys.childDocuments(arg.childId)]
})

export const updateChildDocumentContentMutation = mutation({
  api: (arg: { id: UUID; content: DocumentContent }) =>
    putChildDocumentContent(arg.id, arg.content),
  invalidateQueryKeys: () => []
})

export const publishChildDocumentMutation = mutation({
  api: (arg: { documentId: UUID; childId: UUID }) =>
    putChildDocumentPublish(arg.documentId),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const deleteChildDocumentMutation = mutation({
  api: (arg: { documentId: UUID; childId: UUID }) =>
    deleteChildDocument(arg.documentId),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})
