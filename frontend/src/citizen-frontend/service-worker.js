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
