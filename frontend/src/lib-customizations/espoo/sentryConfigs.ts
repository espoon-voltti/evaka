// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { BrowserOptions } from '@sentry/browser'

import { env } from './env'

const environment = env()
const enabled = environment === 'staging' || environment === 'prod'

export const employeeConfig: BrowserOptions | undefined = enabled
  ? {
      dsn: 'https://9b97efdb9ffc453c8cd12589367ab3b9@o318158.ingest.sentry.io/1821330',
      environment
    }
  : undefined
export const employeeMobileConfig: BrowserOptions | undefined = enabled
  ? {
      dsn: 'https://0c9cf7b7c6f84c9ba953e2796db03cb7@o318158.ingest.sentry.io/5603170',
      environment
    }
  : undefined
export const citizenConfig: BrowserOptions | undefined = enabled
  ? {
      dsn: 'https://040a66e1905f432b9bd3a13a22d1d16e@o318158.ingest.sentry.io/5578235',
      environment
    }
  : undefined
