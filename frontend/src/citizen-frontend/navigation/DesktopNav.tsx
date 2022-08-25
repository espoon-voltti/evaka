// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useContext, useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { ChildrenContext } from 'citizen-frontend/children/state'
import { desktopMin, desktopSmall } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faLockAlt,
  faSignIn,
  fasChevronDown,
  fasChevronUp,
  farBars,
  farXmark,
  farSignOut
} from 'lib-icons'

import { AuthContext, User } from '../auth/state'
import { Lang, langs, useLang, useTranslation } from '../localization'

import AttentionIndicator from './AttentionIndicator'
import { getLogoutUri } from './const'
import { CircledChar, DropDownButton, DropDownLink } from './shared-components'

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

  if (user === undefined) {
    return null
  }

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
  const { children, unreadChildNotifications, totalUnreadChildNotifications } =
    useContext(ChildrenContext)
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(
    (e: React.MouseEvent<HTMLAnchorElement>) => {
      e.preventDefault()
      setOpen((state) => !state)
    },
    [setOpen]
  )
  const dropDownRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )

  if (children.getOrElse([]).length === 0) {
    return (
      <HeaderNavLink
        to="/children"
        data-qa="nav-children-desktop"
        text={t.header.nav.children}
        notificationCount={totalUnreadChildNotifications}
      />
    )
  }

  return (
    <DropDownContainer ref={dropDownRef}>
      <HeaderNavLink
        to="/children"
        text={t.header.nav.children}
        notificationCount={totalUnreadChildNotifications}
        onClick={toggleOpen}
        icon={open ? fasChevronUp : fasChevronDown}
        data-qa="nav-children-desktop"
      />
      {open ? (
        <DropDown $align="left" data-qa="select-child">
          {children.getOrElse([]).map((child) => (
            <DropDownLink
              key={child.id}
              to={`/children/${child.id}`}
              onClick={() => {
                setOpen(false)
              }}
              data-qa={`children-menu-${child.id}`}
            >
              {child.preferredName || child.firstName} {child.lastName}
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
            </DropDownLink>
          ))}
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

const LanguageMenu = React.memo(function LanguageMenu() {
  const t = useTranslation()
  const [lang, setLang] = useLang()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])
  const dropDownRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )

  return (
    <DropDownContainer ref={dropDownRef}>
      <DropDownButton onClick={toggleOpen} data-qa="button-select-language">
        {t.header.lang[lang]}
        <DropDownIcon icon={open ? fasChevronUp : fasChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown $align="right" data-qa="select-lang">
          {langs.map((l: Lang) => (
            <DropDownButton
              key={l}
              className={classNames({ active: lang === l })}
              onClick={() => {
                setLang(l)
                setOpen(false)
              }}
              data-qa={`lang-${l}`}
              lang={l}
            >
              <span>{t.header.lang[l]}</span>
            </DropDownButton>
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
  const dropDownRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )
  const showUserAttentionIndicator = !user.email
  const maybeLockElem = user.authLevel !== 'STRONG' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )

  return (
    <DropDownContainer ref={dropDownRef}>
      <DropDownButton onClick={toggleOpen} data-qa="sub-nav-menu-desktop">
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
        <DropDown $align="right" data-qa="user-menu">
          <DropDownLink
            data-qa="sub-nav-menu-applications"
            to="/applications"
            onClick={() => setOpen(false)}
          >
            {t.header.nav.applications} {maybeLockElem}
          </DropDownLink>
          <DropDownLink
            data-qa="sub-nav-menu-decisions"
            to="/decisions"
            onClick={() => setOpen(false)}
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
          >
            {t.header.nav.income} {maybeLockElem}
          </DropDownLink>
          <Separator />
          <DropDownLink
            data-qa="sub-nav-menu-personal-details"
            to="/personal-details"
            onClick={() => setOpen(false)}
          >
            {t.header.nav.personalDetails}
            {maybeLockElem}
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
            key="sub-nav-menu-logout"
            to={getLogoutUri(user)}
            onClick={() => (location.href = getLogoutUri(user))}
          >
            {t.header.logout}
            <FontAwesomeIcon icon={farSignOut} />
          </DropDownLink>
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

const DropDownContainer = styled.nav`
  position: relative;
`

const DropDownIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
`

const DropDown = styled.ul<{ $align: 'left' | 'right' }>`
  position: absolute;
  z-index: 30;
  list-style: none;
  margin: 0;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  background: ${colors.grayscale.g0};
  box-shadow: 0 2px 6px 0 ${colors.grayscale.g15};
  min-width: 240px;
  max-width: 600px;
  width: max-content;
  ${({ $align }) =>
    $align === 'left'
      ? css`
          left: 0;
          align-items: flex-start;
          text-align: left;
        `
      : css`
          right: 0;
          align-items: flex-end;
          text-align: right;
        `}
`

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
