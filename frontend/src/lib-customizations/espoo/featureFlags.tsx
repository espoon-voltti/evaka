// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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
    groupsTableServiceNeeds: false,
    preschool: true,
    preparatory: true,
    urgencyAttachments: true,
    experimental: {
      leops: true,
      citizenVasu: true,
      voucherUnitPayments: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: true,
      fosterParents: true
    }
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
    groupsTableServiceNeeds: false,
    preschool: true,
    preparatory: true,
    urgencyAttachments: true,
    experimental: {
      leops: true,
      citizenVasu: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: true,
      fosterParents: true
    }
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
    groupsTableServiceNeeds: false,
    preschool: true,
    preparatory: true,
    urgencyAttachments: true,
    experimental: {
      leops: true,
      citizenVasu: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: false,
      fosterParents: true
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
