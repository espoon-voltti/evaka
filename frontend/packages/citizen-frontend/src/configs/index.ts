// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defaultsDeep } from 'lodash'
import { BaseAppConfig, DeepReadonly } from '@evaka/lib-common/src/types'
import { getEnvironment } from '@evaka/lib-common/src/utils/helpers'

type CitizenAppConfig = {
  enduserBaseUrl: string
}

type AppConfig = DeepReadonly<BaseAppConfig & CitizenAppConfig>

const configs: Record<string, AppConfig> = {}
configs._default = {
  enduserBaseUrl: '',
  sentry: {
    dsn: 'https://9b97efdb9ffc453c8cd12589367ab3b9@sentry.io/1821330',
    enabled: false
  }
}
// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
configs.local = defaultsDeep(
  {
    enduserBaseUrl: 'http://localhost:9091'
  },
  configs._default
)
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
