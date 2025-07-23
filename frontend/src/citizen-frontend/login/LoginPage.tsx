// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useCallback, useState } from 'react'
import styled from 'styled-components'
import { Link, Redirect, useLocation, useSearchParams } from 'wouter'

import { useQueryResult } from 'lib-common/query'
import useLocalStorage from 'lib-common/utils/useLocalStorage'
import Main from 'lib-components/atoms/Main'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import Container, {
  CollapsibleContentArea,
  ContentArea
} from 'lib-components/layout/Container'
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
import { useLang, useTranslation } from '../localization'
import { getStrongLoginUri, getWeakLoginUri } from '../navigation/const'
import useTitle from '../useTitle'

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
  const [, navigate] = useLocation()

  const [showInfoBoxText1, setShowInfoBoxText1] = useState(false)
  const [showInfoBoxText2, setShowInfoBoxText2] = useState(false)

  const systemNotifications = useQueryResult(systemNotificationsQuery())

  if (user) {
    return <Redirect to="/" replace />
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
            {systemNotifications.isSuccess &&
              systemNotifications.value.notification && (
                <>
                  <Gap size="m" />
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
              <Gap size="m" />
              <AddToHomeScreenInstructions />
            </MobileOnly>
          </ContentArea>
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
              onClick={(e) => {
                e.preventDefault()
                navigate(getWeakLoginUri(unvalidatedNextPath ?? '/'))
              }}
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

const MapLink = styled(Link)`
  text-decoration: none;
  display: inline-block;
  font-weight: ${fontWeights.semibold};
`

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
        opaque={false}
        title={i18n.loginPage.addToHomeScreen.title}
        paddingHorizontal="0"
        paddingVertical="0"
      >
        <P noMargin>{i18n.loginPage.addToHomeScreen.subTitle}</P>
        <Gap size="s" />
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
