// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from '../localization'
import type { BrowserInfo } from '../pwa/detectPlatform'

interface Props {
  browser: BrowserInfo
}

export const PermissionGuide = React.memo(function PermissionGuide({ browser }: Props) {
  const t = useTranslation()
  const guide = t.personalDetails.webPushSection.guide

  const body = (() => {
    if (browser.os === 'ios' && browser.family === 'safari') return guide.safariIOS
    if (browser.os === 'android' && browser.family === 'chrome') return guide.chromeAndroid
    if (browser.os === 'android' && browser.family === 'samsung-internet') return guide.samsungAndroid
    if (browser.os === 'android' && browser.family === 'firefox') return guide.firefoxAndroid
    if (browser.family === 'chrome' || browser.family === 'edge') return guide.chromeDesktop
    if (browser.family === 'firefox') return guide.firefoxDesktop
    if (browser.os === 'macos' && browser.family === 'safari') return guide.safariMacos
    return guide.fallback
  })()

  return (
    <div role="group" data-qa="webpush-permission-guide">
      {body}
    </div>
  )
})
