// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeatureFlags } from 'lib-customizations/types'

type Env = 'staging' | 'prod'

type Features = {
  default: FeatureFlags
} & {
  [k in Env]: FeatureFlags
}

const env = (): Env | 'default' => {
  if (window.location.host === 'espoonvarhaiskasvatus.fi') {
    return 'prod'
  }

  if (window.location.host === 'staging.espoonvarhaiskasvatus.fi') {
    return 'staging'
  }

  return 'default'
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
      serviceProviders: true,
      valueDecisionsPage: true
    },
    experimental: {
      ai: true
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
      serviceProviders: true,
      valueDecisionsPage: true
    },
    experimental: {
      ai: true
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
      serviceProviders: false,
      valueDecisionsPage: false
    },
    experimental: {
      ai: false
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
