// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link, Redirect, useLocation, useSearchParams } from 'wouter'

import { useQueryResult } from 'lib-common/query'
import { parseUrlWithOrigin } from 'lib-common/utils/parse-url-with-origin'
import useLocalStorage from 'lib-common/utils/useLocalStorage'
import { StaticChip } from 'lib-components/atoms/Chip'
import Main from 'lib-components/atoms/Main'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { Button } from 'lib-components/atoms/buttons/Button'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import Container, {
  CollapsibleContentArea,
  ContentArea
} from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
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
import colors from 'lib-customizations/common'
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
import { systemNotificationsQuery } from './queries'

const ParagraphInfoButton = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`

export default React.memo(function LoginPage() {
  const i18n = useTranslation()
  useTitle(i18n, i18n.common.title)
  const [lang] = useLang()
  const user = useUser()

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')

  const [showInfoBoxText1, setShowInfoBoxText1] = useState(false)
  const [showInfoBoxText2, setShowInfoBoxText2] = useState(false)

  const systemNotifications = useQueryResult(systemNotificationsQuery())

  if (user) {
    return <Redirect to="/" replace />
  }

  return (
    <Main>
      <TabletAndDesktop>
        <Gap $size="L" />
      </TabletAndDesktop>
      <MobileOnly>
        <Gap $size="xs" />
      </MobileOnly>
      <Container>
        <FixedSpaceColumn $spacing="s">
          <ContentArea $opaque>
            <H1 $noMargin $hyphenate>
              {i18n.loginPage.title}
            </H1>
            {systemNotifications.isSuccess &&
              systemNotifications.value.notification && (
                <>
                  <Gap $size="m" />
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
                </>
              )}
            <MobileOnly>
              <Gap $size="m" />
              <AddToHomeScreenInstructions />
            </MobileOnly>
          </ContentArea>
          <ContentArea $opaque>
            <H2 $noMargin $hyphenate>
              {i18n.loginPage.login.title}
            </H2>
            <Gap $size="m" />
            <P $noMargin>
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
            <Gap $size="s" />
            <WeakLoginMethods unvalidatedNextPath={unvalidatedNextPath} />
          </ContentArea>
          <ContentArea $opaque>
            <H2 $noMargin>{i18n.loginPage.applying.title}</H2>
            <Gap $size="m" />
            <P $noMargin>
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
            <UnorderedList>
              {i18n.loginPage.applying.infoBullets.map((item, index) => (
                <li key={`bullet-item-${index}`}>{item}</li>
              ))}
            </UnorderedList>
            <Gap $size="s" />
            <LinkButton
              href={getStrongLoginUri(unvalidatedNextPath ?? '/')}
              data-qa="strong-login"
            >
              {i18n.loginPage.applying.link}
            </LinkButton>
            <Gap $size="m" />
            <P $noMargin>{i18n.loginPage.applying.mapText}</P>
            <Gap $size="xs" />
            <MapLink to="/map">
              <FontAwesomeIcon icon={farMap} />
              <Gap $size="xs" $horizontal />
              {i18n.loginPage.applying.mapLink}
            </MapLink>
          </ContentArea>
        </FixedSpaceColumn>
      </Container>
      <Footer />
    </Main>
  )
})

const MapLink = styled(Link)`
  text-decoration: none;
  display: inline-block;
  font-weight: ${fontWeights.semibold};
`

const WeakLoginMethods = React.memo(function WeakLoginMethods({
  unvalidatedNextPath
}: {
  unvalidatedNextPath: string | null
}) {
  const i18n = useTranslation()
  const t = i18n.loginPage.login
  const [, navigate] = useLocation()

  const passkeysEnabled = passkeysSupported()
  const lastLoginMethod = useLastLoginMethod()
  const [passkeyFailed, setPasskeyFailed] = useState(false)

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
  }, [nextUrl])

  const emailButton = (isPrimary: boolean) => (
    <LoginMethodRow key="email">
      <LinkButton
        href={getWeakLoginUri(unvalidatedNextPath ?? '/')}
        onClick={(e) => {
          e.preventDefault()
          navigate(getWeakLoginUri(unvalidatedNextPath ?? '/'))
        }}
        $style={isPrimary ? 'primary' : 'secondary'}
        data-qa="weak-login"
      >
        {t.link}
      </LinkButton>
      {isPrimary && lastLoginMethod === 'email' && <UsedLastTag />}
    </LoginMethodRow>
  )
  const passkeyButton = (isPrimary: boolean) => (
    <LoginMethodRow key="passkey">
      <Button
        primary={isPrimary}
        text={t.passkeyLink}
        onClick={() => void loginWithPasskey()}
        data-qa="passkey-login"
      />
      {isPrimary && lastLoginMethod === 'passkey' && <UsedLastTag />}
    </LoginMethodRow>
  )

  const passkeyFirst = passkeysEnabled && lastLoginMethod === 'passkey'
  return (
    <FixedSpaceColumn $spacing="s" $alignItems="flex-start">
      {passkeyFirst && passkeyButton(true)}
      {emailButton(!passkeyFirst)}
      {passkeysEnabled && !passkeyFirst && passkeyButton(false)}
      {passkeyFailed && (
        <AlertBox
          message={t.passkeyError}
          noMargin
          data-qa="passkey-login-error"
        />
      )}
    </FixedSpaceColumn>
  )
})

const UsedLastTag = React.memo(function UsedLastTag() {
  const i18n = useTranslation()
  return (
    <StaticChip $color={colors.main.m1} data-qa="used-last-tag">
      {i18n.loginPage.login.usedLast}
    </StaticChip>
  )
})

const LoginMethodRow = styled(FixedSpaceRow).attrs({
  $spacing: 's',
  $alignItems: 'center'
})``

const AddToHomeScreenInstructions = React.memo(
  function AddToHomeScreenInstructions() {
    const i18n = useTranslation()

    const [open, setOpen] = useLocalStorage(
      'add-to-homescreen-instructions',
      'open',
      (value) => value === 'open' || value === 'closed'
    )
    const toggleOpen = useCallback(
      () => setOpen((open) => (open === 'open' ? 'closed' : 'open')),
      [setOpen]
    )

    const [instructions, setInstructions] = useState<'ios' | 'android' | null>(
      null
    )
    const toggle = (which: 'ios' | 'android') => {
      setInstructions((current) => (current === which ? null : which))
    }

    return (
      <CollapsibleContentArea
        open={open === 'open'}
        toggleOpen={toggleOpen}
        $opaque={false}
        title={i18n.loginPage.addToHomeScreen.title}
        $paddingHorizontal="0"
        $paddingVertical="0"
      >
        <P $noMargin>{i18n.loginPage.addToHomeScreen.subTitle}</P>
        <Gap $size="s" />
        <UnorderedList>
          <li>
            {i18n.loginPage.addToHomeScreen.ios}{' '}
            <ParagraphInfoButton
              onClick={() => toggle('ios')}
              aria-label={i18n.common.openExpandingInfo}
            />
          </li>
          <li>
            {i18n.loginPage.addToHomeScreen.android}{' '}
            <ParagraphInfoButton
              onClick={() => toggle('android')}
              aria-label={i18n.common.openExpandingInfo}
            />
          </li>
        </UnorderedList>
        {instructions && (
          <ExpandingInfoBox
            info={i18n.loginPage.addToHomeScreen.instructions[instructions]}
            close={() => setInstructions(null)}
          />
        )}
      </CollapsibleContentArea>
    )
  }
)
