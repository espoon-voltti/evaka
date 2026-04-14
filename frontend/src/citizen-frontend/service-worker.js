// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* global self */

/// <reference lib="WebWorker" />
/** @type {ServiceWorkerGlobalScope} */
const serviceWorker = self

serviceWorker.addEventListener('install', (event) => {
  event.waitUntil(serviceWorker.skipWaiting())
})

serviceWorker.addEventListener('activate', (event) => {
  event.waitUntil(serviceWorker.clients.claim())
})

// Pass-through fetch handler. Chrome's PWA installability check requires the
// service worker to have a `fetch` listener — we don't actually want to cache
// anything because the citizen app needs live backend connectivity to be
// useful.
serviceWorker.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return
  event.respondWith(fetch(event.request))
})

serviceWorker.addEventListener('push', (event) => {
  const data = (() => {
    try {
      return event.data ? event.data.json() : {}
    } catch {
      return {}
    }
  })()
  const title = data.title ?? 'eVaka'
  event.waitUntil(
    serviceWorker.registration.showNotification(title, {
      body: data.body ?? '',
      icon: '/citizen/evaka-192px.png',
      badge: '/citizen/evaka-180px.png',
      tag: data.tag,
      data: { url: data.url ?? '/messages' }
    })
  )
})

serviceWorker.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url ?? '/messages'
  event.waitUntil(
    serviceWorker.clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((list) => {
        for (const client of list) {
          if (client.url.includes(url) && 'focus' in client) {
            return client.focus()
          }
        }
        return serviceWorker.clients.openWindow(url)
      })
  )
})
