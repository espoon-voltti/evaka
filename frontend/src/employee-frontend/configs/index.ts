// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createBrowserHistory } from 'history'
import { defaultsDeep } from 'lodash'
import { BaseAppConfig, DeepReadonly } from '@evaka/lib-common/types'
import { getEnvironment } from '@evaka/lib-common/utils/helpers'

export const baseHistory = (refresh = true) => {
  return createBrowserHistory({
    basename: '/employee',
    forceRefresh: refresh
  })
}

type AppConfig = DeepReadonly<BaseAppConfig>

const configs: Record<string, AppConfig> = {}
configs._default = {
  sentry: {
    dsn:
      'https://9b97efdb9ffc453c8cd12589367ab3b9@o318158.ingest.sentry.io/1821330',
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
