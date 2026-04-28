{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

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
    assistanceActionOther: true,
    financeDecisionHandlerSelect: false,
    feeDecisionPreschoolClubFilter: true,
    daycareApplication: {
      dailyTimes: false,
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
    placementGuarantee: true,
    voucherUnitPayments: true,
    extendedPreschoolTerm: false,
    intermittentShiftCare: true,
    citizenAttendanceSummary: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: true,
    serviceApplications: true,
    titaniaErrorsReport: false,
    multiSelectDeparture: true,
    voucherValueSeparation: false,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    absenceApplications: true,
    showCitizenApplicationPreschoolTerms: true,
    showMetadataToCitizen: true,
    placementDesktop: false,
    employeeLanguageSelection: true,
    decisionReasoningOptions: true
  },
  staging: {
    environmentLabel: 'Staging',
    citizenShiftCareAbsence: false,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: false,
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
    feeDecisionPreschoolClubFilter: true,
    placementGuarantee: true,
    voucherUnitPayments: true,
    extendedPreschoolTerm: false,
    intermittentShiftCare: true,
    citizenAttendanceSummary: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: true,
    serviceApplications: true,
    titaniaErrorsReport: false,
    multiSelectDeparture: true,
    voucherValueSeparation: false,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    absenceApplications: true,
    showCitizenApplicationPreschoolTerms: true,
    showMetadataToCitizen: true,
    placementDesktop: true,
    employeeLanguageSelection: true,
    decisionReasoningOptions: true
  },
  prod: {
    environmentLabel: null,
    citizenShiftCareAbsence: false,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: false,
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
    titaniaErrorsReport: false,
    multiSelectDeparture: true,
    voucherValueSeparation: false,
    citizenChildDocumentTypes: true,
    decisionChildDocumentTypes: true,
    absenceApplications: true,
    showCitizenApplicationPreschoolTerms: true,
    showMetadataToCitizen: true,
    placementDesktop: true,
    employeeLanguageSelection: false
  }
}

const featureFlags = features[env()]

export default featureFlags
