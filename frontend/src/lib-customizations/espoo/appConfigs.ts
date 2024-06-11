// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { BaseAppConfig } from 'lib-customizations/types'

import { env, Env } from './env'

type AppConfigs = {
  default: BaseAppConfig
} & {
  [k in Env]: BaseAppConfig
}

const employeeConfigs: AppConfigs = {
  default: {
    sentry: {
      dsn: 'https://d2af0931b75ee492c4c1f4da85e035ea@o4507111645052928.ingest.de.sentry.io/4507412723990608',
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: 'https://d2af0931b75ee492c4c1f4da85e035ea@o4507111645052928.ingest.de.sentry.io/4507412723990608',
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: 'https://d2af0931b75ee492c4c1f4da85e035ea@o4507111645052928.ingest.de.sentry.io/4507412723990608',
      enabled: true
    }
  }
}

const employeeMobileConfigs: AppConfigs = {
  default: {
    sentry: {
      dsn: 'https://2e084169f4249f5096edc78d65d4c7bc@o4507111645052928.ingest.de.sentry.io/4507412751122512',
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: 'https://2e084169f4249f5096edc78d65d4c7bc@o4507111645052928.ingest.de.sentry.io/4507412751122512',
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: 'https://2e084169f4249f5096edc78d65d4c7bc@o4507111645052928.ingest.de.sentry.io/4507412751122512',
      enabled: true
    }
  }
}

const citizenConfigs: AppConfigs = {
  default: {
    sentry: {
      dsn: 'https://7f01c64aa4c21cdb5edfdf50ce1f4395@o4507111645052928.ingest.de.sentry.io/4507412540883024',
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: 'https://7f01c64aa4c21cdb5edfdf50ce1f4395@o4507111645052928.ingest.de.sentry.io/4507412540883024',
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: 'https://7f01c64aa4c21cdb5edfdf50ce1f4395@o4507111645052928.ingest.de.sentry.io/4507412540883024',
      enabled: true
    }
  }
}

const employeeConfig = employeeConfigs[env()]
const employeeMobileConfig = employeeMobileConfigs[env()]
const citizenConfig = citizenConfigs[env()]

export { employeeConfig, employeeMobileConfig, citizenConfig }
