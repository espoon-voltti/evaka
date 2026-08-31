// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link, Redirect, useLocation, useSearchParams } from 'wouter'

import { useQueryResult } from 'lib-common/query'
import { parseUrlWithOrigin } from 'lib-common/utils/parse-url-with-origin'
import Main from 'lib-components/atoms/Main'
import { desktopMin } from 'lib-components/breakpoints'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { H1, H2, P, Title } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { farMap } from 'lib-icons'

import Footer from '../Footer'
import { authPasskeyLogin, passkeysSupported } from '../auth/passkeys'
import { useUser } from '../auth/state'
import { useLang, useTranslation } from '../localization'
import { getStrongLoginUri, getWeakLoginUri } from '../navigation/const'
import useTitle from '../useTitle'

import {
  rememberLastLoginMethod,
  useLastLoginMethod
} from './last-login-method'
import {
  HelpLink,
  LinkRow,
  LoginCard,
  LoginColumns,
  LoginContainer,
  rowLinkStyles,
  TopGap,
  TwoLineButton,
  UsedLastChip,
  WideLinkButton
} from './layout'
import { systemNotificationsQuery } from './queries'

export default React.memo(function LoginPage() {
  const i18n = useTranslation()
  useTitle(i18n, i18n.common.title)
  const [lang] = useLang()
  const user = useUser()

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')

  const systemNotifications = useQueryResult(systemNotificationsQuery())
  const [passkeyFailed, setPasskeyFailed] = useState(false)

  if (user) {
    return <Redirect to="/" replace />
  }

  return (
    <Main>
      <TopGap />
      <LoginContainer>
        <LoginCard>
          {systemNotifications.isSuccess &&
            systemNotifications.value.notification && (
              <>
                <AlertBox
                  title={i18n.loginPage.systemNotification}
                  message={
                    <div>
                      {(lang === 'sv'
                        ? systemNotifications.value.notification.textSv
                        : lang === 'en'
                          ? systemNotifications.value.notification.textEn
                          : systemNotifications.value.notification.text
                      )
                        .split('\n')
                        .map((line, index) => (
                          <Fragment key={index}>
                            {line}
                            <br />
                          </Fragment>
                        ))}
                    </div>
                  }
                  wide
                  noMargin
                  data-qa="system-notification"
                />
                <Gap $size="m" />
              </>
            )}
          <H1 $noMargin $hyphenate>
            {i18n.loginPage.welcomeTitle}
          </H1>
          <Subtitle>{i18n.loginPage.title}</Subtitle>
          <Gap $size="XXL" />
          {passkeyFailed && (
            <>
              <InfoBox
                message={i18n.loginPage.login.passkeyError(
                  getStrongLoginUri('/personal-details')
                )}
                darkBackground
                wide
                noMargin
                data-qa="passkey-login-error"
              />
              <Gap $size="XXL" />
            </>
          )}
          <LoginColumns>
            <section>
              <H2 $noMargin $hyphenate>
                {i18n.loginPage.login.title}
              </H2>
              <Gap $size="m" />
              <WeakLoginMethods
                unvalidatedNextPath={unvalidatedNextPath}
                setPasskeyFailed={setPasskeyFailed}
              />
            </section>
            <section>
              <H2 $noMargin>{i18n.loginPage.applying.title}</H2>
              <Gap $size="m" />
              <Paragraphs>
                <P $noMargin>{i18n.loginPage.applying.paragraph}</P>
                <P $noMargin>{i18n.loginPage.applying.infoBoxText}</P>
              </Paragraphs>
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
        <LinkRow>
          <MapLink to="/map">
            <FontAwesomeIcon icon={farMap} />
            <Gap $size="xs" $horizontal />
            {i18n.loginPage.applying.mapLink}
          </MapLink>
          <HelpLink />
        </LinkRow>
      </LoginContainer>
      <Footer narrow />
    </Main>
  )
})

const Subtitle = styled(Title)`
  display: block;
  margin-top: ${defaultMargins.xxs};
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-size: 16px;
  line-height: 24px;

  @media (min-width: ${desktopMin}) {
    font-size: 20px;
  }
`

const Paragraphs = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`

const MapLink = styled(Link)`
  ${rowLinkStyles}
`

const WeakLoginMethods = React.memo(function WeakLoginMethods({
  unvalidatedNextPath,
  setPasskeyFailed
}: {
  unvalidatedNextPath: string | null
  setPasskeyFailed: (failed: boolean) => void
}) {
  const i18n = useTranslation()
  const t = i18n.loginPage.login
  const [, navigate] = useLocation()

  const passkeysEnabled = passkeysSupported()
  const lastLoginMethod = useLastLoginMethod()

  const nextUrl = useMemo(
    () =>
      unvalidatedNextPath
        ? parseUrlWithOrigin(window.location, unvalidatedNextPath)
        : undefined,
    [unvalidatedNextPath]
  )

  const loginWithPasskey = useCallback(async () => {
    setPasskeyFailed(false)
    const result = await authPasskeyLogin()
    if (result === 'success') {
      rememberLastLoginMethod('passkey')
      window.location.replace(nextUrl ?? '/')
    } else {
      setPasskeyFailed(true)
    }
  }, [nextUrl, setPasskeyFailed])

  const emailButton = (isPrimary: boolean) => (
    <LoginMethod key="email">
      {isPrimary && lastLoginMethod === 'email' && <UsedLastTag />}
      <WideLinkButton
        href={getWeakLoginUri(unvalidatedNextPath ?? '/')}
        onClick={(e) => {
          e.preventDefault()
          navigate(getWeakLoginUri(unvalidatedNextPath ?? '/'))
        }}
        $style={isPrimary ? 'primary' : 'secondary'}
        data-qa="weak-login"
      >
        {t.emailLink}
      </WideLinkButton>
    </LoginMethod>
  )
  const passkeyButton = (isPrimary: boolean) => (
    <LoginMethod key="passkey">
      {isPrimary && lastLoginMethod === 'passkey' && <UsedLastTag />}
      <TwoLineButton
        label={t.passkeyLink}
        descriptionDesktop={t.passkeyDescriptionDesktop}
        descriptionMobile={t.passkeyDescriptionMobile}
        primary={isPrimary}
        onClick={() => void loginWithPasskey()}
        data-qa="passkey-login"
      />
    </LoginMethod>
  )

  const passkeyFirst = passkeysEnabled && lastLoginMethod === 'passkey'
  return (
    <LoginMethodList>
      {passkeyFirst && passkeyButton(true)}
      {emailButton(!passkeyFirst)}
      {passkeysEnabled && !passkeyFirst && passkeyButton(false)}
    </LoginMethodList>
  )
})

const UsedLastTag = React.memo(function UsedLastTag() {
  const i18n = useTranslation()
  return (
    <UsedLastChip data-qa="used-last-tag">
      {i18n.loginPage.login.usedLast}
    </UsedLastChip>
  )
})

const LoginMethodList = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`

const LoginMethod = styled.div`
  display: flex;
  flex-direction: column;
`
