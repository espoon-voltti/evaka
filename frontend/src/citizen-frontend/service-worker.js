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
