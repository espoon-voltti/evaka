// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* global self */

/// <reference lib="WebWorker" />
/** @type {ServiceWorkerGlobalScope} */
const serviceWorker = self

const cachePrefix = 'citizen-offline-'
// Bump whenever offline.html changes: the install handler that precaches it
// runs again only when this script's own bytes differ, so an unchanged name
// leaves the stale copy in place.
const cacheName = `${cachePrefix}v1`
const offlinePage = '/offline.html'

serviceWorker.addEventListener('install', (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(cacheName)
      await cache.add(new Request(offlinePage, { cache: 'reload' }))
      // Attempt to replace previous service worker(s) immediately
      await serviceWorker.skipWaiting()
    })()
  )
})

serviceWorker.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      const names = await caches.keys()
      await Promise.all(
        names
          .filter((name) => name.startsWith(cachePrefix) && name !== cacheName)
          .map((name) => caches.delete(name))
      )
      // Take over all pages from previous service worker(s)
      await serviceWorker.clients.claim()
    })()
  )
})

serviceWorker.addEventListener('fetch', (event) => {
  if (event.request.mode !== 'navigate') return

  // This service worker is registered at the origin root, so its scope also
  // covers the employee frontends, which are separate applications and must
  // never be served the citizen offline page.
  if (new URL(event.request.url).pathname.startsWith('/employee')) return

  event.respondWith(
    (async () => {
      try {
        return await fetch(event.request)
      } catch (_e) {
        const cache = await caches.open(cacheName)
        const cached = await cache.match(offlinePage)
        return cached ?? Response.error()
      }
    })()
  )
})

// Backend sends the push payload in the Declarative Web Push format,
// which is a JSON object with a `web_push` field set to 8030 and a
// `notification` field containing the notification data. Browsers that
// understand this format show the notification and handle the click
// themselves, and never run the `push` or `notificationclick` event
// handlers. Browsers that do not understand this format run the event
// handlers, which show the notification and handle the click themselves.
//
// As of Sep 2026: iOS handles declarative web push notifications itself, Android does not
//
// Reference: https://w3c.github.io/push-api/

async function showDeclarativeNotification(/** @type{object} */ notification) {
  return await serviceWorker.registration.showNotification(notification.title, {
    body: notification.body ?? undefined,
    icon: '/icons/evaka-192px.png',
    tag: notification.tag,
    data: { navigate: notification.navigate }
  })
}

serviceWorker.addEventListener('push', (event) => {
  const json = event.data?.json()
  if (json?.web_push === 8030) {
    event.waitUntil(showDeclarativeNotification(json.notification))
  }
})

const notificationAckTimeoutMs = 500

// Tell the app to route to the path without reloading. Resolves to `true` if the app
// received the message.
function requestRouting(
  /** @type{WindowClient} */ client,
  /** @type{string} */ path
) {
  return new Promise((resolve) => {
    const channel = new MessageChannel()
    const timeout = setTimeout(() => resolve(false), notificationAckTimeoutMs)

    // Client responds when it has received the message
    channel.port1.onmessage = () => {
      clearTimeout(timeout)
      resolve(true)
    }

    client.postMessage({ type: 'notification-click', path }, [channel.port2])
  })
}

async function openNotificationTarget(/** @type{URL} */ url) {
  const clients = await serviceWorker.clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  })
  const citizenClients = clients.filter(
    (client) => !new URL(client.url).pathname.startsWith('/employee')
  )
  const client =
    citizenClients.find((c) => c.visibilityState === 'visible') ??
    citizenClients[0]
  if (!client) {
    await serviceWorker.clients.openWindow(url.href)
    return
  }
  await client.focus()
  if (!(await requestRouting(client, url.pathname + url.search + url.hash))) {
    await client.navigate(url.href)
  }
}

serviceWorker.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const navigate = event.notification.data?.navigate
  event.waitUntil(
    openNotificationTarget(
      new URL(navigate ?? '/', serviceWorker.location.origin)
    )
  )
})
