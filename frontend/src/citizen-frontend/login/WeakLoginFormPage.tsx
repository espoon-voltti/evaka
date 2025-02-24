// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router'

import { authWeakLogin } from 'citizen-frontend/auth/api'
import { wrapResult } from 'lib-common/api'
import { string } from 'lib-common/form/fields'
import { object, required, validated, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import { parseUrlWithOrigin } from 'lib-common/utils/parse-url-with-origin'
import Main from 'lib-components/atoms/Main'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'

export default React.memo(function WeakLoginFormPage() {
  const i18n = useTranslation()
  const user = useUser()

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')

  if (user) {
    return <Navigate to="/" replace />
  }

  return (
    <Main>
      <TabletAndDesktop>
        <Gap size="L" />
      </TabletAndDesktop>
      <MobileOnly>
        <Gap size="xs" />
      </MobileOnly>
      <Container>
        <FixedSpaceColumn spacing="s">
          <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
          <ContentArea opaque>
            <H1 noMargin>{i18n.loginPage.login.title}</H1>
            <Gap size="m" />
            <WeakLoginForm unvalidatedNextPath={unvalidatedNextPath} />
          </ContentArea>
        </FixedSpaceColumn>
      </Container>
      <Footer />
    </Main>
  )
})

const weakLoginForm = object({
  username: validated(required(string()), nonBlank),
  // value<string> is used to avoid trimming
  password: validated(required(value<string>()), nonBlank)
})

const authWeakLoginResult = wrapResult(authWeakLogin)

const WeakLoginForm = React.memo(function WeakLogin({
  unvalidatedNextPath
}: {
  unvalidatedNextPath: string | null
}) {
  const i18n = useTranslation()
  const t = i18n.loginPage.login
  const [rateLimitError, setRateLimitError] = useState(false)

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
  return (
    <form
      action=""
      onSubmit={(e) => e.preventDefault()}
      data-qa="weak-login-form"
    >
      <FixedSpaceColumn spacing="L">
        {rateLimitError && <AlertBox message={t.rateLimitError} />}
        <FixedSpaceColumn spacing="zero">
          <Label htmlFor="username">{t.username}</Label>
          <InputFieldF
            id="username"
            data-qa="username"
            autoComplete="username"
            bind={username}
            placeholder={t.username}
            width="L"
            hideErrorsBeforeTouched={true}
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="zero">
          <Label htmlFor="password">{t.password}</Label>
          <InputFieldF
            id="password"
            data-qa="password"
            autoComplete="current-password"
            bind={password}
            type="password"
            placeholder={t.password}
            width="L"
            hideErrorsBeforeTouched={true}
          />
        </FixedSpaceColumn>
        <AsyncButton
          primary
          data-qa="login"
          type="submit"
          text={t.link}
          disabled={!form.isValid()}
          onClick={() => {
            const { username, password } = form.value()
            return authWeakLoginResult(username, password)
          }}
          onSuccess={() => window.location.replace(nextUrl ?? '/')}
          onFailure={(error) => {
            if (error.statusCode === 429) {
              setRateLimitError(true)
            }
          }}
        />
        <ExpandingInfo info={t.forgotPasswordInfo}>
          {t.forgotPassword}
        </ExpandingInfo>
        <ExpandingInfo info={t.noUsernameInfo}>{t.noUsername}</ExpandingInfo>
      </FixedSpaceColumn>
    </form>
  )
})
