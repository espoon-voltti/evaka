// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeatureFlags } from 'lib-customizations/types'

import { env, Env } from './env'

type Features = {
  default: FeatureFlags
} & {
  [k in Env]: FeatureFlags
}

const features: Features = {
  default: {
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
    forceUnpublishDocumentTemplate: true
  },
  staging: {
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
    forceUnpublishDocumentTemplate: true
  },
  prod: {
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
    discussionReservations: false,
    forceUnpublishDocumentTemplate: false
  }
}

const featureFlags = features[env()]

export default featureFlags
