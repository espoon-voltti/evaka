// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const scriptUrl = '/service-worker.js'

// Duplicated from service-worker.js, which is registered as a classic worker
// and therefore cannot import shared modules
const cachePrefix = 'citizen-offline-'

export async function registerServiceWorker(): Promise<void> {
  if (!('serviceWorker' in navigator)) return
  await navigator.serviceWorker.register(scriptUrl)
}

// Unregistering is needed if the PWA feature flag is ever turned off in some environment
export async function unregisterServiceWorker(): Promise<void> {
  if ('serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations()
    await Promise.all(
      registrations
        // Filter out the mobile frontend worker at /employee/mobile/
        .filter((registration) => new URL(registration.scope).pathname === '/')
        .map((registration) => registration.unregister())
    )
  }
  // The worker that would normally evict these during activation is gone, so
  // they are deleted here. An unregistered worker still controls open pages
  // until they navigate and can recreate a cache, so this runs on every load
  // while the feature flag is off.
  await deleteOfflineCaches()
}

async function deleteOfflineCaches(): Promise<void> {
  if (!('caches' in window)) return
  const names = await caches.keys()
  await Promise.all(
    names
      .filter((name) => name.startsWith(cachePrefix))
      .map((name) => caches.delete(name))
  )
}
