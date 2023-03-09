// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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

serviceWorker.addEventListener('push', (event) => {
  event.waitUntil(
    serviceWorker.registration.showNotification('Uusi viesti', {})
  )
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
