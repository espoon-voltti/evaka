// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'lib-common/assets/fonts/fonts.css'
import 'core-js/stable'
import * as Sentry from '@sentry/browser'
import {
  Chart,
  defaults,
  LinearScale,
  LineElement,
  PointElement,
  TimeScale,
  Tooltip
} from 'chart.js'
import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter } from 'react-router-dom'
import { polyfill as smoothScrollPolyfill } from 'seamless-scroll-polyfill'

import { getEnvironment } from 'lib-common/utils/helpers'
import colors from 'lib-customizations/common'
import { appConfig } from 'lib-customizations/employee'

import App from './App'
import 'chartjs-adapter-date-fns'
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

Chart.register(TimeScale, LinearScale, PointElement, LineElement, Tooltip)
Chart.defaults.animation = false
Chart.defaults.font = {
  family: '"Open Sans", "Arial", sans-serif',
  ...defaults.font
}
Chart.defaults.color = colors.grayscale.g100

ReactDOM.render(
  <BrowserRouter basename="/employee">
    <App />
  </BrowserRouter>,
  document.getElementById('app')
)

// Let the HTML template inline script know we have loaded successfully
if (!window.evaka) {
  window.evaka = {}
}
