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
    assistanceBasisOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    groupsTableServiceNeedsEnabled: false,
    evakaLogin: true,
    financeBasicsPage: true,
    pedagogicalDocumentsEnabled: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    experimental: {
      ai: true,
      incomeStatements: true,
      messageAttachments: true,
      vasu: true
    }
  },
  staging: {
    assistanceActionOtherEnabled: true,
    assistanceBasisOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    groupsTableServiceNeedsEnabled: false,
    evakaLogin: true,
    financeBasicsPage: true,
    pedagogicalDocumentsEnabled: true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    experimental: {
      ai: true,
      incomeStatements: true,
      messageAttachments: true,
      vasu: true
    }
  },
  prod: {
    assistanceActionOtherEnabled: true,
    assistanceBasisOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    groupsTableServiceNeedsEnabled: false,
    evakaLogin: true,
    financeBasicsPage: true,
    pedagogicalDocumentsEnabled: false,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    experimental: {
      ai: false,
      incomeStatements: false,
      messageAttachments: false,
      vasu: false
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
