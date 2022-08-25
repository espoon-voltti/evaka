// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { SelectionChip } from 'lib-components/atoms/Chip'
import { desktopMin } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faBars,
  faCalendar,
  faChild,
  faEnvelope,
  faLockAlt,
  farSignOut,
  farXmark,
  fasCalendar,
  fasChild,
  fasEnvelope
} from 'lib-icons'

import { ApplicationsContext } from '../applications/state'
import { UnwrapResult } from '../async-rendering'
import { AuthContext, User } from '../auth/state'
import { ChildrenContext } from '../children/state'
import { langs, useLang, useTranslation } from '../localization'
import { MessageContext } from '../messages/state'

import AttentionIndicator from './AttentionIndicator'
import { CircledChar, DropDownLink } from './DesktopNav'
import {
  getLogoutUri,
  headerHeightMobile,
  mobileBottomNavHeight
} from './const'

export default React.memo(function MobileNav() {
  const t = useTranslation()
  const { user } = useContext(AuthContext)
  const { unreadChildNotifications } = useContext(ChildrenContext)
  const { unreadMessagesCount } = useContext(MessageContext)
  const { waitingConfirmationCount: unreadDecisions } =
    useContext(ApplicationsContext)
  const [menuOpen, setMenuOpen] = useState(false)
  const toggleMenu = useCallback(() => setMenuOpen((open) => !open), [])
  const closeMenu = useCallback(() => setMenuOpen(false), [])

  if (!user.getOrElse(undefined)) {
    return null
  }

  return (
    <UnwrapResult result={user} loading={() => null}>
      {(user) =>
        user ? (
          <>
            <BottomBar>
              {user.accessibleFeatures.reservations && (
                <BottomBarLink
                  to="/calendar"
                  data-qa="nav-calendar"
                  text={t.header.nav.calendar}
                  icon={faCalendar}
                  activeIcon={fasCalendar}
                  showNotification={false}
                />
              )}
              {user.accessibleFeatures.messages && (
                <BottomBarLink
                  to="/messages"
                  data-qa="nav-messages"
                  text={t.header.nav.messages}
                  icon={faEnvelope}
                  activeIcon={fasEnvelope}
                  showNotification={(unreadMessagesCount ?? 0) > 0}
                />
              )}
              <BottomBarLink
                to="/children"
                data-qa="nav-children"
                text={t.header.nav.children}
                icon={faChild}
                activeIcon={fasChild}
                showNotification={unreadChildNotifications > 0}
              />
              <StyledButton $isActive={menuOpen} onClick={toggleMenu}>
                <FontAwesomeIcon icon={menuOpen ? farXmark : faBars} />
                {t.header.nav.subNavigationMenu}
              </StyledButton>
            </BottomBar>
            {menuOpen ? (
              <Menu
                user={user}
                closeMenu={closeMenu}
                unreadDecisions={unreadDecisions}
              />
            ) : null}
          </>
        ) : null
      }
    </UnwrapResult>
  )
})

const BottomBar = styled.nav`
  z-index: 25;
  color: ${colors.grayscale.g100};
  background-color: ${colors.grayscale.g0};
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: ${mobileBottomNavHeight}px;
  width: 100%;
  padding: ${defaultMargins.xs};
  position: sticky;
  bottom: 0;
  left: 0;
  box-shadow: 0px -2px 4px rgba(0, 0, 0, 0.15);

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

const BottomBarLink = React.memo(function BottomBarLink({
  to,
  text,
  icon,
  activeIcon,
  showNotification,
  'data-qa': dataQa
}: {
  to: string
  text: string
  icon: IconDefinition
  activeIcon: IconDefinition
  showNotification: boolean
  'data-qa'?: string
}) {
  const location = useLocation()
  const active = location.pathname.includes(to)
  return (
    <StyledLink to={to} $isActive={active} data-qa={dataQa}>
      <AttentionIndicator toggled={showNotification} position="top">
        <FontAwesomeIcon icon={active ? activeIcon : icon} />
      </AttentionIndicator>
      {text}
    </StyledLink>
  )
})

const bottomNavClickableStyles = (isActive: boolean) => css`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-weight: ${fontWeights.semibold};
  font-size: 0.875rem;
  color: ${isActive ? colors.main.m2 : colors.grayscale.g100};
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;

  svg {
    height: 22px;
    width: 22px;
    color: ${isActive ? colors.main.m2 : colors.grayscale.g100};
  }
`

const StyledLink = styled(Link)<{ $isActive: boolean }>`
  ${({ $isActive }) => bottomNavClickableStyles($isActive)}
`

const StyledButton = styled.button<{ $isActive: boolean }>`
  ${({ $isActive }) => bottomNavClickableStyles($isActive)}
`

const Menu = React.memo(function Menu({
  user,
  closeMenu,
  unreadDecisions
}: {
  user: User
  closeMenu: () => void
  unreadDecisions: number
}) {
  const t = useTranslation()
  const [lang, setLang] = useLang()
  const showUserAttentionIndicator = !user.email
  const lock = user.authLevel !== 'STRONG' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )
  return (
    <MenuContainer>
      <FixedSpaceRow spacing="xs" justifyContent="flex-end">
        {langs.map((l) => (
          <SelectionChip
            key={l}
            text={t.header.langMobile[l]}
            selected={lang === l}
            onChange={() => setLang(l)}
          />
        ))}
      </FixedSpaceRow>
      <Separator />
      <DropDownLink
        selected={window.location.pathname.includes('/applications')}
        data-qa="user-menu-applications"
        to="/applications"
        onClick={closeMenu}
      >
        {t.header.nav.applications} {lock}
      </DropDownLink>
      <DropDownLink
        selected={window.location.pathname.includes('/decisions')}
        data-qa="user-menu-decisions"
        to="/decisions"
        onClick={closeMenu}
      >
        {t.header.nav.decisions} {lock}
        {unreadDecisions ? (
          <CircledChar
            aria-label={`${unreadDecisions} ${t.header.notifications}`}
            data-qa="user-menu-decisions-notification-count"
          >
            {unreadDecisions}
          </CircledChar>
        ) : null}
      </DropDownLink>
      <DropDownLink
        selected={window.location.pathname.includes('/income')}
        data-qa="user-menu-income"
        to="/income"
        onClick={closeMenu}
      >
        {t.header.nav.income} {lock}
      </DropDownLink>
      <Separator />
      <DropDownLink
        selected={window.location.pathname.includes('/personal-details')}
        data-qa="user-menu-personal-details"
        to="/personal-details"
        onClick={closeMenu}
      >
        {t.header.nav.personalDetails}
        {lock}
        {showUserAttentionIndicator && (
          <CircledChar
            aria-label={t.header.attention}
            data-qa="personal-details-notification"
          >
            !
          </CircledChar>
        )}
      </DropDownLink>
      <DropDownLink
        selected={false}
        key="user-menu-logout"
        to={getLogoutUri(user)}
        onClick={() => (location.href = getLogoutUri(user))}
      >
        {t.header.logout}
        <FontAwesomeIcon icon={farSignOut} />
      </DropDownLink>
    </MenuContainer>
  )
})

const MenuContainer = styled.div`
  position: fixed;
  overflow-y: scroll;
  top: ${headerHeightMobile}px;
  bottom: ${mobileBottomNavHeight}px;
  right: 0;
  background: ${colors.grayscale.g0};
  box-sizing: border-box;
  width: 100vw;
  height: calc(100% - ${headerHeightMobile}px - ${mobileBottomNavHeight}px);
  padding: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  align-items: flex-end;

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

const Separator = styled.div`
  border-bottom: 1px solid ${colors.grayscale.g15};
  width: calc(100% + ${defaultMargins.s} + ${defaultMargins.s});
  margin: ${defaultMargins.s} -${defaultMargins.s};
`
