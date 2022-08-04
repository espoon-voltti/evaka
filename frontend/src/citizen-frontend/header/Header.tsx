// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sumBy from 'lodash/sumBy'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { ChildrenContext } from 'citizen-frontend/children/state'
import { childConsentTypes } from 'lib-common/generated/api-types/children'
import { desktopMin } from 'lib-components/breakpoints'
import colors from 'lib-customizations/common'

import { useUser } from '../auth/state'
import {
  ChildDocumentsContext,
  ChildDocumentsState
} from '../child-documents/state'
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
    unreadPedagogicalDocumentsCount,
    unreadVasuDocumentsCount,
    refreshUnreadPedagogicalDocumentsCount,
    refreshUnreadVasuDocumentsCount
  } = useContext<ChildDocumentsState>(ChildDocumentsContext)

  const {
    unreadAssistanceNeedDecisionCounts,
    refreshUnreadAssistanceNeedDecisionCounts,
    childConsents
  } = useContext(ChildrenContext)

  useEffect(() => {
    if (user) {
      refreshUnreadPedagogicalDocumentsCount()
      refreshUnreadVasuDocumentsCount()
      refreshUnreadAssistanceNeedDecisionCounts()
    }
  }, [refreshUnreadVasuDocumentsCount, refreshUnreadPedagogicalDocumentsCount, refreshUnreadAssistanceNeedDecisionCounts, user])

  const location = useLocation()
  const hideLoginButton = location.pathname === '/login'

  const unreadDocumentCount =
    (unreadPedagogicalDocumentsCount || 0) + (unreadVasuDocumentsCount || 0)

  const unreadChildrenCount = useMemo(
    () => sumBy(unreadAssistanceNeedDecisionCounts, ({ count }) => count),
    [unreadAssistanceNeedDecisionCounts]
  )

  const unconsentedCount = useMemo(
    () =>
      Object.values(childConsents.getOrElse({})).reduce(
        (prev, n) =>
          prev +
          childConsentTypes.filter(
            (type) => !n.some((consent) => consent.type === type)
          ).length,
        0
      ),
    [childConsents]
  )

  return (
    <HeaderContainer showMenu={showMenu} aria-hidden={props.ariaHidden}>
      <CityLogo />
      <EvakaLogo />
      <DesktopNav
        unreadMessagesCount={unreadMessagesCount ?? 0}
        unreadChildDocuments={unreadDocumentCount}
        unreadChildren={unreadChildrenCount + unconsentedCount}
        hideLoginButton={hideLoginButton}
      />
      <MobileNav
        showMenu={showMenu}
        setShowMenu={setShowMenu}
        unreadMessagesCount={unreadMessagesCount ?? 0}
        unreadChildDocumentsCount={unreadDocumentCount}
        unreadChildrenCount={unreadChildrenCount + unconsentedCount}
        hideLoginButton={hideLoginButton}
      />
    </HeaderContainer>
  )
})

const HeaderContainer = styled.header<{ showMenu: boolean }>`
  z-index: 25;
  color: ${colors.grayscale.g0};
  background-color: ${colors.main.m2};
  display: grid;
  grid: minmax(60px, min-content) / repeat(3, minmax(100px, 1fr));
  height: ${headerHeightMobile}px;
  width: 100%;

  @media (min-width: ${desktopMin}) {
    grid: minmax(${headerHeightDesktop}px, min-content) / max-content max-content auto;
    height: ${headerHeightDesktop}px;
  }
`
