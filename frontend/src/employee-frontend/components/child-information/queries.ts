// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { getChildApplicationSummaries } from '../../generated/api-clients/application'
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
  annulAssistanceNeedPreschoolDecision,
  createAssistanceNeedPreschoolDecision,
  createAssistanceNeedVoucherCoefficient,
  decideAssistanceNeedPreschoolDecision,
  deleteAssistanceNeedPreschoolDecision,
  deleteAssistanceNeedVoucherCoefficient,
  getAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecisions,
  getAssistanceNeedVoucherCoefficients,
  getAssistancePreschoolDecisionMakerOptions,
  revertAssistanceNeedPreschoolDecisionToUnsent,
  sendAssistanceNeedPreschoolDecisionForDecision,
  updateAssistanceNeedPreschoolDecision,
  updateAssistanceNeedVoucherCoefficient
} from '../../generated/api-clients/assistanceneed'
import {
  createBackupPickup,
  deleteBackupPickup,
  getBackupPickups,
  updateBackupPickup
} from '../../generated/api-clients/backuppickup'
import {
  deleteDailyServiceTimes,
  getDailyServiceTimes,
  postDailyServiceTimes,
  putDailyServiceTimes,
  putDailyServiceTimesEnd
} from '../../generated/api-clients/dailyservicetimes'
import {
  getAdditionalInfo,
  getUnits,
  updateAdditionalInfo
} from '../../generated/api-clients/daycare'
import {
  createDocument,
  deleteDraftDocument,
  getDocument,
  getDocuments,
  nextDocumentStatus,
  prevDocumentStatus,
  publishDocument,
  takeDocumentWriteLock,
  updateDocumentContent
} from '../../generated/api-clients/document'
import {
  createFeeAlteration,
  deleteFeeAlteration,
  getFeeAlterations,
  updateFeeAlteration
} from '../../generated/api-clients/invoicing'
import { getFosterParents } from '../../generated/api-clients/pis'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('childInformation', {
  childDocuments: (childId: UUID) => ['childDocuments', childId],
  childDocument: (id: UUID) => ['childDocument', id],
  childDocumentWriteLock: (id: UUID) => ['childDocument', id, 'lock'],
  assistance: (childId: UUID) => ['assistance', childId],
  assistanceNeedPreschoolDecisionBasics: (childId: UUID) => [
    'assistanceNeedPreschoolDecisionBasics',
    childId
  ],
  assistanceNeedPreschoolDecision: (decisionId: UUID) => [
    'assistanceNeedPreschoolDecision',
    decisionId
  ],
  assistanceNeedPreschoolDecisionDecisionMakerOptions: (
    decisionId: UUID,
    unitId: UUID | null
  ) => [
    'assistanceNeedPreschoolDecisionDecisionMakerOptions',
    decisionId,
    unitId
  ],
  units: () => ['units'],
  backupPickups: (childId: UUID) => ['backupPickups', childId],
  fosterParents: (childId: UUID) => ['fosterParents', childId],
  feeAlterations: (personId: UUID) => ['feeAlterations', personId],
  applicationSummaries: (childId: UUID) => ['applicationSummaries', childId],
  assistanceNeedVoucherCoefficients: (childId: UUID) => [
    'assistanceNeedVoucherCoefficients',
    childId
  ],
  backupCares: (childId: UUID) => ['backupCares', childId],
  dailyServiceTimes: (childId: UUID) => ['dailyServiceTimes', childId],
  additionalInfo: (childId: UUID) => ['additionalInfo', childId]
})

export const childDocumentsQuery = query({
  api: getDocuments,
  queryKey: ({ childId }) => queryKeys.childDocuments(childId)
})

export const childDocumentQuery = query({
  api: getDocument,
  queryKey: ({ documentId }) => queryKeys.childDocument(documentId)
})

export const childDocumentWriteLockQuery = query({
  api: takeDocumentWriteLock,
  queryKey: ({ documentId }) => queryKeys.childDocumentWriteLock(documentId)
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
  api: getAssistanceNeedPreschoolDecisions,
  queryKey: ({ childId }) =>
    queryKeys.assistanceNeedPreschoolDecisionBasics(childId)
})

export const assistanceNeedPreschoolDecisionQuery = query({
  api: getAssistanceNeedPreschoolDecision,
  queryKey: ({ id }) => queryKeys.assistanceNeedPreschoolDecision(id)
})

export const assistanceNeedPreschoolDecisionMakerOptionsQuery = query({
  api: (
    arg: Arg0<typeof getAssistancePreschoolDecisionMakerOptions> & {
      unitId: UUID | null
    }
  ) => getAssistancePreschoolDecisionMakerOptions(arg),
  queryKey: ({ id, unitId }) =>
    queryKeys.assistanceNeedPreschoolDecisionDecisionMakerOptions(id, unitId)
})

export const createAssistanceNeedPreschoolDecisionMutation = mutation({
  api: createAssistanceNeedPreschoolDecision,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(childId)
  ]
})

export const updateAssistanceNeedPreschoolDecisionMutation = mutation({
  api: updateAssistanceNeedPreschoolDecision,
  invalidateQueryKeys: () => [] // no automatic invalidation due to auto-save
})

export const sendAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (
    arg: Arg0<typeof sendAssistanceNeedPreschoolDecisionForDecision> & {
      childId: UUID
    }
  ) => sendAssistanceNeedPreschoolDecisionForDecision(arg),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const unsendAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (
    arg: Arg0<typeof revertAssistanceNeedPreschoolDecisionToUnsent> & {
      childId: UUID
    }
  ) => revertAssistanceNeedPreschoolDecisionToUnsent(arg),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const decideAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (
    arg: Arg0<typeof decideAssistanceNeedPreschoolDecision> & { childId: UUID }
  ) => decideAssistanceNeedPreschoolDecision(arg),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const annulAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (
    arg: Arg0<typeof annulAssistanceNeedPreschoolDecision> & { childId: UUID }
  ) => annulAssistanceNeedPreschoolDecision(arg),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const deleteAssistanceNeedPreschoolDecisionMutation = mutation({
  api: (
    arg: Arg0<typeof deleteAssistanceNeedPreschoolDecision> & { childId: UUID }
  ) => deleteAssistanceNeedPreschoolDecision(arg),
  invalidateQueryKeys: (arg) => [
    queryKeys.assistanceNeedPreschoolDecisionBasics(arg.childId),
    queryKeys.assistanceNeedPreschoolDecision(arg.id)
  ]
})

export const unitsQuery = query({
  api: getUnits,
  queryKey: queryKeys.units
})

export const getBackupPickupsQuery = query({
  api: getBackupPickups,
  queryKey: ({ childId }) => queryKeys.backupPickups(childId)
})

export const createBackupPickupMutation = mutation({
  api: createBackupPickup,
  invalidateQueryKeys: ({ childId }) => [queryKeys.backupPickups(childId)]
})

export const updateBackupPickupMutation = mutation({
  api: (arg: Arg0<typeof updateBackupPickup> & { childId: UUID }) =>
    updateBackupPickup(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.backupPickups(childId)]
})

export const deleteBackupPickupMutation = mutation({
  api: (arg: Arg0<typeof deleteBackupPickup> & { childId: UUID }) =>
    deleteBackupPickup(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.backupPickups(childId)]
})

export const getFosterParentsQuery = query({
  api: getFosterParents,
  queryKey: ({ childId }) => queryKeys.fosterParents(childId)
})

export const getFeeAlterationsQuery = query({
  api: getFeeAlterations,
  queryKey: ({ personId }) => queryKeys.feeAlterations(personId)
})

export const createFeeAlterationMutation = mutation({
  api: createFeeAlteration,
  invalidateQueryKeys: ({ body }) => [queryKeys.feeAlterations(body.personId)]
})

export const updateFeeAlterationMutation = mutation({
  api: (arg: Arg0<typeof updateFeeAlteration> & { personId: UUID }) =>
    updateFeeAlteration(arg),
  invalidateQueryKeys: ({ personId }) => [queryKeys.feeAlterations(personId)]
})

export const deleteFeeAlterationMutation = mutation({
  api: (arg: Arg0<typeof deleteFeeAlteration> & { personId: UUID }) =>
    deleteFeeAlteration(arg),
  invalidateQueryKeys: ({ personId }) => [queryKeys.feeAlterations(personId)]
})

export const getChildApplicationSummariesQuery = query({
  api: getChildApplicationSummaries,
  queryKey: ({ childId }) => queryKeys.applicationSummaries(childId)
})

export const getAssistanceNeedVoucherCoefficientsQuery = query({
  api: getAssistanceNeedVoucherCoefficients,
  queryKey: ({ childId }) =>
    queryKeys.assistanceNeedVoucherCoefficients(childId)
})

export const createAssistanceNeedVoucherCoefficientMutation = mutation({
  api: createAssistanceNeedVoucherCoefficient,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.assistanceNeedVoucherCoefficients(childId)
  ]
})

export const updateAssistanceNeedVoucherCoefficientMutation = mutation({
  api: (
    arg: Arg0<typeof updateAssistanceNeedVoucherCoefficient> & { childId: UUID }
  ) => updateAssistanceNeedVoucherCoefficient(arg),
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.assistanceNeedVoucherCoefficients(childId)
  ]
})

export const deleteAssistanceNeedVoucherCoefficientMutation = mutation({
  api: (
    arg: Arg0<typeof deleteAssistanceNeedVoucherCoefficient> & { childId: UUID }
  ) => deleteAssistanceNeedVoucherCoefficient(arg),
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.assistanceNeedVoucherCoefficients(childId)
  ]
})

export const getDailyServiceTimesQuery = query({
  api: getDailyServiceTimes,
  queryKey: ({ childId }) => queryKeys.dailyServiceTimes(childId)
})

export const postDailyServiceTimesMutation = mutation({
  api: postDailyServiceTimes,
  invalidateQueryKeys: ({ childId }) => [queryKeys.dailyServiceTimes(childId)]
})

export const putDailyServiceTimesMutation = mutation({
  api: (arg: Arg0<typeof putDailyServiceTimes> & { childId: UUID }) =>
    putDailyServiceTimes(arg),
  invalidateQueryKeys: (arg) => [queryKeys.dailyServiceTimes(arg.childId)]
})

export const putDailyServiceTimesEndMutation = mutation({
  api: (arg: Arg0<typeof putDailyServiceTimesEnd> & { childId: UUID }) =>
    putDailyServiceTimesEnd(arg),
  invalidateQueryKeys: (arg) => [queryKeys.dailyServiceTimes(arg.childId)]
})

export const deleteDailyServiceTimesMutation = mutation({
  api: (arg: Arg0<typeof deleteDailyServiceTimes> & { childId: UUID }) =>
    deleteDailyServiceTimes(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.dailyServiceTimes(childId)]
})

export const getAdditionalInfoQuery = query({
  api: getAdditionalInfo,
  queryKey: ({ childId }) => queryKeys.additionalInfo(childId)
})

export const updateAdditionalInfoMutation = mutation({
  api: updateAdditionalInfo,
  invalidateQueryKeys: ({ childId }) => [queryKeys.additionalInfo(childId)]
})
