// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* global self */

/// <reference lib="WebWorker" />
/** @type {ServiceWorkerGlobalScope} */
const serviceWorker = self

serviceWorker.addEventListener('install', (event) => {
  // Attempt to replace previous service worker(s) immediately
  event.waitUntil(serviceWorker.skipWaiting())
})

serviceWorker.addEventListener('activate', (event) => {
  // Take over all pages from previous service worker(s)
  event.waitUntil(serviceWorker.clients.claim())
})

async function handlePushPayloads(/** @type{Array} */ payloads) {
  // Handle the *first* payload we know how to handle. If multiple versions
  // are present and supported, we should handle only the latest version.
  for (const payload of payloads) {
    if (payload.type === 'NotificationV1') {
      return await serviceWorker.registration.showNotification(
        payload.title,
        {}
      )
    }
  }
}

serviceWorker.addEventListener('push', (event) => {
  const json = event.data?.json()
  if (Array.isArray(json)) {
    event.waitUntil(handlePushPayloads(json))
  }
})

serviceWorker.addEventListener('notificationclick', (event) => {
  event.waitUntil(
    (async () => {
      const windows = await serviceWorker.clients.matchAll({ type: 'window' })
      const existingWindow = windows.at(0)
      if (existingWindow) {
        await existingWindow.focus()
      } else {
        await serviceWorker.clients.openWindow('/employee/mobile')
      }
      event.notification.close()
    })()
  )
})
