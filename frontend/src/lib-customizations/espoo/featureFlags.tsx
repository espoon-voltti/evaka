// SPDX-FileCopyrightText: 2017-2021 City of Espoo
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
    assistanceActionOtherEnabled: true,
    childIncomeEnabled: false,
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
      realtimeStaffAttendance: false,
      personalDetailsPage: true,
      mobileMessages: true,
      leops: true
    }
  },
  staging: {
    assistanceActionOtherEnabled: true,
    childIncomeEnabled: false,
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
      realtimeStaffAttendance: true,
      personalDetailsPage: true,
      mobileMessages: true,
      leops: true
    }
  },
  prod: {
    assistanceActionOtherEnabled: true,
    childIncomeEnabled: false,
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
      realtimeStaffAttendance: false,
      personalDetailsPage: false,
      mobileMessages: false,
      leops: true
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
