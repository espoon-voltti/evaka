// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useContext, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
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

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { ApplicationsContext } from '../applications/state'
import { UnwrapResult } from '../async-rendering'
import { AuthContext, User } from '../auth/state'
import { ChildrenContext } from '../children/state'
import { langs, useLang, useTranslation } from '../localization'
import { MessageContext } from '../messages/state'
import { useChildUnreadNotifications } from '../state/children/childrenState'

import AttentionIndicator from './AttentionIndicator'
import {
  getLogoutUri,
  headerHeightMobile,
  mobileBottomNavHeight
} from './const'
import {
  CircledChar,
  DropDownLink,
  DropDownLocalLink
} from './shared-components'

export default React.memo(function MobileNav() {
  const t = useTranslation()
  const { user } = useContext(AuthContext)
  const { unreadMessagesCount } = useContext(MessageContext)
  const { waitingConfirmationCount: unreadDecisions } =
    useContext(ApplicationsContext)
  const [menuOpen, setMenuOpen] = useState<'children' | 'submenu'>()
  const toggleSubMenu = useCallback(
    () => setMenuOpen((open) => (open === 'submenu' ? undefined : 'submenu')),
    []
  )
  const toggleChildrenMenu = useCallback(
    () => setMenuOpen((open) => (open === 'children' ? undefined : 'children')),
    []
  )
  const closeMenu = useCallback(() => setMenuOpen(undefined), [])

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
                  data-qa="nav-calendar-mobile"
                  text={t.header.nav.calendar}
                  icon={faCalendar}
                  activeIcon={fasCalendar}
                  showNotification={false}
                  onClick={closeMenu}
                />
              )}
              {user.accessibleFeatures.messages && (
                <BottomBarLink
                  to="/messages"
                  data-qa="nav-messages-mobile"
                  text={t.header.nav.messages}
                  icon={faEnvelope}
                  activeIcon={fasEnvelope}
                  showNotification={(unreadMessagesCount ?? 0) > 0}
                  onClick={closeMenu}
                />
              )}
              <ChildrenLink
                toggleChildrenMenu={toggleChildrenMenu}
                closeMenu={closeMenu}
              />
              <StyledButton
                onClick={toggleSubMenu}
                data-qa="sub-nav-menu-mobile"
              >
                <AttentionIndicator
                  toggled={
                    showUserAttentionIndicator(user) || unreadDecisions > 0
                  }
                  position="top"
                  data-qa="attention-indicator-sub-menu-mobile"
                >
                  <FontAwesomeIcon
                    icon={menuOpen === 'submenu' ? farXmark : faBars}
                  />
                </AttentionIndicator>
                {t.header.nav.subNavigationMenu}
              </StyledButton>
            </BottomBar>
            {menuOpen === 'submenu' ? (
              <Menu
                user={user}
                closeMenu={closeMenu}
                unreadDecisions={unreadDecisions}
              />
            ) : menuOpen === 'children' ? (
              <ChildrenMenu user={user} closeMenu={closeMenu} />
            ) : null}
          </>
        ) : null
      }
    </UnwrapResult>
  )
})

const showUserAttentionIndicator = (user: User) => !user.email

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

  @media print {
    display: none;
  }
`

const BottomBarLink = React.memo(function BottomBarLink({
  to,
  text,
  icon,
  activeIcon,
  showNotification,
  onClick,
  'data-qa': dataQa
}: {
  to: string
  text: string
  icon: IconDefinition
  activeIcon: IconDefinition
  showNotification: boolean
  onClick?: (e: React.MouseEvent<HTMLAnchorElement>) => void
  'data-qa': string
}) {
  const location = useLocation()
  const active = location.pathname.includes(to)
  return (
    <StyledLink to={to} onClick={onClick} data-qa={dataQa}>
      <AttentionIndicator
        toggled={showNotification}
        position="top"
        data-qa={`attention-indicator-${dataQa}`}
      >
        <FontAwesomeIcon icon={active ? activeIcon : icon} />
      </AttentionIndicator>
      {text}
    </StyledLink>
  )
})

const bottomNavClickableStyles = css`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-weight: ${fontWeights.semibold};
  font-size: 0.875rem;
  color: ${colors.grayscale.g100};
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;

  svg {
    height: 22px;
    width: 22px;
    color: ${colors.grayscale.g100};
  }

  &.active {
    color: ${colors.main.m2};

    svg {
      color: ${colors.main.m2};
    }
  }
`

const StyledLink = styled(NavLink)`
  ${bottomNavClickableStyles}
`

const StyledButton = styled.button`
  ${bottomNavClickableStyles}
`

const ChildrenLink = React.memo(function ChildrenLink({
  toggleChildrenMenu,
  closeMenu
}: {
  toggleChildrenMenu: () => void
  closeMenu: () => void
}) {
  const t = useTranslation()
  const location = useLocation()
  const { childrenWithOwnPage } = useContext(ChildrenContext)
  const { totalUnreadChildNotifications } = useChildUnreadNotifications()
  const active = location.pathname.startsWith('/children')

  if (childrenWithOwnPage.getOrElse([]).length === 0) {
    return null
  }

  if (childrenWithOwnPage.getOrElse([]).length === 1) {
    const childId = childrenWithOwnPage.getOrElse([])[0].id
    return (
      <BottomBarLink
        to={`/children/${childId}`}
        data-qa="nav-children-mobile"
        text={t.header.nav.children}
        icon={faChild}
        activeIcon={fasChild}
        showNotification={totalUnreadChildNotifications > 0}
        onClick={closeMenu}
      />
    )
  }

  return (
    <StyledButton
      className={classNames({ active })}
      onClick={toggleChildrenMenu}
      data-qa="nav-children-mobile"
    >
      <AttentionIndicator
        toggled={totalUnreadChildNotifications > 0}
        position="top"
        data-qa="attention-indicator-children-mobile"
      >
        <FontAwesomeIcon icon={active ? fasChild : faChild} />
      </AttentionIndicator>
      {t.header.nav.children}
    </StyledButton>
  )
})

const ChildrenMenu = React.memo(function ChildrenMenu({
  user,
  closeMenu
}: {
  user: User
  closeMenu: () => void
}) {
  const t = useTranslation()
  const { childrenWithOwnPage } = useContext(ChildrenContext)
  const { unreadChildNotifications } = useChildUnreadNotifications()
  const lock = user.authLevel !== 'STRONG' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )
  return (
    <ModalAccessibilityWrapper>
      <MenuContainer>
        {childrenWithOwnPage.getOrElse([]).map((child) => (
          <DropDownLink
            key={child.id}
            data-qa={`children-menu-${child.id}`}
            to={`/children/${child.id}`}
            onClick={closeMenu}
          >
            {child.preferredName || child.firstName} {child.lastName} {lock}
            {unreadChildNotifications[child.id] ? (
              <CircledChar
                aria-label={`${unreadChildNotifications[child.id]} ${
                  t.header.notifications
                }`}
                data-qa="sub-nav-menu-decisions-notification-count"
              >
                {unreadChildNotifications[child.id]}
              </CircledChar>
            ) : null}
          </DropDownLink>
        ))}
      </MenuContainer>
    </ModalAccessibilityWrapper>
  )
})

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
  const lock = user.authLevel !== 'STRONG' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )
  return (
    <ModalAccessibilityWrapper>
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
          data-qa="sub-nav-menu-applications"
          to="/applications"
          onClick={closeMenu}
        >
          {t.header.nav.applications} {lock}
        </DropDownLink>
        <DropDownLink
          data-qa="sub-nav-menu-decisions"
          to="/decisions"
          onClick={closeMenu}
        >
          {t.header.nav.decisions} {lock}
          {unreadDecisions ? (
            <CircledChar
              aria-label={`${unreadDecisions} ${t.header.notifications}`}
              data-qa="sub-nav-menu-decisions-notification-count"
            >
              {unreadDecisions}
            </CircledChar>
          ) : null}
        </DropDownLink>
        <DropDownLink
          data-qa="sub-nav-menu-income"
          to="/income"
          onClick={closeMenu}
        >
          {t.header.nav.income} {lock}
        </DropDownLink>
        <Separator />
        <DropDownLink
          data-qa="sub-nav-menu-personal-details"
          to="/personal-details"
          onClick={closeMenu}
        >
          {t.header.nav.personalDetails}
          {lock}
          {showUserAttentionIndicator(user) && (
            <CircledChar
              aria-label={t.header.attention}
              data-qa="personal-details-notification"
            >
              !
            </CircledChar>
          )}
        </DropDownLink>
        <DropDownLocalLink key="sub-nav-menu-logout" href={getLogoutUri(user)}>
          {t.header.logout}
          <FontAwesomeIcon icon={farSignOut} />
        </DropDownLocalLink>
      </MenuContainer>
    </ModalAccessibilityWrapper>
  )
})

const MenuContainer = styled.div`
  z-index: 24;
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
  text-align: right;

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

const Separator = styled.div`
  border-bottom: 1px solid ${colors.grayscale.g15};
  width: calc(100% + ${defaultMargins.s} + ${defaultMargins.s});
  margin: ${defaultMargins.s} -${defaultMargins.s};
`
