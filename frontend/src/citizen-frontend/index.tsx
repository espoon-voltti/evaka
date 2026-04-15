// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import { createRoot } from 'react-dom/client'
import 'lib-common/assets/fonts/fonts.css'
import { polyfill as smoothScrollPolyfill } from 'seamless-scroll-polyfill'

import { appVersion } from 'lib-common/globals'
import { sentryEventFilter } from 'lib-common/sentry'
import { getEnvironment } from 'lib-common/utils/helpers'
import 'leaflet/dist/leaflet.css'
import { appConfig } from 'lib-customizations/citizen'

import { applyPwaName } from './pwa/applyPwaName'
import Root from './router'
import './index.css'

// Load Sentry before React to make Sentry's integrations work automatically
Sentry.init({
  enabled: appConfig.sentry?.enabled === true,
  dsn: appConfig.sentry?.dsn,
  release: appVersion,
  environment: getEnvironment()
})
Sentry.getGlobalScope().addEventProcessor(sentryEventFilter)

applyPwaName()

// Smooth-scrolling requires polyfilling in Safari, IE and older browsers:
// https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollTo#browser_compatibility
// https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView#browser_compatibility
smoothScrollPolyfill()

const root = createRoot(document.getElementById('app')!)
root.render(<Root />)

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/service-worker.js', { scope: '/' })
      .catch((err) => {
        Sentry.captureException(err)
      })
  })
}

// Let the HTML template inline script know we have loaded successfully
if (!window.evaka) {
  window.evaka = {}
}
