// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { FeatureFlags } from 'lib-customizations/types'

import type { Env } from './env'
import { env } from './env'

type Features = {
  default: FeatureFlags
} & Record<Env, FeatureFlags>

const features: Features = {
  default: {
    environmentLabel: 'Lokaali',
    citizenShiftCareAbsence: true,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: true,
      serviceNeedOption: false
    },
    preschoolApplication: {
      connectedDaycarePreferredStartDate: true,
      serviceNeedOption: false
    },
    decisionDraftMultipleUnits: false,
    preschool: true,
    preparatory: true,
    urgencyAttachments: true,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: false,
    placementGuarantee: true,
    voucherUnitPayments: true,
    voucherValueSeparation: true,
    assistanceNeedDecisionsLanguageSelect: true,
    extendedPreschoolTerm: true,
    citizenAttendanceSummary: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    discussionReservations: true,
    jamixIntegration: true,
    nekkuIntegration: false,
    forceUnpublishDocumentTemplate: true,
    invoiceDisplayAccountNumber: true,
    serviceApplications: true,
    multiSelectDeparture: true,
    archiveIntegration: {
      childDocuments: true
    },
    aromiIntegration: true,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    showCitizenApplicationPreschoolTerms: true,
    missingQuestionnaireAnswerMarkerEnabled: false,
    absenceApplications: true,
    showMetadataToCitizen: true,
    placementDesktop: true
  },
  staging: {
    environmentLabel: 'Staging',
    citizenShiftCareAbsence: true,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: true,
      serviceNeedOption: false
    },
    preschoolApplication: {
      connectedDaycarePreferredStartDate: true,
      serviceNeedOption: false
    },
    decisionDraftMultipleUnits: false,
    preschool: true,
    preparatory: true,
    urgencyAttachments: true,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: false,
    placementGuarantee: true,
    assistanceNeedDecisionsLanguageSelect: true,
    extendedPreschoolTerm: true,
    citizenAttendanceSummary: false,
    voucherUnitPayments: false,
    voucherValueSeparation: true,
    intermittentShiftCare: false,
    noAbsenceType: false,
    discussionReservations: true,
    jamixIntegration: false,
    nekkuIntegration: false,
    forceUnpublishDocumentTemplate: true,
    invoiceDisplayAccountNumber: true,
    serviceApplications: true,
    multiSelectDeparture: true,
    archiveIntegration: {
      childDocuments: true
    },
    aromiIntegration: true,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    showCitizenApplicationPreschoolTerms: true,
    missingQuestionnaireAnswerMarkerEnabled: false,
    absenceApplications: true,
    showMetadataToCitizen: true,
    placementDesktop: true
  },
  prod: {
    environmentLabel: null,
    citizenShiftCareAbsence: true,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: true,
      serviceNeedOption: false
    },
    preschoolApplication: {
      connectedDaycarePreferredStartDate: true,
      serviceNeedOption: false
    },
    decisionDraftMultipleUnits: false,
    preschool: true,
    preparatory: true,
    urgencyAttachments: true,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: false,
    placementGuarantee: true,
    assistanceNeedDecisionsLanguageSelect: true,
    extendedPreschoolTerm: true,
    citizenAttendanceSummary: false,
    voucherUnitPayments: false,
    voucherValueSeparation: true,
    intermittentShiftCare: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: false,
    invoiceDisplayAccountNumber: true,
    serviceApplications: false,
    multiSelectDeparture: true,
    archiveIntegration: {
      childDocuments: true
    },
    aromiIntegration: false,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    showCitizenApplicationPreschoolTerms: false,
    missingQuestionnaireAnswerMarkerEnabled: false,
    absenceApplications: true,
    showMetadataToCitizen: false,
    placementDesktop: true
  }
}

const featureFlags = features[env()]

export default featureFlags
