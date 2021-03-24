// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defaultsDeep } from 'lodash'
import { BaseAppConfig, DeepReadonly } from 'lib-common/types'
import { getEnvironment } from 'lib-common/utils/helpers'

type AppConfig = DeepReadonly<BaseAppConfig>

const configs: Record<string, AppConfig> = {}
configs._default = {
  sentry: {
    dsn:
      'https://040a66e1905f432b9bd3a13a22d1d16e@o318158.ingest.sentry.io/5578235',
    enabled: false
  }
}
// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
configs.staging = defaultsDeep(
  {
    sentry: {
      enabled: true
    }
  },
  configs._default
)
// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
configs.prod = defaultsDeep(
  {
    sentry: {
      enabled: true
    }
  },
  configs._default
)

export const config: AppConfig = configs[getEnvironment()] || configs._default
