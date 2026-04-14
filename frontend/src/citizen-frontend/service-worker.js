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
  // Backend sends a JSON array of WebPushPayload entries (sealed class, tagged with
  // `type`), not a single object, so we pick the first NotificationV1 entry.
  const payload = (() => {
    try {
      const parsed = event.data ? event.data.json() : null
      const list = Array.isArray(parsed) ? parsed : parsed ? [parsed] : []
      return (
        list.find((p) => p && p.type === 'NotificationV1') ?? list[0] ?? {}
      )
    } catch {
      return {}
    }
  })()
  const title = payload.title ?? 'eVaka'
  const actions = []
  if (payload.replyAction) {
    actions.push({
      action: 'reply',
      type: 'text',
      title: payload.replyAction.actionLabel,
      placeholder: payload.replyAction.actionPlaceholder
    })
  }
  event.waitUntil(
    serviceWorker.registration.showNotification(title, {
      body: payload.body ?? '',
      icon: '/citizen/evaka-192px.png',
      // Android Chrome uses the alpha channel of the badge image to draw a
      // monochrome mask in the status bar, so the file is a transparent PNG
      // with just the "e" glyph opaque.
      badge: '/citizen/evaka-badge-72.png',
      tag: payload.tag,
      data: {
        url: payload.url ?? '/messages',
        replyAction: payload.replyAction ?? null
      },
      actions
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
