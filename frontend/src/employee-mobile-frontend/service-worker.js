// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/// <reference lib="WebWorker" />
/** @type {ServiceWorkerGlobalScope} */
const serviceWorker = self

serviceWorker.addEventListener('install', () => {
  // Attempt to replace previous service worker(s) immediately
  void serviceWorker.skipWaiting()
})

serviceWorker.addEventListener('activate', (event) => {
  // Take over all pages from previous service worker(s)
  event.waitUntil(serviceWorker.clients.claim())
})
