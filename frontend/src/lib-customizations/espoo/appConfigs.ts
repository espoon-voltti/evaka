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
  'https://598dec1de405bd7ae255f5ee81edceb0@o4507111645052928.ingest.de.sentry.io/4509354693623888'

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
