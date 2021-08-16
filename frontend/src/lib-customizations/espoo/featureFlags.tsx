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

// Tuomarilan pk, Kartanonpuiston pk, Ruusulinnan pk, Säterin kerho, Kartanonvintin rppk, Storängens daghem och förskola, Järvenperän pk, Luhtaniityn pk, Suvituuli rppk, Kesäheinä rppk, Sunakatti pk
// The ones starting with letters are all different "groups" of Säterin kerho
const espooPilotUnitIds = [
  '2dd63494-788e-11e9-bd69-b3441c1df64b',
  '2ddc2390-788e-11e9-bdc6-93f4e99145c6',
  '2ddf8300-788e-11e9-bdf9-f7700f85506b',
  '2df11214-788e-11e9-bef8-a30aaf67b874',
  '2dff766a-788e-11e9-bfea-27258547b1dc',
  '2dcf4530-788e-11e9-bd15-9b874fded9b9',
  '2dd4b664-788e-11e9-bd51-4f6bec28e68b',
  '2def27b0-788e-11e9-bed7-d38b0305d689',
  '2deef8f8-788e-11e9-bed4-c3aec9f586af',
  '40e834d8-3152-11ea-9ad7-1761bdb4e741',
  'e6bae2a4-5c43-11ea-9921-9ff01014d16d',
  'e6d2b190-5c43-11ea-9922-87e29b5189e3',
  'd5cce2d8-2abe-11e9-8db0-4f32672b511c',
  'd5cd0326-2abe-11e9-8db1-93687938341f',
  'd5cd22f2-2abe-11e9-8db2-57da70f23901',
  'e6e37750-5c43-11ea-9923-df804c6c209c'
]

const features: Features = {
  default: {
    assistanceActionOtherEnabled: true,
    assistanceBasisOtherEnabled: true,
    daycareApplication: {
      dailyTimesEnabled: true,
      serviceNeedOptionsEnabled: false
    },
    employeeMobile: () => true,
    evakaLogin: true,
    financeBasicsPage: true,
    messaging: () => true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    experimental: {
      ai: true,
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
    employeeMobile: () => true,
    evakaLogin: true,
    financeBasicsPage: true,
    messaging: () => true,
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    experimental: {
      ai: true,
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
    employeeMobile: (unitId: string) => espooPilotUnitIds.includes(unitId),
    evakaLogin: true,
    financeBasicsPage: true,
    messaging: (unitId: string) => espooPilotUnitIds.includes(unitId),
    preschoolEnabled: true,
    urgencyAttachmentsEnabled: true,
    experimental: {
      ai: false,
      vasu: false
    }
  }
}

const featureFlags = features[env()]

export default featureFlags
