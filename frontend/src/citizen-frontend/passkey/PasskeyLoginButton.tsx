// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { P } from 'lib-components/typography'
import { faLockAlt } from 'lib-icons'

import { useTranslation } from '../localization'

import { usePasskeyAuth, useWebAuthnSupported } from './usePasskeyAuth'

interface Props {
  nextUrl: string | null
}

export const PasskeyLoginButton = React.memo(function PasskeyLoginButton({
  nextUrl
}: Props) {
  const t = useTranslation()
  const supported = useWebAuthnSupported()
  const { state, login } = usePasskeyAuth()

  const onClick = useCallback(async () => {
    const ok = await login()
    if (ok) {
      window.location.replace(nextUrl ?? '/')
    }
  }, [login, nextUrl])

  if (!supported) return null

  return (
    <FixedSpaceColumn $spacing="xs" data-qa="passkey-login-section">
      <Button
        appearance="button"
        primary
        icon={faLockAlt}
        text={t.loginPage.login.passkey.title}
        onClick={onClick}
        data-qa="passkey-login-button"
        data-testid="passkey-login-button"
      />
      <P>{t.loginPage.login.passkey.subtitle}</P>
      {state.status === 'error' && state.code === 'no-credentials' && (
        <P data-qa="passkey-no-credentials-hint">
          {t.loginPage.login.passkey.noCredentialsHint}
        </P>
      )}
      {state.status === 'error' &&
        state.code !== 'no-credentials' &&
        state.code !== 'cancelled' && (
          <P data-qa="passkey-generic-error">
            {t.loginPage.login.passkey.failed}
          </P>
        )}
    </FixedSpaceColumn>
  )
})
