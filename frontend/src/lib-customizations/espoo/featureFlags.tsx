// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeatureFlags } from 'lib-customizations/types'

import { env, Env } from './env'

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
    assistanceNeedDecisionsLanguageSelect: true,
    staffAttendanceTypes: true,
    extendedPreschoolTerm: true,
    personDuplicate: false,
    citizenAttendanceSummary: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    discussionReservations: true,
    jamixIntegration: true,
    forceUnpublishDocumentTemplate: true,
    invoiceDisplayAccountNumber: true,
    serviceApplications: true
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
    staffAttendanceTypes: true,
    extendedPreschoolTerm: true,
    personDuplicate: false,
    citizenAttendanceSummary: false,
    voucherUnitPayments: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    discussionReservations: true,
    jamixIntegration: true,
    forceUnpublishDocumentTemplate: true,
    invoiceDisplayAccountNumber: true,
    serviceApplications: true,
    calendarMonthView: true
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
    staffAttendanceTypes: false,
    extendedPreschoolTerm: true,
    personDuplicate: false,
    citizenAttendanceSummary: false,
    voucherUnitPayments: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    discussionReservations: true,
    forceUnpublishDocumentTemplate: false,
    invoiceDisplayAccountNumber: true,
    serviceApplications: false
  }
}

const featureFlags = features[env()]

export default featureFlags
