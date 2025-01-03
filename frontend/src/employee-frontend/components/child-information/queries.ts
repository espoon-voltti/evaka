// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildId,
  DaycareId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import {
  mutation,
  parametricMutation,
  parametricQuery,
  query
} from 'lib-common/query'
import { UUID } from 'lib-common/types'

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
import {
  getAssistanceNeedDecisionMetadata,
  getAssistanceNeedPreschoolDecisionMetadata,
  getChildDocumentMetadata
} from '../../generated/api-clients/process'
import {
  acceptServiceApplication,
  getChildServiceApplications,
  rejectServiceApplication
} from '../../generated/api-clients/serviceneed'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('childInformation', {
  childDocuments: (childId: UUID) => ['childDocuments', childId],
  childDocument: (id: UUID) => ['childDocument', id],
  childDocumentMetadata: (id: UUID) => ['childDocumentMetadata', id],
  childDocumentWriteLock: (id: UUID) => ['childDocument', id, 'lock'],
  serviceApplications: (childId: UUID) => ['serviceApplications', childId],
  assistance: (childId: UUID) => ['assistance', childId],
  assistanceNeedDecisionMetadata: (decisionId: UUID) => [
    'assistanceNeedDecisionMetadata',
    decisionId
  ],
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
  assistanceNeedPreschoolDecisionMetadata: (decisionId: UUID) => [
    'assistanceNeedPreschoolDecisionMetadata',
    decisionId
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
  queryKey: ({ documentId }) => queryKeys.childDocument(documentId),
  options: {
    refetchOnMount: 'always',
    refetchOnWindowFocus: false
  }
})

export const childDocumentMetadataQuery = query({
  api: getChildDocumentMetadata,
  queryKey: ({ childDocumentId }) =>
    queryKeys.childDocumentMetadata(childDocumentId)
})

export const childDocumentWriteLockQuery = query({
  api: takeDocumentWriteLock,
  queryKey: ({ documentId }) => queryKeys.childDocumentWriteLock(documentId),
  options: {
    refetchOnMount: 'always',
    refetchOnWindowFocus: false
  }
})

export const createChildDocumentMutation = mutation({
  api: createDocument,
  invalidateQueryKeys: (arg) => [queryKeys.childDocuments(arg.body.childId)]
})

export const updateChildDocumentContentMutation = mutation({
  api: updateDocumentContent,
  invalidateQueryKeys: () => [] // do not invalidate automatically because of auto-save
})

export const publishChildDocumentMutation = parametricMutation<ChildId>()({
  api: publishDocument,
  invalidateQueryKeys: (childId, { documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const childDocumentNextStatusMutation = parametricMutation<ChildId>()({
  api: nextDocumentStatus,
  invalidateQueryKeys: (childId, { documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const childDocumentPrevStatusMutation = parametricMutation<ChildId>()({
  api: prevDocumentStatus,
  invalidateQueryKeys: (childId, { documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const deleteChildDocumentMutation = parametricMutation<ChildId>()({
  api: deleteDraftDocument,
  invalidateQueryKeys: (childId, { documentId }) => [
    queryKeys.childDocuments(childId),
    queryKeys.childDocument(documentId)
  ]
})

export const childServiceApplicationsQuery = query({
  api: getChildServiceApplications,
  queryKey: ({ childId }) => queryKeys.serviceApplications(childId)
})

export const acceptServiceApplicationsMutation = parametricMutation<ChildId>()({
  api: acceptServiceApplication,
  invalidateQueryKeys: (childId) => [queryKeys.serviceApplications(childId)]
})

export const rejectServiceApplicationsMutation = parametricMutation<ChildId>()({
  api: rejectServiceApplication,
  invalidateQueryKeys: (childId) => [queryKeys.serviceApplications(childId)]
})

export const assistanceQuery = query({
  api: getChildAssistance,
  queryKey: ({ child }) => queryKeys.assistance(child)
})

export const createAssistanceActionMutation = mutation({
  api: createAssistanceAction,
  invalidateQueryKeys: ({ childId }) => [queryKeys.assistance(childId)]
})

export const updateAssistanceActionMutation = parametricMutation<ChildId>()({
  api: updateAssistanceAction,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceActionMutation = parametricMutation<ChildId>()({
  api: deleteAssistanceAction,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const createAssistanceFactorMutation = mutation({
  api: createAssistanceFactor,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updateAssistanceFactorMutation = parametricMutation<ChildId>()({
  api: updateAssistanceFactor,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const deleteAssistanceFactorMutation = parametricMutation<ChildId>()({
  api: deleteAssistanceFactor,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const createDaycareAssistanceMutation = mutation({
  api: createDaycareAssistance,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updateDaycareAssistanceMutation = parametricMutation<ChildId>()({
  api: updateDaycareAssistance,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const deleteDaycareAssistanceMutation = parametricMutation<ChildId>()({
  api: deleteDaycareAssistance,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const createPreschoolAssistanceMutation = mutation({
  api: createPreschoolAssistance,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updatePreschoolAssistanceMutation = parametricMutation<ChildId>()({
  api: updatePreschoolAssistance,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const deletePreschoolAssistanceMutation = parametricMutation<ChildId>()({
  api: deletePreschoolAssistance,
  invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
})

export const createOtherAssistanceMeasureMutation = mutation({
  api: createOtherAssistanceMeasure,
  invalidateQueryKeys: ({ child }) => [queryKeys.assistance(child)]
})

export const updateOtherAssistanceMeasureMutation =
  parametricMutation<ChildId>()({
    api: updateOtherAssistanceMeasure,
    invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
  })

export const deleteOtherAssistanceMeasureMutation =
  parametricMutation<ChildId>()({
    api: deleteOtherAssistanceMeasure,
    invalidateQueryKeys: (childId) => [queryKeys.assistance(childId)]
  })

export const assistanceNeedDecisionMetadataQuery = query({
  api: getAssistanceNeedDecisionMetadata,
  queryKey: ({ decisionId }) =>
    queryKeys.assistanceNeedDecisionMetadata(decisionId)
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

export const assistanceNeedPreschoolDecisionMakerOptionsQuery =
  parametricQuery<DaycareId | null>()({
    api: getAssistancePreschoolDecisionMakerOptions,
    queryKey: (unitId, { id }) =>
      queryKeys.assistanceNeedPreschoolDecisionDecisionMakerOptions(id, unitId)
  })

export const assistanceNeedPreschoolDecisionMetadataQuery = query({
  api: getAssistanceNeedPreschoolDecisionMetadata,
  queryKey: ({ decisionId }) =>
    queryKeys.assistanceNeedPreschoolDecisionMetadata(decisionId)
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

export const sendAssistanceNeedPreschoolDecisionMutation =
  parametricMutation<ChildId>()({
    api: sendAssistanceNeedPreschoolDecisionForDecision,
    invalidateQueryKeys: (childId, { id }) => [
      queryKeys.assistanceNeedPreschoolDecisionBasics(childId),
      queryKeys.assistanceNeedPreschoolDecision(id)
    ]
  })

export const unsendAssistanceNeedPreschoolDecisionMutation =
  parametricMutation<ChildId>()({
    api: revertAssistanceNeedPreschoolDecisionToUnsent,
    invalidateQueryKeys: (childId, { id }) => [
      queryKeys.assistanceNeedPreschoolDecisionBasics(childId),
      queryKeys.assistanceNeedPreschoolDecision(id)
    ]
  })

export const decideAssistanceNeedPreschoolDecisionMutation =
  parametricMutation<ChildId>()({
    api: decideAssistanceNeedPreschoolDecision,
    invalidateQueryKeys: (childId, { id }) => [
      queryKeys.assistanceNeedPreschoolDecisionBasics(childId),
      queryKeys.assistanceNeedPreschoolDecision(id)
    ]
  })

export const annulAssistanceNeedPreschoolDecisionMutation =
  parametricMutation<ChildId>()({
    api: annulAssistanceNeedPreschoolDecision,
    invalidateQueryKeys: (childId, { id }) => [
      queryKeys.assistanceNeedPreschoolDecisionBasics(childId),
      queryKeys.assistanceNeedPreschoolDecision(id)
    ]
  })

export const deleteAssistanceNeedPreschoolDecisionMutation =
  parametricMutation<ChildId>()({
    api: deleteAssistanceNeedPreschoolDecision,
    invalidateQueryKeys: (childId, { id }) => [
      queryKeys.assistanceNeedPreschoolDecisionBasics(childId),
      queryKeys.assistanceNeedPreschoolDecision(id)
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

export const updateBackupPickupMutation = parametricMutation<ChildId>()({
  api: updateBackupPickup,
  invalidateQueryKeys: (childId) => [queryKeys.backupPickups(childId)]
})

export const deleteBackupPickupMutation = parametricMutation<ChildId>()({
  api: deleteBackupPickup,
  invalidateQueryKeys: (childId) => [queryKeys.backupPickups(childId)]
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
  api: updateFeeAlteration,
  invalidateQueryKeys: ({ body: { personId } }) => [
    queryKeys.feeAlterations(personId)
  ]
})

export const deleteFeeAlterationMutation = parametricMutation<PersonId>()({
  api: deleteFeeAlteration,
  invalidateQueryKeys: (personId) => [queryKeys.feeAlterations(personId)]
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

export const updateAssistanceNeedVoucherCoefficientMutation =
  parametricMutation<ChildId>()({
    api: updateAssistanceNeedVoucherCoefficient,
    invalidateQueryKeys: (childId) => [
      queryKeys.assistanceNeedVoucherCoefficients(childId)
    ]
  })

export const deleteAssistanceNeedVoucherCoefficientMutation =
  parametricMutation<ChildId>()({
    api: deleteAssistanceNeedVoucherCoefficient,
    invalidateQueryKeys: (childId) => [
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

export const putDailyServiceTimesMutation = parametricMutation<ChildId>()({
  api: putDailyServiceTimes,
  invalidateQueryKeys: (childId) => [queryKeys.dailyServiceTimes(childId)]
})

export const putDailyServiceTimesEndMutation = parametricMutation<ChildId>()({
  api: putDailyServiceTimesEnd,
  invalidateQueryKeys: (childId) => [queryKeys.dailyServiceTimes(childId)]
})

export const deleteDailyServiceTimesMutation = parametricMutation<ChildId>()({
  api: deleteDailyServiceTimes,
  invalidateQueryKeys: (childId) => [queryKeys.dailyServiceTimes(childId)]
})

export const getAdditionalInfoQuery = query({
  api: getAdditionalInfo,
  queryKey: ({ childId }) => queryKeys.additionalInfo(childId)
})

export const updateAdditionalInfoMutation = mutation({
  api: updateAdditionalInfo,
  invalidateQueryKeys: ({ childId }) => [queryKeys.additionalInfo(childId)]
})
