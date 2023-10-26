// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import { formatFirstName } from 'lib-common/names'
import { desktopMin, desktopSmall } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { Gap, defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faLockAlt,
  faSignIn,
  farBars,
  farSignOut,
  farXmark,
  fasChevronDown,
  fasChevronUp
} from 'lib-icons'

import { AuthContext, User } from '../auth/state'
import { useTranslation } from '../localization'

import AttentionIndicator from './AttentionIndicator'
import { getLogoutUri } from './const'
import {
  CircledChar,
  DropDown,
  DropDownButton,
  DropDownContainer,
  DropDownIcon,
  DropDownInfo,
  DropDownLink,
  DropDownLocalLink,
  LanguageMenu
} from './shared-components'
import { useChildrenWithOwnPage, useUnreadChildNotifications } from './utils'

interface Props {
  unreadMessagesCount: number
  unreadDecisions: number
  hideLoginButton: boolean
}

export default React.memo(function DesktopNav({
  unreadMessagesCount,
  unreadDecisions,
  hideLoginButton
}: Props) {
  const t = useTranslation()
  const { user: userResult } = useContext(AuthContext)
  const user = userResult.getOrElse(undefined)

  return (
    <Container data-qa="desktop-nav">
      <Nav>
        {user ? (
          <>
            {user.accessibleFeatures.reservations && (
              <HeaderNavLink
                to="/calendar"
                data-qa="nav-calendar-desktop"
                text={t.header.nav.calendar}
              />
            )}
            {user.accessibleFeatures.messages && (
              <HeaderNavLink
                to="/messages"
                data-qa="nav-messages-desktop"
                text={t.header.nav.messages}
                notificationCount={unreadMessagesCount}
              />
            )}
            <ChildrenMenu />
          </>
        ) : null}
      </Nav>
      <FixedSpaceRow spacing="zero">
        <LanguageMenu />
        {user ? (
          <SubNavigationMenu user={user} unreadDecisions={unreadDecisions} />
        ) : hideLoginButton ? null : (
          <nav>
            <Login to="/login" data-qa="login-btn">
              <Icon icon={faSignIn} />
              <Gap size="xs" horizontal />
              {t.header.login}
            </Login>
          </nav>
        )}
      </FixedSpaceRow>
    </Container>
  )
})

const NavLinkText = React.memo(function NavLinkText({
  text
}: {
  text: string
}) {
  return (
    <div>
      <SpaceReservingText aria-hidden="true">{text}</SpaceReservingText>
      <div data-qa="nav-text">{text}</div>
    </div>
  )
})

const Container = styled.div`
  display: none;

  @media (min-width: ${desktopMin}) {
    margin-top: 16px;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
  }
`

const Nav = styled.nav`
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  margin-left: ${defaultMargins.X3L};
`

const StyledNavLink = styled(NavLink)`
  color: inherit;
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: ${defaultMargins.xs};
  font-family: Montserrat, sans-serif;
  font-weight: ${fontWeights.semibold};
  font-size: 1.125rem;
  line-height: 2rem;
  text-align: center;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-bottom: 4px solid transparent;

  @media screen and (min-width: ${desktopSmall}) {
    padding: ${defaultMargins.xs} ${defaultMargins.m};
  }

  &:hover {
    color: ${colors.main.m2Hover};

    .circled-char {
      border-color: ${colors.main.m2Hover};
    }
  }

  &.active {
    color: ${colors.main.m2};
    border-bottom-color: ${colors.main.m2};

    .circled-char {
      border-width: 2px;
      border-color: ${colors.main.m2};
      padding: 10px;
    }
  }
`

const SpaceReservingText = styled.span`
  font-weight: ${fontWeights.bold};
  display: block;
  height: 0;
  visibility: hidden;
`

const Login = styled(Link)`
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  color: inherit;
  text-decoration: none;
  font-size: 1.125rem;
  font-weight: ${fontWeights.semibold};
  line-height: 2rem;
  padding: ${defaultMargins.xs} ${defaultMargins.m};
  border-bottom: 3px solid transparent;
`

const Icon = styled(FontAwesomeIcon)`
  font-size: 1.25rem;
`

const ChildrenMenu = React.memo(function ChildrenMenu() {
  const t = useTranslation()
  const location = useLocation()
  const childrenWithOwnPage = useChildrenWithOwnPage()
  const { unreadChildNotifications, totalUnreadChildNotifications } =
    useUnreadChildNotifications()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])
  const dropDownContainerRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )
  const dropDownRef = useRef<HTMLUListElement | null>(null)
  useEffect(() => {
    if (open && dropDownRef.current) {
      const firstAnchor = dropDownRef.current.querySelector('a')
      if (firstAnchor) {
        firstAnchor.focus()
      }
    }
  }, [open])

  if (childrenWithOwnPage.length === 0) {
    return null
  }

  if (childrenWithOwnPage.length === 1) {
    const childId = childrenWithOwnPage[0].id
    return (
      <HeaderNavLink
        to={`/children/${childId}`}
        data-qa="nav-children-desktop"
        text={t.header.nav.children}
        notificationCount={totalUnreadChildNotifications}
      />
    )
  }

  const duplicateChildInfo = getDuplicateChildInfo(
    childrenWithOwnPage,
    t,
    'long'
  )

  return (
    <DropDownContainer ref={dropDownContainerRef}>
      <DropDownButton
        className={classNames({
          active: location.pathname.startsWith('/children')
        })}
        onClick={toggleOpen}
        aria-label={`${t.header.nav.children}${
          totalUnreadChildNotifications && totalUnreadChildNotifications > 0
            ? `, ${totalUnreadChildNotifications} ${t.header.notifications}`
            : ''
        }`}
        aria-expanded={open}
        aria-haspopup="true"
        data-qa="nav-children-desktop"
        role="menuitem"
      >
        {t.header.nav.children}
        {totalUnreadChildNotifications > 0 && (
          <CircledChar
            aria-label={`${totalUnreadChildNotifications} ${t.header.notifications}`}
            data-qa="nav-children-desktop-notification-count"
          >
            {totalUnreadChildNotifications}
          </CircledChar>
        )}
        <DropDownIcon
          icon={open ? fasChevronUp : fasChevronDown}
          data-qa="drop-down-icon"
        />
      </DropDownButton>
      {open ? (
        <DropDown ref={dropDownRef} $align="left" data-qa="select-child">
          {childrenWithOwnPage.map((child) => (
            <DropDownLink
              key={child.id}
              to={`/children/${child.id}`}
              onClick={() => {
                setOpen(false)
              }}
              data-qa={`children-menu-${child.id}`}
            >
              <span>
                {formatFirstName(child)} {child.lastName}
              </span>
              {unreadChildNotifications[child.id] ? (
                <CircledChar
                  aria-label={`${unreadChildNotifications[child.id]} ${
                    t.header.notifications
                  }`}
                  data-qa={`children-menu-${child.id}-notification-count`}
                >
                  {unreadChildNotifications[child.id]}
                </CircledChar>
              ) : null}
              {duplicateChildInfo[child.id] !== undefined && (
                <DropDownInfo>{duplicateChildInfo[child.id]}</DropDownInfo>
              )}
            </DropDownLink>
          ))}
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

const SubNavigationMenu = React.memo(function SubNavigationMenu({
  user,
  unreadDecisions
}: {
  user: User
  unreadDecisions: number
}) {
  const t = useTranslation()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])
  const dropDownContainerRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )
  const showUserAttentionIndicator = !user.email
  const weakAuth = user.authLevel !== 'STRONG'
  const maybeLockElem = weakAuth && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )

  const dropDownRef = useRef<HTMLUListElement | null>(null)
  useEffect(() => {
    if (open && dropDownRef.current) {
      const firstAnchor = dropDownRef.current.querySelector('a')
      if (firstAnchor) {
        firstAnchor.focus()
      }
    }
  }, [open])

  return (
    <DropDownContainer ref={dropDownContainerRef}>
      <DropDownButton
        onClick={toggleOpen}
        data-qa="sub-nav-menu-desktop"
        aria-expanded={open}
        aria-haspopup="true"
      >
        {t.header.nav.subNavigationMenu}
        <AttentionIndicator
          toggled={showUserAttentionIndicator}
          position="bottom"
          data-qa="attention-indicator-sub-menu-desktop"
        >
          <DropDownIcon icon={open ? farXmark : farBars} />
        </AttentionIndicator>
      </DropDownButton>
      {open ? (
        <DropDown ref={dropDownRef} $align="right" data-qa="user-menu">
          <DropDownLink
            data-qa="sub-nav-menu-applications"
            to="/applications"
            onClick={() => setOpen(false)}
            aria-label={
              t.header.nav.applications +
              (weakAuth ? ` (${t.header.requiresStrongAuth})` : '')
            }
          >
            {t.header.nav.applications} {maybeLockElem}
          </DropDownLink>
          <DropDownLink
            data-qa="sub-nav-menu-decisions"
            to="/decisions"
            onClick={() => setOpen(false)}
            aria-label={
              t.header.nav.decisions +
              (weakAuth ? ` (${t.header.requiresStrongAuth})` : '')
            }
          >
            {t.header.nav.decisions} {maybeLockElem}
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
            onClick={() => setOpen(false)}
            aria-label={
              t.header.nav.income +
              (weakAuth ? ` (${t.header.requiresStrongAuth})` : '')
            }
          >
            {t.header.nav.income} {maybeLockElem}
          </DropDownLink>
          <Separator />
          <DropDownLink
            data-qa="sub-nav-menu-personal-details"
            to="/personal-details"
            onClick={() => setOpen(false)}
            aria-label={
              t.header.nav.personalDetails +
              (showUserAttentionIndicator ? ` (${t.header.attention})` : '')
            }
          >
            {t.header.nav.personalDetails}
            {showUserAttentionIndicator && (
              <CircledChar
                aria-label={t.header.attention}
                data-qa="personal-details-notification"
              >
                !
              </CircledChar>
            )}
          </DropDownLink>
          <DropDownLocalLink
            data-qa="sub-nav-menu-logout"
            href={getLogoutUri(user)}
          >
            {t.header.logout}
            <FontAwesomeIcon icon={farSignOut} />
          </DropDownLocalLink>
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

const Separator = styled.div`
  border-top: 1px solid ${colors.grayscale.g15};
  margin: ${defaultMargins.s} -${defaultMargins.m};
  width: calc(100% + ${defaultMargins.m} + ${defaultMargins.m});
`

const HeaderNavLink = React.memo(function HeaderNavLink({
  notificationCount,
  text,
  lockElem,
  icon,
  ...props
}: {
  to: string
  notificationCount?: number
  text: string
  lockElem?: React.ReactNode
  onClick?: (e: React.MouseEvent<HTMLAnchorElement>) => void
  icon?: IconDefinition
  'data-qa': string
}) {
  const t = useTranslation()

  return (
    <StyledNavLink
      {...props}
      aria-label={`${text}${
        lockElem ? `, ${t.header.requiresStrongAuth}` : ''
      }${
        notificationCount && notificationCount > 0
          ? `, ${notificationCount} ${t.header.notifications}`
          : ''
      }`}
      role="menuitem"
    >
      <NavLinkText text={text} />
      {lockElem}
      {!!notificationCount && (
        <CircledChar
          aria-label={`${notificationCount} ${t.header.notifications}`}
          data-qa={`${props['data-qa']}-notification-count`}
        >
          {notificationCount}
        </CircledChar>
      )}
      {icon && <DropDownIcon icon={icon} data-qa="icon" />}
    </StyledNavLink>
  )
})
