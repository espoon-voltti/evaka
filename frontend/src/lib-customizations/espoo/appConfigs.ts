// SPDX-FileCopyrightText: 2017-2021 City of Espoo
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
      dsn: 'https://9b97efdb9ffc453c8cd12589367ab3b9@o318158.ingest.sentry.io/1821330',
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: 'https://9b97efdb9ffc453c8cd12589367ab3b9@o318158.ingest.sentry.io/1821330',
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: 'https://9b97efdb9ffc453c8cd12589367ab3b9@o318158.ingest.sentry.io/1821330',
      enabled: true
    }
  }
}

const employeeMobileConfigs: AppConfigs = {
  default: {
    sentry: {
      dsn: 'https://0c9cf7b7c6f84c9ba953e2796db03cb7@o318158.ingest.sentry.io/5603170',
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: 'https://0c9cf7b7c6f84c9ba953e2796db03cb7@o318158.ingest.sentry.io/5603170',
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: 'https://0c9cf7b7c6f84c9ba953e2796db03cb7@o318158.ingest.sentry.io/5603170',
      enabled: true
    }
  }
}

const citizenConfigs: AppConfigs = {
  default: {
    sentry: {
      dsn: 'https://040a66e1905f432b9bd3a13a22d1d16e@o318158.ingest.sentry.io/5578235',
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: 'https://040a66e1905f432b9bd3a13a22d1d16e@o318158.ingest.sentry.io/5578235',
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: 'https://040a66e1905f432b9bd3a13a22d1d16e@o318158.ingest.sentry.io/5578235',
      enabled: true
    }
  }
}

const employeeConfig = employeeConfigs[env()]
const employeeMobileConfig = employeeMobileConfigs[env()]
const citizenConfig = citizenConfigs[env()]

export { employeeConfig, employeeMobileConfig, citizenConfig }
