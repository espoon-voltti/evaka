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
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    evakaLogin: true,
    financeBasicsPage: true,
    messaging: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    vasu: true,
    voucher: {
      valueDecisionsPage: true
    },
    experimental: {
      ai: true,
      mobileDailyNotes: true
    }
  },
  staging: {
    assistanceActionOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    evakaLogin: true,
    financeBasicsPage: true,
    messaging: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    vasu: false,
    voucher: {
      valueDecisionsPage: true
    },
    experimental: {
      ai: true,
      mobileDailyNotes: false
    }
  },
  prod: {
    assistanceActionOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    evakaLogin: true,
    financeBasicsPage: true,
    messaging: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    vasu: false,
    voucher: {
      valueDecisionsPage: false
    },
    experimental: {
      ai: false,
      mobileDailyNotes: false
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
