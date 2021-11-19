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
    assistanceBasisOtherEnabled: false,
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
      realtimeStaffAttendance: false
    }
  },
  staging: {
    assistanceActionOtherEnabled: true,
    assistanceBasisOtherEnabled: false,
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
      realtimeStaffAttendance: true
    }
  },
  prod: {
    assistanceActionOtherEnabled: true,
    assistanceBasisOtherEnabled: false,
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
      realtimeStaffAttendance: false
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
