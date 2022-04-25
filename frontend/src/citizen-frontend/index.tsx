// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'core-js/stable'
import * as Sentry from '@sentry/browser'
import React from 'react'
import ReactDOM from 'react-dom'
import 'lib-common/assets/fonts/fonts.css'
import { polyfill as smoothScrollPolyfill } from 'seamless-scroll-polyfill'

import { getEnvironment } from 'lib-common/utils/helpers'
import 'leaflet/dist/leaflet.css'
import { appConfig } from 'lib-customizations/citizen'

import App from './App'
import './index.css'

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

// Let the HTML template inline script know we have loaded successfully
if (!window.evaka) {
  window.evaka = {}
}
