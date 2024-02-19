// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AssistanceNeedDecisionStatus,
  AssistanceNeedPreschoolDecisionForm
} from 'lib-common/generated/api-types/assistanceneed'
import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { getUnitsRaw } from '../../api/daycare'
import {
  createAssistanceAction,
  createAssistanceFactor,
  createDaycareAssistance,
  createOtherAssistanceMeasure,
  createPreschoolAssistance,
  deleteAssistanceAction,
  deleteAssistanceFactor,
  deleteDaycareAssistance,
  deleteOtherAssistanceMeasure,
  deletePreschoolAssistance,
  getChildAssistance,
  updateAssistanceAction,
  updateAssistanceFactor,
  updateDaycareAssistance,
  updateOtherAssistanceMeasure,
  updatePreschoolAssistance
} from '../../generated/api-clients/assistance'
import {
  createDocument,
  deleteDraftDocument,
  getDocument,
  getDocuments,
  nextDocumentStatus,
  prevDocumentStatus,
  publishDocument,
  updateDocumentContent
} from '../../generated/api-clients/document'
import { createQueryKeys } from '../../query'

import {
  deleteAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecisionBasics,
  getAssistanceNeedPreschoolDecisionMakerOptions,
  postAssistanceNeedPreschoolDecision,
  putAssistanceNeedPreschoolDecision,
  putAssistanceNeedPreschoolDecisionAnnul,
  putAssistanceNeedPreschoolDecisionDecide,
  putAssistanceNeedPreschoolDecisionSend,
  putAssistanceNeedPreschoolDecisionUnsend
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
  units: () => ['units'],
  decisionMakerOptions: (decisionId: UUID, unitId: UUID | null) => [
    'decisionMakerOptions',
    decisionId,
    unitId
  ]
})

export const childDocumentsQuery = query({
  api: getDocuments,
  queryKey: ({ childId }) => queryKeys.childDocuments(childId)
})

export const childDocumentQuery = query({
  api: getDocument,
  queryKey: ({ documentId }) => queryKeys.childDocument(documentId)
})

export const createChildDocumentMutation = mutation({
  api: createDocument,
  invalidateQueryKeys: (arg) => [queryKeys.childDocuments(arg.body.childId)]
})

export const updateChildDocumentContentMutation = mutation({
  api: updateDocumentContent,
  invalidateQueryKeys: () => [] // do not invalidate automatically because of auto-save
})

export const publishChildDocumentMutation = mutation({
  api: (arg: Arg0<typeof publishDocument> & { childId: UUID }) =>
    publishDocument(arg),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const childDocumentNextStatusMutation = mutation({
  api: (arg: Arg0<typeof nextDocumentStatus> & { childId: UUID }) =>
    nextDocumentStatus(arg),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const childDocumentPrevStatusMutation = mutation({
  api: (arg: Arg0<typeof prevDocumentStatus> & { childId: UUID }) =>
    prevDocumentStatus(arg),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const deleteChildDocumentMutation = mutation({
  api: (arg: Arg0<typeof deleteDraftDocument> & { childId: UUID }) =>
    deleteDraftDocument(arg),
  invalidateQueryKeys: ({ childId, documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const assistanceQuery = query({
  api: getChildAssistance,
  queryKey: ({ child }) => queryKeys.assistance(child)
})

export const createAssistanceActionMutation = mutation({
  api: (arg: Arg0<typeof createAssistanceAction> & { childId: UUID }) =>
    createAssistanceAction(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateAssistanceActionMutation = mutation({
  api: (arg: Arg0<typeof updateAssistanceAction> & { childId: UUID }) =>
    updateAssistanceAction(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceActionMutation = mutation({
  api: (arg: Arg0<typeof deleteAssistanceAction> & { childId: UUID }) =>
    deleteAssistanceAction(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createAssistanceFactorMutation = mutation({
  api: createAssistanceFactor,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updateAssistanceFactorMutation = mutation({
  api: (arg: Arg0<typeof updateAssistanceFactor> & { childId: UUID }) =>
    updateAssistanceFactor(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceFactorMutation = mutation({
  api: (arg: Arg0<typeof deleteAssistanceFactor> & { childId: UUID }) =>
    deleteAssistanceFactor(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createDaycareAssistanceMutation = mutation({
  api: createDaycareAssistance,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updateDaycareAssistanceMutation = mutation({
  api: (arg: Arg0<typeof updateDaycareAssistance> & { childId: UUID }) =>
    updateDaycareAssistance(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteDaycareAssistanceMutation = mutation({
  api: (arg: Arg0<typeof deleteDaycareAssistance> & { childId: UUID }) =>
    deleteDaycareAssistance(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createPreschoolAssistanceMutation = mutation({
  api: createPreschoolAssistance,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updatePreschoolAssistanceMutation = mutation({
  api: (arg: Arg0<typeof updatePreschoolAssistance> & { childId: UUID }) =>
    updatePreschoolAssistance(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deletePreschoolAssistanceMutation = mutation({
  api: (arg: Arg0<typeof deletePreschoolAssistance> & { childId: UUID }) =>
    deletePreschoolAssistance(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const createOtherAssistanceMeasureMutation = mutation({
  api: createOtherAssistanceMeasure,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updateOtherAssistanceMeasureMutation = mutation({
  api: (arg: Arg0<typeof updateOtherAssistanceMeasure> & { childId: UUID }) =>
    updateOtherAssistanceMeasure(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const deleteOtherAssistanceMeasureMutation = mutation({
  api: (arg: Arg0<typeof deleteOtherAssistanceMeasure> & { childId: UUID }) =>
    deleteOtherAssistanceMeasure(arg),
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

export const assistanceNeedPreschoolDecisionMakerOptionsQuery = query({
  api: (arg: { decisionId: UUID; unitId: UUID | null }) =>
    getAssistanceNeedPreschoolDecisionMakerOptions(arg.decisionId),
  queryKey: (arg) => queryKeys.decisionMakerOptions(arg.decisionId, arg.unitId)
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

export const sendAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: { childId: UUID; id: UUID }) =>
    putAssistanceNeedPreschoolDecisionSend(arg.id),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const unsendAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: { childId: UUID; id: UUID }) =>
    putAssistanceNeedPreschoolDecisionUnsend(arg.id),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const decideAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: {
    childId: UUID
    id: UUID
    status: AssistanceNeedDecisionStatus
  }) => putAssistanceNeedPreschoolDecisionDecide(arg.id, arg.status),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const annulAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: { childId: UUID; id: UUID; reason: string }) =>
    putAssistanceNeedPreschoolDecisionAnnul(arg.id, arg.reason),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const deleteAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (arg: { childId: UUID; id: UUID }) =>
    deleteAssistanceNeedPreschoolDecision(arg.id),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const unitsQuery = query({
  api: () => getUnitsRaw([], 'ALL'),
  queryKey: queryKeys.units
})
