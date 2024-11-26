// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { useQueryResult } from 'lib-common/query'
import { parseUrlWithOrigin } from 'lib-common/utils/parse-url-with-origin'
import Main from 'lib-components/atoms/Main'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights, H1, H2, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { farMap } from 'lib-icons'

import Footer from '../Footer'
import { authWeakLogin } from '../auth/api'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri, getWeakLoginUri } from '../navigation/const'

import { systemNotificationsQuery } from './queries'

const ParagraphInfoButton = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`

export default React.memo(function LoginPage() {
  const i18n = useTranslation()
  const user = useUser()

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')

  const [showInfoBoxText1, setShowInfoBoxText1] = useState(false)
  const [showInfoBoxText2, setShowInfoBoxText2] = useState(false)

  const systemNotifications = useQueryResult(systemNotificationsQuery())

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
          <ContentArea opaque>
            <H1 noMargin>{i18n.loginPage.title}</H1>
          </ContentArea>
          {systemNotifications.isSuccess &&
            systemNotifications.value.notification && (
              <ContentArea opaque>
                <AlertBox
                  title={i18n.loginPage.systemNotification}
                  message={systemNotifications.value.notification.text}
                  wide
                  noMargin
                  data-qa="system-notification"
                />
              </ContentArea>
            )}
          <ContentArea opaque>
            <H2 noMargin>{i18n.loginPage.login.title}</H2>
            <Gap size="m" />
            <P noMargin>
              {i18n.loginPage.login.paragraph}
              <ParagraphInfoButton
                aria-label={i18n.common.openExpandingInfo}
                onClick={() => setShowInfoBoxText1(!showInfoBoxText1)}
                open={showInfoBoxText1}
              />
            </P>
            {showInfoBoxText1 && (
              <ExpandingInfoBox
                info={i18n.loginPage.login.infoBoxText}
                close={() => setShowInfoBoxText1(false)}
              />
            )}
            <Gap size="s" />
            <LinkButton
              href={getWeakLoginUri(unvalidatedNextPath ?? '/')}
              data-qa="weak-login"
            >
              {i18n.loginPage.login.link}
            </LinkButton>
            {featureFlags.weakLogin && (
              <WeakLoginForm unvalidatedNextPath={unvalidatedNextPath} />
            )}
          </ContentArea>
          <ContentArea opaque>
            <H2 noMargin>{i18n.loginPage.applying.title}</H2>
            <Gap size="m" />
            <P noMargin>
              {i18n.loginPage.applying.paragraph}
              <ParagraphInfoButton
                aria-label={i18n.common.openExpandingInfo}
                onClick={() => setShowInfoBoxText2(!showInfoBoxText2)}
                open={showInfoBoxText2}
              />
            </P>
            {showInfoBoxText2 && (
              <ExpandingInfoBox
                info={i18n.loginPage.applying.infoBoxText}
                close={() => setShowInfoBoxText2(false)}
              />
            )}
            <ul>
              {i18n.loginPage.applying.infoBullets.map((item, index) => (
                <li key={`bullet-item-${index}`}>{item}</li>
              ))}
            </ul>
            <Gap size="s" />
            <LinkButton
              href={getStrongLoginUri(unvalidatedNextPath ?? '/applications')}
              data-qa="strong-login"
            >
              {i18n.loginPage.applying.link}
            </LinkButton>
            <Gap size="m" />
            <P noMargin>{i18n.loginPage.applying.mapText}</P>
            <Gap size="xs" />
            <MapLink to="/map">
              <FontAwesomeIcon icon={farMap} />
              <Gap size="xs" horizontal />
              {i18n.loginPage.applying.mapLink}
            </MapLink>
          </ContentArea>
        </FixedSpaceColumn>
      </Container>
      <Footer />
    </Main>
  )
})

const weakLoginForm = validated(
  object({
    username: string(),
    password: string()
  }),
  (form) => {
    if (form.username.length === 0 || form.password.length === 0) {
      return 'required'
    }
    return undefined
  }
)

const authWeakLoginResult = wrapResult(authWeakLogin)

const WeakLoginForm = React.memo(function WeakLogin({
  unvalidatedNextPath
}: {
  unvalidatedNextPath: string | null
}) {
  const i18n = useTranslation()
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
    <>
      <Gap size="m" />
      <form action="" onSubmit={(e) => e.preventDefault()}>
        <FixedSpaceColumn spacing="xs">
          {rateLimitError && (
            <AlertBox message={i18n.loginPage.login.rateLimitError} />
          )}
          <InputFieldF
            autoComplete="email"
            bind={username}
            placeholder={i18n.loginPage.login.email}
            width="L"
            hideErrorsBeforeTouched={true}
          />
          <InputFieldF
            autoComplete="current-password"
            bind={password}
            type="password"
            placeholder={i18n.loginPage.login.password}
            width="L"
            hideErrorsBeforeTouched={true}
          />
          <AsyncButton
            primary
            type="submit"
            text={i18n.loginPage.login.link}
            disabled={!form.isValid()}
            onClick={() =>
              authWeakLoginResult(form.state.username, form.state.password)
            }
            onSuccess={() => window.location.replace(nextUrl ?? '/')}
            onFailure={(error) => {
              if (error.statusCode === 429) {
                setRateLimitError(true)
              }
            }}
          />
        </FixedSpaceColumn>
      </form>
    </>
  )
})

const MapLink = styled(Link)`
  text-decoration: none;
  display: inline-block;
  font-weight: ${fontWeights.semibold};
`
