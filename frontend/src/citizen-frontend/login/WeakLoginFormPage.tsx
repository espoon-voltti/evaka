// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Redirect, useSearchParams } from 'wouter'

import { string } from 'lib-common/form/fields'
import { object, validated, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import { useMutationResult } from 'lib-common/query'
import { parseUrlWithOrigin } from 'lib-common/utils/parse-url-with-origin'
import Main from 'lib-components/atoms/Main'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import PasswordInputF from 'lib-components/molecules/PasswordInputF'
import { H1, H2, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { authPasskeyLogin, passkeysSupported } from '../auth/passkeys'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'
import useTitle from '../useTitle'

import { rememberLastLoginMethod } from './last-login-method'
import {
  HelpLinkRow,
  LoginCard,
  LoginColumns,
  LoginContainer,
  TopGap,
  WideAsyncButton,
  WideLinkButton
} from './layout'
import { authWeakLoginMutation } from './queries'

export default React.memo(function WeakLoginFormPage() {
  const i18n = useTranslation()
  useTitle(i18n, i18n.loginPage.login.formTitle)
  const user = useUser()

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')

  if (user) {
    return <Redirect to="/" replace />
  }

  return (
    <Main>
      <TopGap />
      <LoginContainer>
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        <LoginCard>
          <H1 $noMargin $hyphenate>
            {i18n.loginPage.login.formTitle}
          </H1>
          <Gap $size="L" />
          <LoginColumns>
            <section>
              <WeakLoginForm unvalidatedNextPath={unvalidatedNextPath} />
            </section>
            <section>
              <H2 $noMargin $smaller>
                {i18n.loginPage.login.forgotPassword}
              </H2>
              <Gap $size="xs" />
              <P $noMargin>{i18n.loginPage.login.forgotPasswordInfo}</P>
              <Gap $size="m" />
              <H2 $noMargin $smaller>
                {i18n.loginPage.login.noUsername}
              </H2>
              <Gap $size="xs" />
              <P $noMargin>{i18n.loginPage.login.noUsernameInfo}</P>
              <Gap $size="m" />
              <WideLinkButton
                href={getStrongLoginUri(unvalidatedNextPath ?? '/')}
                $style="secondary"
                data-qa="strong-login"
              >
                {i18n.loginPage.applying.link}
              </WideLinkButton>
            </section>
          </LoginColumns>
        </LoginCard>
        <HelpLinkRow />
      </LoginContainer>
      <Footer narrow />
    </Main>
  )
})

const weakLoginForm = object({
  username: validated(string(), nonBlank),
  // value<string>() is used to avoid trimming
  password: validated(value<string>(), nonBlank)
})

const WeakLoginForm = React.memo(function WeakLogin({
  unvalidatedNextPath
}: {
  unvalidatedNextPath: string | null
}) {
  const i18n = useTranslation()
  const t = i18n.loginPage.login
  const [rateLimitError, setRateLimitError] = useState(false)
  const [passkeyFailed, setPasskeyFailed] = useState(false)
  const { mutateAsync: authWeakLogin } = useMutationResult(
    authWeakLoginMutation
  )

  const nextUrl = useMemo(
    () =>
      unvalidatedNextPath
        ? parseUrlWithOrigin(window.location, unvalidatedNextPath)
        : undefined,
    [unvalidatedNextPath]
  )

  const form = useForm(
    weakLoginForm,
    () => ({ username: '', password: '' }),
    i18n.validationErrors
  )
  const { username, password } = useFormFields(form)

  // Offer passkeys in the username field's autofill as an invisible
  // enhancement (WebAuthn conditional mediation)
  useEffect(() => {
    if (!passkeysSupported()) return
    const abortController = new AbortController()
    void (async () => {
      if (!(await PublicKeyCredential.isConditionalMediationAvailable?.())) {
        return
      }
      const result = await authPasskeyLogin(
        'conditional',
        abortController.signal
      )
      if (result === 'success') {
        rememberLastLoginMethod('passkey')
        window.location.replace(nextUrl ?? '/')
      } else if (result === 'failure') {
        // A 'cancelled' result must stay silent: it also happens when the citizen
        // ignores the autofill suggestion, or when the effect aborts the ceremony
        setPasskeyFailed(true)
      }
    })().catch(() => undefined)

    // Abort the pending passkey login if the component unmounts or nextUrl changes
    return () => abortController.abort()
  }, [nextUrl])
  return (
    <form
      action=""
      onSubmit={(e) => e.preventDefault()}
      data-qa="weak-login-form"
    >
      <FixedSpaceColumn $spacing="m">
        {rateLimitError && <AlertBox message={t.rateLimitError} noMargin />}
        {passkeyFailed && (
          <InfoBox
            message={t.passkeyError(
              getStrongLoginUri(unvalidatedNextPath ?? '/')
            )}
            darkBackground
            wide
            data-qa="passkey-login-error"
          />
        )}
        <FixedSpaceColumn $spacing="zero">
          <Label htmlFor="username">{t.username}</Label>
          <InputFieldF
            id="username"
            data-qa="username"
            autoComplete="username webauthn"
            bind={username}
            // Avoid "required" warning when selecting passkey from autofill
            info={username.state === '' ? undefined : username.inputInfo()}
            placeholder={t.usernamePlaceholder}
            width="full"
            hideErrorsBeforeTouched={true}
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn $spacing="zero">
          <Label htmlFor="password">{t.password}</Label>
          <PasswordInputF
            id="password"
            data-qa="password"
            autoComplete="current-password"
            bind={password}
            placeholder={t.password}
            width="full"
            hideErrorsBeforeTouched={true}
          />
        </FixedSpaceColumn>
        <WideAsyncButton
          primary
          data-qa="login"
          type="submit"
          text={t.link}
          disabled={!form.isValid()}
          onClick={() => {
            const { username, password } = form.value()
            return authWeakLogin({ username, password })
          }}
          onSuccess={() => {
            rememberLastLoginMethod('email')
            window.location.replace(nextUrl ?? '/')
          }}
          onFailure={(error) => {
            if (error.statusCode === 429) {
              setRateLimitError(true)
            }
          }}
        />
      </FixedSpaceColumn>
    </form>
  )
})
