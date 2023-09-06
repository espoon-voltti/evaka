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
    citizenContractDayAbsence: false,
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
    hideGeneralSupportDaycareAssistanceLevel: true,
    experimental: {
      leops: true,
      voucherUnitPayments: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: true,
      fosterParents: true,
      serviceWorkerMessaging: true,
      childDocuments: true,
      assistanceNeedPreschoolDecisions: true,
      citizenEmailNotificationSettings: true,
      feeDecisionIgnoredStatus: true
    }
  },
  staging: {
    citizenShiftCareAbsence: true,
    citizenContractDayAbsence: false,
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
    hideGeneralSupportDaycareAssistanceLevel: false,
    experimental: {
      leops: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: true,
      fosterParents: true,
      serviceWorkerMessaging: true,
      childDocuments: true,
      assistanceNeedPreschoolDecisions: true,
      citizenEmailNotificationSettings: true,
      feeDecisionIgnoredStatus: true
    }
  },
  prod: {
    citizenShiftCareAbsence: true,
    citizenContractDayAbsence: false,
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
    hideGeneralSupportDaycareAssistanceLevel: false,
    experimental: {
      leops: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: false,
      fosterParents: true,
      serviceWorkerMessaging: true,
      childDocuments: false,
      citizenEmailNotificationSettings: true,
      feeDecisionIgnoredStatus: true
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
