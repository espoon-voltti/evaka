// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AssistanceFactorUpdate,
  DaycareAssistanceUpdate,
  OtherAssistanceMeasureUpdate,
  PreschoolAssistanceUpdate
} from 'lib-common/generated/api-types/assistance'
import { AssistanceActionRequest } from 'lib-common/generated/api-types/assistanceaction'
import {
  AssistanceNeedPreschoolDecisionForm,
  AssistanceNeedRequest
} from 'lib-common/generated/api-types/assistanceneed'
import {
  ChildDocumentCreateRequest,
  DocumentContent
} from 'lib-common/generated/api-types/document'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  createAssistanceFactor,
  createDaycareAssistance,
  createPreschoolAssistance,
  deleteAssistanceFactor,
  deleteDaycareAssistance,
  getAssistanceData,
  updateAssistanceFactor,
  updatePreschoolAssistance,
  updateDaycareAssistance,
  deletePreschoolAssistance,
  deleteOtherAssistanceMeasure,
  updateOtherAssistanceMeasure,
  createOtherAssistanceMeasure
} from '../../api/child/assistance'
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
import { getUnitsRaw } from '../../api/daycare'
import { createQueryKeys } from '../../query'

import {
  deleteAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecisionBasics,
  postAssistanceNeedPreschoolDecision,
  putAssistanceNeedPreschoolDecision
} from './assistance-need/decision/api-preschool'

export const queryKeys = createQueryKeys('childInformation', {
  childDocuments: (childId: UUID) => ['childDocuments', childId],
  childDocument: (id: UUID) => ['childDocument', id],
  assistance: (childId: UUID) => ['assistance', childId],
  assistanceNeedPreschoolDecisionBasics: (childId: UUID) => [
    'assistanceNeedPreschoolDecisionBasics',
    childId
  ],
  assistanceNeedPreschoolDecision: (decisionId: UUID) => [
    'assistanceNeedPreschoolDecision',
    decisionId
  ],
  preschoolUnits: () => ['preschoolUnits']
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

export const createAssistanceFactorMutation = mutation({
  api: (arg: { childId: UUID; data: AssistanceFactorUpdate }) =>
    createAssistanceFactor(arg.childId, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateAssistanceFactorMutation = mutation({
  api: (arg: { id: UUID; childId: UUID; data: AssistanceFactorUpdate }) =>
    updateAssistanceFactor(arg.id, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceFactorMutation = mutation({
  api: (arg: { id: UUID; childId: UUID }) => deleteAssistanceFactor(arg.id),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createDaycareAssistanceMutation = mutation({
  api: (arg: { childId: UUID; data: DaycareAssistanceUpdate }) =>
    createDaycareAssistance(arg.childId, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateDaycareAssistanceMutation = mutation({
  api: (arg: { id: UUID; childId: UUID; data: DaycareAssistanceUpdate }) =>
    updateDaycareAssistance(arg.id, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteDaycareAssistanceMutation = mutation({
  api: (arg: { id: UUID; childId: UUID }) => deleteDaycareAssistance(arg.id),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createPreschoolAssistanceMutation = mutation({
  api: (arg: { childId: UUID; data: PreschoolAssistanceUpdate }) =>
    createPreschoolAssistance(arg.childId, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updatePreschoolAssistanceMutation = mutation({
  api: (arg: { id: UUID; childId: UUID; data: PreschoolAssistanceUpdate }) =>
    updatePreschoolAssistance(arg.id, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deletePreschoolAssistanceMutation = mutation({
  api: (arg: { id: UUID; childId: UUID }) => deletePreschoolAssistance(arg.id),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createOtherAssistanceMeasureMutation = mutation({
  api: (arg: { childId: UUID; data: OtherAssistanceMeasureUpdate }) =>
    createOtherAssistanceMeasure(arg.childId, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateOtherAssistanceMeasureMutation = mutation({
  api: (arg: { id: UUID; childId: UUID; data: OtherAssistanceMeasureUpdate }) =>
    updateOtherAssistanceMeasure(arg.id, arg.data),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteOtherAssistanceMeasureMutation = mutation({
  api: (arg: { id: UUID; childId: UUID }) =>
    deleteOtherAssistanceMeasure(arg.id),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const assistanceNeedPreschoolDecisionBasicsQuery = query({
  api: getAssistanceNeedPreschoolDecisionBasics,
  queryKey: queryKeys.assistanceNeedPreschoolDecisionBasics
})

export const assistanceNeedPreschoolDecisionQuery = query({
  api: getAssistanceNeedPreschoolDecision,
  queryKey: queryKeys.assistanceNeedPreschoolDecision
})

export const createAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: UUID) => postAssistanceNeedPreschoolDecision(arg),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg)
  ]
})

export const updateAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: { id: UUID; body: AssistanceNeedPreschoolDecisionForm }) =>
    putAssistanceNeedPreschoolDecision(arg.id, arg.body),
  invalidateQueryKeys: () => [] // no automatic invalidation due to auto-save
})

export const deleteAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: { childId: UUID; id: UUID }) =>
    deleteAssistanceNeedPreschoolDecision(arg.id),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const preschoolUnitsQuery = query({
  api: () => getUnitsRaw([], 'PRESCHOOL'),
  queryKey: queryKeys.preschoolUnits
})
