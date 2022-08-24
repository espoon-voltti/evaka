// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'
import sumBy from 'lodash/sumBy'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { ApplicationsContext } from 'citizen-frontend/applications/state'
import { ChildrenContext } from 'citizen-frontend/children/state'
import { desktopMin } from 'lib-components/breakpoints'
import colors from 'lib-customizations/common'

import { useUser } from '../auth/state'
import { MessageContext, MessagePageState } from '../messages/state'

import CityLogo from './CityLogo'
import DesktopNav from './DesktopNav'
import EvakaLogo from './EvakaLogo'
import MobileNav from './MobileNav'
import { headerHeightDesktop, headerHeightMobile } from './const'

export default React.memo(function Header(props: { ariaHidden: boolean }) {
  const [showMenu, setShowMenu] = useState(false)
  const user = useUser()

  const { unreadMessagesCount, refreshUnreadMessagesCount } =
    useContext<MessagePageState>(MessageContext)

  useEffect(() => {
    if (user) refreshUnreadMessagesCount()
  }, [refreshUnreadMessagesCount, user])

  const {
    unreadAssistanceNeedDecisionCounts,
    refreshUnreadAssistanceNeedDecisionCounts,
    childConsentNotifications,
    unreadPedagogicalDocumentsCount,
    unreadVasuDocumentsCount,
    refreshUnreadPedagogicalDocumentsCount,
    refreshUnreadVasuDocumentsCount
  } = useContext(ChildrenContext)

  const { waitingConfirmationCount, refreshWaitingConfirmationCount } =
    useContext(ApplicationsContext)

  useEffect(() => {
    if (user) {
      refreshUnreadPedagogicalDocumentsCount()
      refreshUnreadVasuDocumentsCount()
      refreshUnreadAssistanceNeedDecisionCounts()
      refreshWaitingConfirmationCount()
    }
  }, [refreshUnreadVasuDocumentsCount, refreshUnreadPedagogicalDocumentsCount, refreshUnreadAssistanceNeedDecisionCounts, refreshWaitingConfirmationCount, user])

  const location = useLocation()
  const hideLoginButton = location.pathname === '/login'

  const unreadAssistanceNeedDecisionCount = useMemo(
    () => sumBy(unreadAssistanceNeedDecisionCounts, ({ count }) => count),
    [unreadAssistanceNeedDecisionCounts]
  )

  const unreadChildDocumentsCount = useMemo(
    () =>
      sum(Object.values(unreadPedagogicalDocumentsCount ?? {})) +
      sum(Object.values(unreadVasuDocumentsCount ?? {})),
    [unreadPedagogicalDocumentsCount, unreadVasuDocumentsCount]
  )

  const unreadChildrenCount =
    unreadAssistanceNeedDecisionCount +
    childConsentNotifications +
    unreadChildDocumentsCount

  return (
    <HeaderContainer showMenu={showMenu} aria-hidden={props.ariaHidden}>
      <CityLogo />
      <EvakaLogo />
      <DesktopNav
        unreadMessagesCount={unreadMessagesCount ?? 0}
        unreadChildren={unreadChildrenCount}
        unreadApplications={waitingConfirmationCount}
        hideLoginButton={hideLoginButton}
      />
      <MobileNav
        showMenu={showMenu}
        setShowMenu={setShowMenu}
        unreadMessagesCount={unreadMessagesCount ?? 0}
        unreadChildrenCount={unreadChildrenCount}
        unreadApplications={waitingConfirmationCount}
        hideLoginButton={hideLoginButton}
      />
    </HeaderContainer>
  )
})

const HeaderContainer = styled.header<{ showMenu: boolean }>`
  z-index: 25;
  color: ${colors.grayscale.g100};
  background-color: transparent;
  display: grid;
  grid: minmax(60px, min-content) / repeat(3, minmax(100px, 1fr));
  height: ${headerHeightMobile}px;
  width: 100%;
  position: sticky;
  top: 0;

  @media (min-width: ${desktopMin}) {
    grid: minmax(${headerHeightDesktop}px, min-content) / max-content max-content auto;
    height: ${headerHeightDesktop}px;
  }
`
