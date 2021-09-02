// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'lib-common/assets/fonts/fonts.css'
import 'core-js/stable'
import React from 'react'
import ReactDOM from 'react-dom'
import * as Sentry from '@sentry/browser'
import { polyfill as smoothScrollPolyfill } from 'seamless-scroll-polyfill'
import App from './App'
import './index.css'
import { getEnvironment } from 'lib-common/utils/helpers'
import { appConfig } from 'lib-customizations/employee'

// Load Sentry before React to make Sentry's integrations work automatically
Sentry.init({
  enabled: appConfig.sentry?.enabled === true,
  dsn: appConfig.sentry?.dsn,
  environment: getEnvironment()
})

// Smooth-scrolling requires polyfilling in Safari, IE and older browsers:
// https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollTo#browser_compatibility
// https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView#browser_compatibility
smoothScrollPolyfill()

ReactDOM.render(<App />, document.getElementById('app'))
