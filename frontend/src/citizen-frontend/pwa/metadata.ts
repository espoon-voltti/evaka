// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const appName = 'eVaka'
const themeColor = '#3273c9'

const appMeta: Record<string, string> = {
  'apple-mobile-web-app-title': appName,
  'apple-mobile-web-app-capable': 'yes',
  'apple-mobile-web-app-status-bar-style': 'default',
  'mobile-web-app-capable': 'yes'
}

const buildManifest = (origin: string) => ({
  id: `${origin}/`,
  name: appName,
  short_name: appName,
  display: 'standalone',
  scope: `${origin}/`,
  start_url: `${origin}/`,
  background_color: '#ffffff',
  theme_color: themeColor,
  icons: [512, 192, 180].map((size) => ({
    // Absolute, because a blob URL is a poor base for resolving a relative one.
    src: `${origin}/icons/evaka-${size}px.png`,
    sizes: `${size}x${size}`,
    type: 'image/png',
    purpose: 'maskable any'
  }))
})

// PWA manifest is created on-the-fly to allow putting it behind a feature flag
export function applyPwaMetadata(): void {
  for (const [name, content] of Object.entries(appMeta)) {
    const meta = document.createElement('meta')
    meta.name = name
    meta.content = content
    document.head.appendChild(meta)
  }

  const manifest = new Blob(
    [JSON.stringify(buildManifest(window.location.origin))],
    { type: 'application/manifest+json' }
  )
  const link = document.createElement('link')
  link.rel = 'manifest'
  link.href = URL.createObjectURL(manifest)
  document.head.appendChild(link)
}
