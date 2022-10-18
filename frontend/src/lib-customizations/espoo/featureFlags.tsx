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
    citizenShiftCareAbsenceEnabled: true,
    assistanceActionOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true
    },
    groupsTableServiceNeedsEnabled: false,
    evakaLogin: true,
    financeBasicsPage: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    adminSettingsEnabled: false,
    experimental: {
      ai: true,
      messageAttachments: true,
      personalDetailsPage: true,
      mobileMessages: true,
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
    citizenShiftCareAbsenceEnabled: true,
    assistanceActionOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true
    },
    groupsTableServiceNeedsEnabled: false,
    evakaLogin: true,
    financeBasicsPage: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    adminSettingsEnabled: false,
    experimental: {
      ai: true,
      messageAttachments: true,
      personalDetailsPage: true,
      mobileMessages: true,
      leops: true,
      citizenVasu: true,
      assistanceNeedDecisions: true,
      assistanceNeedDecisionsLanguageSelect: true,
      staffAttendanceTypes: true,
      fosterParents: true
    }
  },
  prod: {
    citizenShiftCareAbsenceEnabled: true,
    assistanceActionOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true
    },
    groupsTableServiceNeedsEnabled: false,
    evakaLogin: true,
    financeBasicsPage: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    adminSettingsEnabled: false,
    experimental: {
      ai: false,
      messageAttachments: true,
      personalDetailsPage: false,
      mobileMessages: false,
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
