// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const scriptUrl = '/service-worker.js'

export async function registerServiceWorker(): Promise<void> {
  if (!('serviceWorker' in navigator)) return
  await navigator.serviceWorker.register(scriptUrl)
}

// Unregistering is needed if the PWA feature flag is ever turned off in some environment
export async function unregisterServiceWorker(): Promise<void> {
  if (!('serviceWorker' in navigator)) return
  const registrations = await navigator.serviceWorker.getRegistrations()
  await Promise.all(
    registrations
      // Filter out the mobile frontend worker at /employee/mobile/
      .filter((registration) => new URL(registration.scope).pathname === '/')
      .map((registration) => registration.unregister())
  )
}
