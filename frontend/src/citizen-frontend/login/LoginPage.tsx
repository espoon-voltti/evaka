// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import Main from 'lib-components/atoms/Main'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
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
import { farMap } from 'lib-icons'

import Footer from '../Footer'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUriWithPath, getWeakLoginUri } from '../navigation/const'

import { systemNotificationsQuery } from './queries'

const ParagraphInfoButton = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`

/**
 * Ensures that the redirect URL will not contain any host
 * information, only the path/search params/hash.
 */
const getSafeNextPath = (nextParam: string | null) => {
  if (nextParam === null) {
    return null
  }

  const url = new URL(nextParam, window.location.origin)

  return `${url.pathname}${url.search}${url.hash}`
}

export default React.memo(function LoginPage() {
  const i18n = useTranslation()
  const user = useUser()

  const [searchParams] = useSearchParams()

  const nextPath = useMemo(
    () => getSafeNextPath(searchParams.get('next')),
    [searchParams]
  )

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
              href={getWeakLoginUri(nextPath ?? '/')}
              data-qa="weak-login"
            >
              {i18n.loginPage.login.link}
            </LinkButton>
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
              href={getStrongLoginUriWithPath(nextPath ?? '/applications')}
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

const MapLink = styled(Link)`
  text-decoration: none;
  display: inline-block;
  font-weight: ${fontWeights.semibold};
`
