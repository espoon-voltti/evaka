// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AssistanceActionRequest } from 'lib-common/generated/api-types/assistanceaction'
import { AssistanceNeedRequest } from 'lib-common/generated/api-types/assistanceneed'
import {
  ChildDocumentCreateRequest,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { getAssistanceData } from '../../api/child/assistance'
import {
  createAssistanceAction,
  removeAssistanceAction,
  updateAssistanceAction
} from '../../api/child/assistance-actions'
import {
  createAssistanceNeed,
  removeAssistanceNeed,
  updateAssistanceNeed
} from '../../api/child/assistance-needs'
import {
  deleteChildDocument,
  getChildDocument,
  getChildDocuments,
  postChildDocument,
  putChildDocumentContent,
  putChildDocumentPublish,
  putChildDocumentUnpublish
} from '../../api/child/child-documents'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('childInformation', {
  childDocuments: (childId: UUID) => ['childDocuments', childId],
  childDocument: (id: UUID) => ['childDocument', id],
  assistance: (childId: UUID) => ['assistance', childId]
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
  invalidateQueryKeys: () => [] // do not invalidate automatically because of auto-save
})

export const publishChildDocumentMutation = mutation({
  api: (arg: { documentId: UUID; childId: UUID }) =>
    putChildDocumentPublish(arg.documentId),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const unpublishChildDocumentMutation = mutation({
  api: (arg: { documentId: UUID; childId: UUID }) =>
    putChildDocumentUnpublish(arg.documentId),
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

export const assistanceQuery = query({
  api: getAssistanceData,
  queryKey: queryKeys.assistance
})

export const createAssistanceNeedMutation = mutation({
  api: (arg: { childId: UUID; data: AssistanceNeedRequest }) =>
    createAssistanceNeed(arg.childId, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateAssistanceNeedMutation = mutation({
  api: (arg: { id: UUID; childId: UUID; data: AssistanceNeedRequest }) =>
    updateAssistanceNeed(arg.id, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceNeedMutation = mutation({
  api: (arg: { id: UUID; childId: UUID }) => removeAssistanceNeed(arg.id),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createAssistanceActionMutation = mutation({
  api: (arg: { childId: UUID; data: AssistanceActionRequest }) =>
    createAssistanceAction(arg.childId, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateAssistanceActionMutation = mutation({
  api: (arg: { id: UUID; childId: UUID; data: AssistanceActionRequest }) =>
    updateAssistanceAction(arg.id, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceActionMutation = mutation({
  api: (arg: { id: UUID; childId: UUID }) => removeAssistanceAction(arg.id),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})
