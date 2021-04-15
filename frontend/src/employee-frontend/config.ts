// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

type Env = 'staging' | 'prod'

const env = (): Env | 'default' => {
  if (window.location.host === 'espoonvarhaiskasvatus.fi') {
    return 'prod'
  }

  if (window.location.host === 'staging.espoonvarhaiskasvatus.fi') {
    return 'staging'
  }

  return 'default'
}

type FeatureFlags = {
  voucherValueDecisionsPage: boolean
  voucherServiceProviders: boolean
  attachments: boolean
  evakaLogin: boolean
  messaging: boolean
  newServiceNeeds: boolean
}

type Features = {
  default: FeatureFlags
} & {
  [k in Env]: FeatureFlags
}

const features: Features = {
  default: {
    voucherValueDecisionsPage: true,
    voucherServiceProviders: true,
    attachments: true,
    evakaLogin: true,
    messaging: true,
    newServiceNeeds: true
  },
  staging: {
    voucherValueDecisionsPage: true,
    voucherServiceProviders: true,
    attachments: true,
    evakaLogin: true,
    messaging: true,
    newServiceNeeds: true
  },
  prod: {
    voucherValueDecisionsPage: false,
    voucherServiceProviders: false,
    attachments: false,
    evakaLogin: false,
    messaging: false,
    newServiceNeeds: false
  }
}

const featureFlags = features[env()]

export { featureFlags }
