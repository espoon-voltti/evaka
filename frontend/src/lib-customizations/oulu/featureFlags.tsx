{
  /*
SPDX-FileCopyrightText: 2021 City of Oulu

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { FeatureFlags } from 'lib-customizations/types'

import { env } from './env'
import type { Env } from './env'

type Features = {
  default: FeatureFlags
} & Record<Env, FeatureFlags>

const features: Features = {
  default: {
    environmentLabel: 'Kehitys',
    citizenShiftCareAbsence: false,
    assistanceActionOther: false,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: true,
    daycareApplication: {
      dailyTimes: true,
      serviceNeedOption: true
    },
    preschoolApplication: {
      connectedDaycarePreferredStartDate: true,
      serviceNeedOption: true
    },
    decisionDraftMultipleUnits: false,
    preschool: true,
    urgencyAttachments: true,
    preparatory: true,
    placementGuarantee: false,
    voucherUnitPayments: true,
    extendedPreschoolTerm: false,
    intermittentShiftCare: true,
    citizenAttendanceSummary: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: true,
    serviceApplications: true,
    titaniaErrorsReport: true,
    multiSelectDeparture: true,
    voucherValueSeparation: false,
    nekkuIntegration: true,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    absenceApplications: true,
    missingQuestionnaireAnswerMarkerEnabled: true,
    showCitizenApplicationPreschoolTerms: true,
    showMetadataToCitizen: true,
    placementDesktop: true
  },
  staging: {
    environmentLabel: 'Staging',
    citizenShiftCareAbsence: false,
    assistanceActionOther: false,
    daycareApplication: {
      dailyTimes: true,
      serviceNeedOption: true
    },
    preschoolApplication: {
      connectedDaycarePreferredStartDate: true,
      serviceNeedOption: true
    },
    decisionDraftMultipleUnits: false,
    preschool: true,
    urgencyAttachments: true,
    preparatory: true,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: true,
    placementGuarantee: false,
    voucherUnitPayments: true,
    extendedPreschoolTerm: false,
    intermittentShiftCare: true,
    citizenAttendanceSummary: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: true,
    serviceApplications: true,
    titaniaErrorsReport: true,
    multiSelectDeparture: true,
    voucherValueSeparation: false,
    nekkuIntegration: true,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    absenceApplications: true,
    missingQuestionnaireAnswerMarkerEnabled: true,
    showCitizenApplicationPreschoolTerms: true,
    showMetadataToCitizen: true,
    placementDesktop: true
  },
  prod: {
    environmentLabel: null,
    citizenShiftCareAbsence: false,
    assistanceActionOther: false,
    daycareApplication: {
      dailyTimes: true,
      serviceNeedOption: true
    },
    preschoolApplication: {
      connectedDaycarePreferredStartDate: true,
      serviceNeedOption: true
    },
    decisionDraftMultipleUnits: false,
    preschool: true,
    urgencyAttachments: true,
    preparatory: true,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: true,
    placementGuarantee: false,
    voucherUnitPayments: true,
    extendedPreschoolTerm: false,
    intermittentShiftCare: false,
    citizenAttendanceSummary: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: false,
    serviceApplications: true,
    titaniaErrorsReport: true,
    multiSelectDeparture: true,
    voucherValueSeparation: false,
    nekkuIntegration: true,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    absenceApplications: true,
    missingQuestionnaireAnswerMarkerEnabled: true,
    showCitizenApplicationPreschoolTerms: true,
    showMetadataToCitizen: true,
    placementDesktop: true
  }
}

const featureFlags = features[env()]

export default featureFlags
