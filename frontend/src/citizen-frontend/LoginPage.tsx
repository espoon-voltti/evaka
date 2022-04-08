// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useState } from 'react'
import { Redirect } from 'react-router-dom'
import styled from 'styled-components'

import LinkWrapperInlineBlock from 'lib-components/atoms/LinkWrapperInlineBlock'
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
import { fontWeights, H1, H2, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { farMap } from 'lib-icons'

import Footer from './Footer'
import { useUser } from './auth/state'
import { getStrongLoginUri, getWeakLoginUri } from './header/const'
import { useTranslation } from './localization'

export default React.memo(function LoginPage() {
  const i18n = useTranslation()
  const user = useUser()

  const [showInfoBoxText, setShowInfoBoxText] = useState(false)

  if (user) {
    return <Redirect to="/" />
  }

  return (
    <>
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
          <ContentArea opaque>
            <H2 noMargin>{i18n.loginPage.login.title}</H2>
            <Gap size="m" />
            <P noMargin>{i18n.loginPage.login.paragraph}</P>
            <Gap size="s" />
            <LinkButton href={getWeakLoginUri('/')} data-qa="weak-login">
              {i18n.loginPage.login.link}
            </LinkButton>
          </ContentArea>
          <ContentArea opaque>
            <H2 noMargin>{i18n.loginPage.applying.title}</H2>
            <Gap size="m" />
            <FlexRow>
              <P noMargin>{i18n.loginPage.applying.paragraph}</P>
              <InfoButton
                aria-label={i18n.common.openExpandingInfo}
                onClick={() => setShowInfoBoxText(!showInfoBoxText)}
              />
            </FlexRow>
            {showInfoBoxText && (
              <ExpandingInfoBox
                info={i18n.loginPage.applying.infoBoxText}
                close={() => setShowInfoBoxText(false)}
              />
            )}
            <ul>
              {i18n.loginPage.applying.infoBullets.map((item, index) => (
                <li key={`bullet-item-${index}`}>{item}</li>
              ))}
            </ul>
            <Gap size="s" />
            <LinkButton
              href={getStrongLoginUri('/applying')}
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
    </>
  )
})

const MapLink = styled(LinkWrapperInlineBlock)`
  font-weight: ${fontWeights.semibold};
`

const FlexRow = styled.div`
  display: flex;
  gap: ${defaultMargins.xs};
`
