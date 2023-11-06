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
      dailyTimes: true
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
    childDiscussion: false,
    feeDecisionPreschoolClubFilter: false,
    placementGuarantee: true,
    voucherUnitPayments: true,
    assistanceNeedDecisionsLanguageSelect: true,
    staffAttendanceTypes: true,
    personDuplicate: false,
    citizenAttendanceSummary: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    childDocuments: true,
    assistanceNeedPreschoolDecisions: true,
    feeDecisionIgnoredStatus: true,
    hojks: true,
    employeeMobileStaffAttendanceEdit: true,
    voucherValueDecisionIgnoredStatus: true
  },
  staging: {
    citizenShiftCareAbsence: true,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: true
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
    childDiscussion: false,
    feeDecisionPreschoolClubFilter: false,
    placementGuarantee: true,
    assistanceNeedDecisionsLanguageSelect: true,
    staffAttendanceTypes: true,
    personDuplicate: false,
    citizenAttendanceSummary: false,
    voucherUnitPayments: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    childDocuments: true,
    assistanceNeedPreschoolDecisions: true,
    feeDecisionIgnoredStatus: true,
    hojks: true,
    employeeMobileStaffAttendanceEdit: true,
    voucherValueDecisionIgnoredStatus: false
  },
  prod: {
    citizenShiftCareAbsence: true,
    assistanceActionOther: true,
    daycareApplication: {
      dailyTimes: true
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
    childDiscussion: false,
    feeDecisionPreschoolClubFilter: false,
    placementGuarantee: true,
    assistanceNeedDecisionsLanguageSelect: true,
    staffAttendanceTypes: false,
    personDuplicate: false,
    citizenAttendanceSummary: false,
    voucherUnitPayments: false,
    intermittentShiftCare: false,
    noAbsenceType: false,
    childDocuments: true,
    assistanceNeedPreschoolDecisions: true,
    feeDecisionIgnoredStatus: true,
    hojks: false,
    employeeMobileStaffAttendanceEdit: false,
    voucherValueDecisionIgnoredStatus: false
  }
}

const featureFlags = features[env()]

export default featureFlags
