// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildId,
  DaycareId,
  PersonId
} from 'lib-common/generated/api-types/shared'

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
import { queries } from '../../query'

const q = queries('childInformation')

// export const queryKeys = createQueryKeys('childInformation', {
//   childDocuments: (childId: UUID) => ['childDocuments', childId],
//   childDocument: (id: UUID) => ['childDocument', id],
//   childDocumentMetadata: (id: UUID) => ['childDocumentMetadata', id],
//   childDocumentWriteLock: (id: UUID) => ['childDocument', id, 'lock'],
//   serviceApplications: (childId: UUID) => ['serviceApplications', childId],
//   assistance: (childId: UUID) => ['assistance', childId],
//   assistanceNeedDecisionMetadata: (decisionId: UUID) => [
//     'assistanceNeedDecisionMetadata',
//     decisionId
//   ],
//   assistanceNeedPreschoolDecisionBasics: (childId: UUID) => [
//     'assistanceNeedPreschoolDecisionBasics',
//     childId
//   ],
//   assistanceNeedPreschoolDecision: (decisionId: UUID) => [
//     'assistanceNeedPreschoolDecision',
//     decisionId
//   ],
//   assistanceNeedPreschoolDecisionDecisionMakerOptions: (
//     decisionId: UUID,
//     unitId: UUID | null
//   ) => [
//     'assistanceNeedPreschoolDecisionDecisionMakerOptions',
//     decisionId,
//     unitId
//   ],
//   assistanceNeedPreschoolDecisionMetadata: (decisionId: UUID) => [
//     'assistanceNeedPreschoolDecisionMetadata',
//     decisionId
//   ],
//   units: () => ['units'],
//   backupPickups: (childId: UUID) => ['backupPickups', childId],
//   fosterParents: (childId: UUID) => ['fosterParents', childId],
//   feeAlterations: (personId: UUID) => ['feeAlterations', personId],
//   applicationSummaries: (childId: UUID) => ['applicationSummaries', childId],
//   assistanceNeedVoucherCoefficients: (childId: UUID) => [
//     'assistanceNeedVoucherCoefficients',
//     childId
//   ],
//   backupCares: (childId: UUID) => ['backupCares', childId],
//   dailyServiceTimes: (childId: UUID) => ['dailyServiceTimes', childId],
//   additionalInfo: (childId: UUID) => ['additionalInfo', childId]
// })

export const childDocumentsQuery = q.query(getDocuments)

export const childDocumentQuery = q.query(getDocument, {
  refetchOnMount: 'always',
  refetchOnWindowFocus: false
})

export const childDocumentMetadataQuery = q.query(getChildDocumentMetadata)

export const childDocumentWriteLockQuery = q.query(takeDocumentWriteLock, {
  refetchOnMount: 'always',
  refetchOnWindowFocus: false
})

export const createChildDocumentMutation = q.mutation(createDocument, [
  ({ body: { childId } }) => childDocumentsQuery({ childId })
])

export const updateChildDocumentContentMutation = q.mutation(
  updateDocumentContent
  // do not invalidate automatically because of auto-save
)

export const publishChildDocumentMutation = q.parametricMutation<ChildId>()(
  publishDocument,
  [
    (childId) => childDocumentsQuery({ childId }),
    (_, { documentId }) => childDocumentQuery({ documentId })
  ]
)

export const childDocumentNextStatusMutation = q.parametricMutation<ChildId>()(
  nextDocumentStatus,
  [
    (childId) => childDocumentsQuery({ childId }),
    (_, { documentId }) => childDocumentQuery({ documentId })
  ]
)

export const childDocumentPrevStatusMutation = q.parametricMutation<ChildId>()(
  prevDocumentStatus,
  [
    (childId) => childDocumentsQuery({ childId }),
    (_, { documentId }) => childDocumentQuery({ documentId })
  ]
)

export const deleteChildDocumentMutation = q.parametricMutation<ChildId>()(
  deleteDraftDocument,
  [
    (childId) => childDocumentsQuery({ childId }),
    (_, { documentId }) => childDocumentQuery({ documentId })
  ]
)

export const childServiceApplicationsQuery = q.query(
  getChildServiceApplications
)

export const acceptServiceApplicationsMutation =
  q.parametricMutation<ChildId>()(acceptServiceApplication, [
    (childId) => childServiceApplicationsQuery({ childId })
  ])

export const rejectServiceApplicationsMutation =
  q.parametricMutation<ChildId>()(rejectServiceApplication, [
    (childId) => childServiceApplicationsQuery({ childId })
  ])

export const assistanceQuery = q.query(getChildAssistance)

export const createAssistanceActionMutation = q.mutation(
  createAssistanceAction,
  [({ childId }) => assistanceQuery({ child: childId })]
)

export const updateAssistanceActionMutation = q.parametricMutation<ChildId>()(
  updateAssistanceAction,
  [(childId) => assistanceQuery({ child: childId })]
)

export const deleteAssistanceActionMutation = q.parametricMutation<ChildId>()(
  deleteAssistanceAction,
  [(childId) => assistanceQuery({ child: childId })]
)

export const createAssistanceFactorMutation = q.mutation(
  createAssistanceFactor,
  [({ child }) => assistanceQuery({ child })]
)

export const updateAssistanceFactorMutation = q.parametricMutation<ChildId>()(
  updateAssistanceFactor,
  [(childId) => assistanceQuery({ child: childId })]
)

export const deleteAssistanceFactorMutation = q.parametricMutation<ChildId>()(
  deleteAssistanceFactor,
  [(childId) => assistanceQuery({ child: childId })]
)

export const createDaycareAssistanceMutation = q.mutation(
  createDaycareAssistance,
  [({ child }) => assistanceQuery({ child })]
)

export const updateDaycareAssistanceMutation = q.parametricMutation<ChildId>()(
  updateDaycareAssistance,
  [(childId) => assistanceQuery({ child: childId })]
)

export const deleteDaycareAssistanceMutation = q.parametricMutation<ChildId>()(
  deleteDaycareAssistance,
  [(childId) => assistanceQuery({ child: childId })]
)

export const createPreschoolAssistanceMutation = q.mutation(
  createPreschoolAssistance,
  [({ child }) => assistanceQuery({ child })]
)

export const updatePreschoolAssistanceMutation =
  q.parametricMutation<ChildId>()(updatePreschoolAssistance, [
    (childId) => assistanceQuery({ child: childId })
  ])

export const deletePreschoolAssistanceMutation =
  q.parametricMutation<ChildId>()(deletePreschoolAssistance, [
    (childId) => assistanceQuery({ child: childId })
  ])

export const createOtherAssistanceMeasureMutation = q.mutation(
  createOtherAssistanceMeasure,
  [({ child }) => assistanceQuery({ child })]
)

export const updateOtherAssistanceMeasureMutation =
  q.parametricMutation<ChildId>()(updateOtherAssistanceMeasure, [
    (childId) => assistanceQuery({ child: childId })
  ])

export const deleteOtherAssistanceMeasureMutation =
  q.parametricMutation<ChildId>()(deleteOtherAssistanceMeasure, [
    (childId) => assistanceQuery({ child: childId })
  ])

export const assistanceNeedDecisionMetadataQuery = q.query(
  getAssistanceNeedDecisionMetadata
)

export const assistanceNeedPreschoolDecisionBasicsQuery = q.query(
  getAssistanceNeedPreschoolDecisions
)

export const assistanceNeedPreschoolDecisionQuery = q.query(
  getAssistanceNeedPreschoolDecision
)

export const assistanceNeedPreschoolDecisionMakerOptionsQuery =
  q.parametricQuery<DaycareId | null>()(
    getAssistancePreschoolDecisionMakerOptions
  )

export const assistanceNeedPreschoolDecisionMetadataQuery = q.query(
  getAssistanceNeedPreschoolDecisionMetadata
)

export const createAssistanceNeedPreschoolDecisionMutation = q.mutation(
  createAssistanceNeedPreschoolDecision,
  [({ childId }) => assistanceNeedPreschoolDecisionBasicsQuery({ childId })]
)

export const updateAssistanceNeedPreschoolDecisionMutation = q.mutation(
  updateAssistanceNeedPreschoolDecision
  // no automatic invalidation due to auto-save
)

export const sendAssistanceNeedPreschoolDecisionMutation =
  q.parametricMutation<ChildId>()(
    sendAssistanceNeedPreschoolDecisionForDecision,
    [
      (childId) => assistanceNeedPreschoolDecisionBasicsQuery({ childId }),
      (_, { id }) => assistanceNeedPreschoolDecisionQuery({ id })
    ]
  )

export const unsendAssistanceNeedPreschoolDecisionMutation =
  q.parametricMutation<ChildId>()(
    revertAssistanceNeedPreschoolDecisionToUnsent,
    [
      (childId) => assistanceNeedPreschoolDecisionBasicsQuery({ childId }),
      (_, { id }) => assistanceNeedPreschoolDecisionQuery({ id })
    ]
  )

export const decideAssistanceNeedPreschoolDecisionMutation =
  q.parametricMutation<ChildId>()(decideAssistanceNeedPreschoolDecision, [
    (childId) => assistanceNeedPreschoolDecisionBasicsQuery({ childId }),
    (_, { id }) => assistanceNeedPreschoolDecisionQuery({ id })
  ])

export const annulAssistanceNeedPreschoolDecisionMutation =
  q.parametricMutation<ChildId>()(annulAssistanceNeedPreschoolDecision, [
    (childId) => assistanceNeedPreschoolDecisionBasicsQuery({ childId }),
    (_, { id }) => assistanceNeedPreschoolDecisionQuery({ id })
  ])

export const deleteAssistanceNeedPreschoolDecisionMutation =
  q.parametricMutation<ChildId>()(deleteAssistanceNeedPreschoolDecision, [
    (childId) => assistanceNeedPreschoolDecisionBasicsQuery({ childId }),
    (_, { id }) => assistanceNeedPreschoolDecisionQuery({ id })
  ])

export const unitsQuery = q.query(getUnits)

export const getBackupPickupsQuery = q.query(getBackupPickups)

export const createBackupPickupMutation = q.mutation(createBackupPickup, [
  ({ childId }) => getBackupPickupsQuery({ childId })
])

export const updateBackupPickupMutation = q.parametricMutation<ChildId>()(
  updateBackupPickup,
  [(childId) => getBackupPickupsQuery({ childId })]
)

export const deleteBackupPickupMutation = q.parametricMutation<ChildId>()(
  deleteBackupPickup,
  [(childId) => getBackupPickupsQuery({ childId })]
)

export const getFosterParentsQuery = q.query(getFosterParents)

export const getFeeAlterationsQuery = q.query(getFeeAlterations)

export const createFeeAlterationMutation = q.mutation(createFeeAlteration, [
  ({ body: { personId } }) => getFeeAlterationsQuery({ personId })
])

export const updateFeeAlterationMutation = q.mutation(updateFeeAlteration, [
  ({ body: { personId } }) => getFeeAlterationsQuery({ personId })
])

export const deleteFeeAlterationMutation = q.parametricMutation<PersonId>()(
  deleteFeeAlteration,
  [(personId) => getFeeAlterationsQuery({ personId })]
)

export const getChildApplicationSummariesQuery = q.query(
  getChildApplicationSummaries
)

export const getAssistanceNeedVoucherCoefficientsQuery = q.query(
  getAssistanceNeedVoucherCoefficients
)

export const createAssistanceNeedVoucherCoefficientMutation = q.mutation(
  createAssistanceNeedVoucherCoefficient,
  [({ childId }) => getAssistanceNeedVoucherCoefficientsQuery({ childId })]
)

export const updateAssistanceNeedVoucherCoefficientMutation =
  q.parametricMutation<ChildId>()(updateAssistanceNeedVoucherCoefficient, [
    (childId) => getAssistanceNeedVoucherCoefficientsQuery({ childId })
  ])

export const deleteAssistanceNeedVoucherCoefficientMutation =
  q.parametricMutation<ChildId>()(deleteAssistanceNeedVoucherCoefficient, [
    (childId) => getAssistanceNeedVoucherCoefficientsQuery({ childId })
  ])

export const getDailyServiceTimesQuery = q.query(getDailyServiceTimes)

export const postDailyServiceTimesMutation = q.mutation(postDailyServiceTimes, [
  ({ childId }) => getDailyServiceTimesQuery({ childId })
])

export const putDailyServiceTimesMutation = q.parametricMutation<ChildId>()(
  putDailyServiceTimes,
  [(childId) => getDailyServiceTimesQuery({ childId })]
)

export const putDailyServiceTimesEndMutation = q.parametricMutation<ChildId>()(
  putDailyServiceTimesEnd,
  [(childId) => getDailyServiceTimesQuery({ childId })]
)

export const deleteDailyServiceTimesMutation = q.parametricMutation<ChildId>()(
  deleteDailyServiceTimes,
  [(childId) => getDailyServiceTimesQuery({ childId })]
)

export const getAdditionalInfoQuery = q.query(getAdditionalInfo)

export const updateAdditionalInfoMutation = q.mutation(updateAdditionalInfo, [
  ({ childId }) => getAdditionalInfoQuery({ childId })
])
