// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useCallback, useState } from 'react'
import styled from 'styled-components'
import { Link, Redirect, useLocation, useSearchParams } from 'wouter'

import { useQueryResult } from 'lib-common/query'
import Main from 'lib-components/atoms/Main'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import { tabletMin } from 'lib-components/breakpoints'
import Container from 'lib-components/layout/Container'
import { MobileOnly } from 'lib-components/layout/responsive-layout'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights, H1, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faExternalLink, farMap, farUser } from 'lib-icons'

import { useUser } from '../auth/state'
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

  const [searchParams] = useSearchParams()
  const unvalidatedNextPath = searchParams.get('next')
  const [, navigate] = useLocation()

  const systemNotifications = useQueryResult(systemNotificationsQuery())

  const [infoOpen, setInfoOpen] = useState(false)
  const toggleInfo = useCallback(() => setInfoOpen((v) => !v), [])
  const closeInfo = useCallback(() => setInfoOpen(false), [])

  if (user) {
    return <Redirect to="/" replace />
  }

  return (
    <Main>
      <Container>
        <LoginLayout>
          <Heading>
            <H1 $noMargin $hyphenate>
              {i18n.loginPage.welcomeHeadline}
            </H1>
            <Gap $size="xs" />
            <Subtitle>{i18n.loginPage.title}</Subtitle>
          </Heading>

          {systemNotifications.isSuccess &&
            systemNotifications.value.notification && (
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
            )}

          <MobileOnly>
            <PwaInstallButton />
          </MobileOnly>

          <PrimaryFrame>
            <PasskeyLoginButton nextUrl={unvalidatedNextPath} />
          </PrimaryFrame>

          <SecondaryRow>
            <LinkButton
              $style="secondary"
              href={getStrongLoginUri(unvalidatedNextPath ?? '/')}
              data-qa="strong-login"
            >
              <FontAwesomeIcon icon={faExternalLink} />
              <Gap $size="xs" $horizontal />
              {i18n.loginPage.applying.link}
            </LinkButton>
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
              {i18n.loginPage.login.title}
            </LinkButton>
          </SecondaryRow>

          <InfoToggleRow>
            <InfoButton
              aria-label={i18n.common.openExpandingInfo}
              onClick={toggleInfo}
              open={infoOpen}
              data-qa="login-info-toggle"
            />
          </InfoToggleRow>
          {infoOpen && (
            <ExpandingInfoBox
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
              close={closeInfo}
              width="full"
            />
          )}

          <MapLink to="/map">
            <FontAwesomeIcon icon={farMap} />
            <Gap $size="xs" $horizontal />
            {i18n.loginPage.applying.mapLink}
          </MapLink>
        </LoginLayout>
      </Container>
      <LoginFooter>
        <FooterLinks>
          {i18n.footer.privacyPolicyLink}
          <Link to="/accessibility">{i18n.footer.accessibilityStatement}</Link>
          {i18n.footer.sendFeedbackLink}
        </FooterLinks>
        <Copyright data-qa="footer-citylabel">
          {i18n.footer.cityLabel}
        </Copyright>
      </LoginFooter>
    </Main>
  )
})

const LoginLayout = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${defaultMargins.L};
  max-width: 32rem;
  margin: 0 auto;
  padding: ${defaultMargins.XL} ${defaultMargins.s};

  @media (min-width: ${tabletMin}) {
    padding: ${defaultMargins.XXL} 0 ${defaultMargins.XL};
  }
`

const Heading = styled.div`
  text-align: center;

  h1 {
    font-size: 2rem;
    letter-spacing: -0.01em;
  }
`

const Subtitle = styled.p`
  margin: 0;
  font-size: 1.125rem;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const PrimaryFrame = styled.div`
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${defaultMargins.xs};
  padding: ${defaultMargins.L} ${defaultMargins.m};
  border-radius: 16px;
  background: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  [data-qa='passkey-login-section'] {
    width: 100%;
    align-items: center;
  }

  [data-qa='passkey-login-button'] {
    width: 100%;
    min-height: 60px;
    font-size: 1.125rem;
    font-weight: ${fontWeights.semibold};
  }

  [data-qa='passkey-login-section'] p {
    margin: 0;
    text-align: center;
    color: ${(p) => p.theme.colors.grayscale.g70};
  }
`

const SecondaryRow = styled.div`
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: ${defaultMargins.s};

  > a {
    flex: 1 1 14rem;
    min-width: 0;
    white-space: normal;
    overflow-wrap: break-word;
    word-break: normal;
    text-align: center;
    line-height: 1.25rem;
    padding: ${defaultMargins.xs} ${defaultMargins.m};
    min-height: 56px;
    gap: ${defaultMargins.xs};

    svg {
      flex-shrink: 0;
    }
  }
`

const InfoToggleRow = styled.div`
  display: flex;
  justify-content: center;
`

const MapLink = styled(Link)`
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.main.m2};
`

const LoginFooter = styled.footer`
  margin-top: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${defaultMargins.xs};
  padding: ${defaultMargins.L} ${defaultMargins.s};
  font-size: 0.875rem;
`

const FooterLinks = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  row-gap: ${defaultMargins.xs};
  column-gap: ${defaultMargins.L};

  a {
    color: ${(p) => p.theme.colors.main.m2};
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
`

const Copyright = styled.p`
  margin: 0;
  color: ${(p) => p.theme.colors.grayscale.g70};
`
