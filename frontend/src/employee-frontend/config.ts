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
  showNewServiceNeedsList: boolean
  useNewServiceNeeds: boolean
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
    showNewServiceNeedsList: true,
    useNewServiceNeeds: true
  },
  staging: {
    voucherValueDecisionsPage: true,
    voucherServiceProviders: true,
    attachments: true,
    evakaLogin: true,
    messaging: true,
    showNewServiceNeedsList: true,
    useNewServiceNeeds: false
  },
  prod: {
    voucherValueDecisionsPage: false,
    voucherServiceProviders: false,
    attachments: false,
    evakaLogin: true,
    messaging: false,
    showNewServiceNeedsList: false,
    useNewServiceNeeds: false
  }
}

const featureFlags = features[env()]

export { featureFlags }
