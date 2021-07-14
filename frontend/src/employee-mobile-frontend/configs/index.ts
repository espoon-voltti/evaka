// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createBrowserHistory } from 'history'
import { defaultsDeep } from 'lodash'
import { BaseAppConfig, DeepReadonly } from 'lib-common/types'
import { getEnvironment } from 'lib-common/utils/helpers'

export const baseHistory = (refresh = true) => {
  return createBrowserHistory({
    basename: '/employee/mobile',
    forceRefresh: refresh
  })
}

type AppConfig = DeepReadonly<BaseAppConfig>

const configs: Record<string, AppConfig> = {}
configs._default = {
  sentry: {
    dsn: 'https://0c9cf7b7c6f84c9ba953e2796db03cb7@o318158.ingest.sentry.io/5603170',
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
