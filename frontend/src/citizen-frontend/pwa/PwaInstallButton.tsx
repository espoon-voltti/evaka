// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { useTranslation } from '../localization'

import { usePwaInstall } from './usePwaInstall'

export const PwaInstallButton = React.memo(function PwaInstallButton() {
  const i18n = useTranslation()
  const state = usePwaInstall()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((v) => !v), [])

  if (state.kind === 'standalone') {
    return null
  }

  if (state.kind === 'native') {
    return (
      <button
        type="button"
        onClick={() => {
          void state.promptInstall()
        }}
      >
        {i18n.loginPage.pwaInstall.button}
      </button>
    )
  }

  // fallback
  const platform = state.platform
  return (
    <>
      <button type="button" onClick={toggleOpen}>
        {i18n.loginPage.pwaInstall.button}
      </button>
      {open && (
        <div>
          {platform.os === 'ios' && (
            <>
              {!platform.isSafari && (
                <p>{i18n.loginPage.pwaInstall.iosUseSafariNote}</p>
              )}
              {i18n.loginPage.pwaInstall.instructions.ios}
            </>
          )}
          {platform.os === 'android' && (
            <>{i18n.loginPage.pwaInstall.instructions.android}</>
          )}
          {platform.os === 'other' && (
            <p>{i18n.loginPage.pwaInstall.notSupported}</p>
          )}
        </div>
      )}
    </>
  )
})
