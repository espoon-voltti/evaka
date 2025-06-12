// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { BaseAppConfig } from 'lib-customizations/types'

import type { Env } from './env'
import { env } from './env'

type AppConfigs = {
  default: BaseAppConfig
} & Record<Env, BaseAppConfig>

const sentryDsn =
  'https://7f01c64aa4c21cdb5edfdf50ce1f4395@o4507111645052928.ingest.de.sentry.io/4507412540883024'

const appConfigs: AppConfigs = {
  default: {
    sentry: {
      dsn: sentryDsn,
      enabled: false
    }
  },
  staging: {
    sentry: {
      dsn: sentryDsn,
      enabled: true
    }
  },
  prod: {
    sentry: {
      dsn: sentryDsn,
      enabled: true
    }
  }
}

const appConfig = appConfigs[env()]

export { appConfig }
