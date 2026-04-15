// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment } from 'react'
import styled from 'styled-components'
import { Link, Redirect, useLocation, useSearchParams } from 'wouter'

import { useQueryResult } from 'lib-common/query'
import Main from 'lib-components/atoms/Main'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights, H1, H2, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faSignIn, farMap, farUser } from 'lib-icons'

import Footer from '../Footer'
import { useUser } from '../auth/state'
import { useIsStandalone } from '../hooks/useIsStandalone'
import { useLang, useTranslation } from '../localization'
import { getStrongLoginUri, getWeakLoginUri } from '../navigation/const'
import { PasskeyLoginButton } from '../passkey/PasskeyLoginButton'
import { PwaInstallButton } from '../pwa/PwaInstallButton'
import useTitle from '../useTitle'

import { systemNotificationsQuery } from './queries'

export default React.memo(function LoginPage() {
  const i18n = useTranslation()
  useTitle(i18n, i18n.common.title)
  const [lang] = useLang()
  const user = useUser()
  const standalone = useIsStandalone()

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')
  const [, navigate] = useLocation()

  const systemNotifications = useQueryResult(systemNotificationsQuery())

  if (user) {
    return <Redirect to="/" replace />
  }

  const weakLoginContent = (
    <>
      <H2 $noMargin $hyphenate>
        {i18n.loginPage.login.title}
      </H2>
      <Gap $size="m" />
      <LinkButton
        $style="secondary"
        href={getWeakLoginUri(unvalidatedNextPath ?? '/')}
        onClick={(e) => {
          e.preventDefault()
          navigate(getWeakLoginUri(unvalidatedNextPath ?? '/'))
        }}
        data-qa="weak-login"
      >
        <FontAwesomeIcon icon={farUser} />
        <Gap $size="xs" $horizontal />
        {i18n.loginPage.login.link}
      </LinkButton>
    </>
  )

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
              <PwaInstallButton />
            </MobileOnly>
          </ContentArea>
          <ContentArea $opaque>
            <PasskeyLoginButton nextUrl={unvalidatedNextPath} />
          </ContentArea>
          <ContentArea $opaque>
            <ExpandingInfo
              info={
                <>
                  <P $noMargin>{i18n.loginPage.applying.paragraph}</P>
                  <UnorderedList>
                    {i18n.loginPage.applying.infoBullets.map((item, index) => (
                      <li key={`bullet-item-${index}`}>{item}</li>
                    ))}
                  </UnorderedList>
                  <P $noMargin>{i18n.loginPage.applying.infoBoxText}</P>
                </>
              }
            >
              <H2 $noMargin>{i18n.loginPage.applying.title}</H2>
            </ExpandingInfo>
            <Gap $size="m" />
            <LinkButton
              $style="secondary"
              href={getStrongLoginUri(unvalidatedNextPath ?? '/')}
              data-qa="strong-login"
            >
              <FontAwesomeIcon icon={faSignIn} />
              <Gap $size="xs" $horizontal />
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
          <ContentArea $opaque>
            {standalone ? (
              <details data-qa="more-options">
                <summary>
                  {i18n.loginPage.login.passkey.moreOptionsDisclosure}
                </summary>
                <Gap $size="s" />
                {weakLoginContent}
              </details>
            ) : (
              weakLoginContent
            )}
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
