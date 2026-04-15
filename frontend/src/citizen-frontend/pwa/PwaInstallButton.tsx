// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faTimes, faPlus } from 'lib-icons'

import { useTranslation } from '../localization'

import { usePwaInstall } from './usePwaInstall'

const DISMISSED_KEY = 'evaka.pwaInstallDismissed'

export const PwaInstallButton = React.memo(function PwaInstallButton() {
  const i18n = useTranslation()
  const state = usePwaInstall()
  const [open, setOpen] = useState(false)
  const [dismissed, setDismissed] = useState(
    () =>
      typeof localStorage !== 'undefined' &&
      localStorage.getItem(DISMISSED_KEY) === 'true'
  )
  const toggleOpen = useCallback(() => setOpen((v) => !v), [])
  const dismiss = useCallback(() => {
    try {
      localStorage.setItem(DISMISSED_KEY, 'true')
    } catch {
      // localStorage may be blocked; dismissal is then session-only
    }
    setDismissed(true)
  }, [])

  if (state.kind === 'standalone' || dismissed) {
    return null
  }

  const installButton =
    state.kind === 'native' ? (
      <Button
        appearance="button"
        icon={faPlus}
        text={i18n.loginPage.pwaInstall.button}
        onClick={() => {
          void state.promptInstall()
        }}
      />
    ) : (
      <Button
        appearance="button"
        icon={faPlus}
        text={i18n.loginPage.pwaInstall.button}
        onClick={toggleOpen}
      />
    )

  const platform = state.kind === 'fallback' ? state.platform : null

  return (
    <>
      <FixedSpaceRow $alignItems="center" $spacing="s">
        {installButton}
        <IconOnlyButton
          icon={faTimes}
          color="gray"
          onClick={dismiss}
          aria-label={i18n.common.close}
          data-qa="pwa-install-dismiss"
        />
      </FixedSpaceRow>
      {open && platform && (
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
