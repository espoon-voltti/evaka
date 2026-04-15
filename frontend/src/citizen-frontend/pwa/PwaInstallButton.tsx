// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

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
      <Button
        appearance="button"
        icon={faPlus}
        text={i18n.loginPage.pwaInstall.button}
        onClick={() => {
          void state.promptInstall()
        }}
      />
    )
  }

  const platform = state.platform
  return (
    <>
      <Button
        appearance="button"
        icon={faPlus}
        text={i18n.loginPage.pwaInstall.button}
        onClick={toggleOpen}
      />
      {open && (
        <>
          <Gap $size="s" />
          {platform.os === 'ios' && (
            <>
              {!platform.isSafari && (
                <P $noMargin>{i18n.loginPage.pwaInstall.iosUseSafariNote}</P>
              )}
              {i18n.loginPage.pwaInstall.instructions.ios}
            </>
          )}
          {platform.os === 'android' && (
            <>{i18n.loginPage.pwaInstall.instructions.android}</>
          )}
          {platform.os === 'other' && (
            <P $noMargin>{i18n.loginPage.pwaInstall.notSupported}</P>
          )}
        </>
      )}
    </>
  )
})
