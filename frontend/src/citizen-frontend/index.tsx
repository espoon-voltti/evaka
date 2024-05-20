// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'core-js/stable'
import * as Sentry from '@sentry/browser'
import React from 'react'
import { createRoot } from 'react-dom/client'
import 'lib-common/assets/fonts/fonts.css'
import { RouterProvider } from 'react-router-dom'
import { polyfill as smoothScrollPolyfill } from 'seamless-scroll-polyfill'

import { sentryEventFilter } from 'lib-common/sentry'
import { getEnvironment } from 'lib-common/utils/helpers'
import 'leaflet/dist/leaflet.css'
import { appConfig } from 'lib-customizations/citizen'

import router from './App'
import './index.css'

// Load Sentry before React to make Sentry's integrations work automatically
Sentry.init({
  enabled: appConfig.sentry?.enabled === true,
  dsn: appConfig.sentry?.dsn,
  environment: getEnvironment()
})
Sentry.getGlobalScope().addEventProcessor(sentryEventFilter)

// Smooth-scrolling requires polyfilling in Safari, IE and older browsers:
// https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollTo#browser_compatibility
// https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView#browser_compatibility
smoothScrollPolyfill()

// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
const root = createRoot(document.getElementById('app')!)
root.render(<RouterProvider router={router} />)

// Let the HTML template inline script know we have loaded successfully
if (!window.evaka) {
  window.evaka = {}
}
