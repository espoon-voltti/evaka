// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'core-js/stable'
import React from 'react'
import ReactDOM from 'react-dom'
import * as Sentry from '@sentry/browser'
import App from '~App'
import './index.scss'
import { getEnvironment } from '@evaka/lib-common/src/utils/helpers'
import { config } from '@evaka/employee-frontend/src/configs'

// Load Sentry before React to make Sentry's integrations work automatically
Sentry.init({
  enabled: config.sentry.enabled,
  dsn: config.sentry.dsn,
  environment: getEnvironment()
})

ReactDOM.render(<App />, document.getElementById('app'))
