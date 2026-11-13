// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled, { css } from 'styled-components'
import { useLocation } from 'wouter'

import { useQuery } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  desktopMin,
  desktopMinPx,
  tabletMin,
  zoomedMobileMax
} from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'

import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { unreadMessagesCountQuery } from '../messages/queries'
import { loginPageWidth } from '../page-widths'

import CityLogo from './CityLogo'
import DesktopNav from './DesktopNav'
import EvakaLogo from './EvakaLogo'
import { headerHeightDesktop, headerHeightMobile } from './const'
import { MobileHeaderBackButtonContext } from './mobile-header-back-button'
import { LanguageMenu } from './shared-components'
import { useUnreadDecisions } from './utils'

export default React.memo(function Header() {
  const loggedIn = useUser() !== undefined

  const { data: unreadMessagesCount } = useQuery(unreadMessagesCountQuery(), {
    enabled: loggedIn
  })

  const unreadDecisions = useUnreadDecisions()

  const [path] = useLocation()
  const isLoginPage = path === '/login' || path === '/login/form'

  const t = useTranslation()
  const { backButton } = useContext(MobileHeaderBackButtonContext)

  return (
    <>
      <HeaderContainer $narrow={isLoginPage}>
        {backButton && (
          <MobileBackButton
            appearance="inline"
            icon={faArrowLeft}
            text={backButton.text}
            aria-label={`${t.common.return}: ${backButton.text}`}
            onClick={backButton.onClick}
            data-qa="mobile-header-back-button"
          />
        )}
        <Logos $hiddenOnMobile={backButton !== undefined}>
          <CityLogo />
          <EvakaLogo />
        </Logos>
        {isLoginPage && (
          <MobileOnly>
            <LanguageMenu useShortLanguageLabel alignRight />
          </MobileOnly>
        )}
        <DesktopNav
          unreadMessagesCount={unreadMessagesCount ?? 0}
          unreadDecisions={unreadDecisions}
          hideLoginButton={isLoginPage}
        />
      </HeaderContainer>
    </>
  )
})

const wideWidthStyles = css`
  @media screen and (min-width: 1152px) {
    max-width: 1152px;
    width: 1152px;
  }
  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }
`

const narrowWidthStyles = css`
  @media (min-width: ${desktopMin}) {
    max-width: ${loginPageWidth};
    width: ${loginPageWidth};
  }
`

const HeaderContainer = styled.header<{ $narrow: boolean }>`
  z-index: 25;
  color: ${colors.grayscale.g100};
  background-color: ${colors.grayscale.g0};
  display: grid;
  grid: minmax(60px, min-content) / repeat(3, minmax(100px, 1fr));
  height: ${headerHeightMobile}px;
  width: 100%;
  margin: 0 auto;
  position: sticky;
  top: 0;
  box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.15);

  @media (max-width: ${zoomedMobileMax}) {
    grid: minmax(60px, min-content) / repeat(3, minmax(60px, 1fr));
  }

  @media (min-width: ${desktopMin}) {
    position: static;
    grid: minmax(${headerHeightDesktop}px, min-content) / max-content max-content auto;
    height: ${headerHeightDesktop}px;
    background-color: transparent;
    box-shadow: none;
  }

  ${(p) => (p.$narrow ? narrowWidthStyles : wideWidthStyles)}

  @media print {
    display: none;
  }
`

const MobileOnly = styled.div`
  display: none;

  @media (max-width: ${desktopMinPx - 1}px) {
    display: block;
    padding: 4px;
  }
`

const MobileBackButton = styled(Button)`
  display: none;
  grid-column: 1 / -1;
  justify-content: flex-start;
  min-width: 0;
  padding: 0 ${defaultMargins.s};

  @media (max-width: ${tabletMin}) {
    display: flex;
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`

const Logos = styled.div<{ $hiddenOnMobile: boolean }>`
  display: contents;

  ${(p) =>
    p.$hiddenOnMobile &&
    css`
      @media (max-width: ${tabletMin}) {
        display: none;
      }
    `}
`
