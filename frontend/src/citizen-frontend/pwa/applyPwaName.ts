// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { cityName, featureFlags } from 'lib-customizations/citizen'

const STATIC_MANIFEST_URL = '/citizen/manifest.json'

function pwaName(): string {
  const envLabel = featureFlags.environmentLabel
  return envLabel ? `eVaka ${cityName} ${envLabel}` : `eVaka ${cityName}`
}

function setAppleTitle(name: string) {
  const tag = document.querySelector<HTMLMetaElement>(
    'meta[name="apple-mobile-web-app-title"]'
  )
  if (tag) {
    tag.content = name
  }
}

async function rewriteManifest(name: string) {
  const link = document.querySelector<HTMLLinkElement>('link[rel="manifest"]')
  if (!link) return
  try {
    const res = await fetch(STATIC_MANIFEST_URL, { credentials: 'omit' })
    if (!res.ok) return
    const manifest = (await res.json()) as Record<string, unknown>
    manifest.name = name
    manifest.short_name = name
    const blob = new Blob([JSON.stringify(manifest)], {
      type: 'application/manifest+json'
    })
    link.href = URL.createObjectURL(blob)
  } catch {
    // Network or parse error — leave the static manifest in place.
  }
}

export function applyPwaName(): void {
  const name = pwaName()
  setAppleTitle(name)
  void rewriteManifest(name)
}
