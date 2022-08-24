// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { formatPreferredName } from 'lib-common/names'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { desktopMin, desktopSmall } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faExclamation,
  faLockAlt,
  faSignIn,
  faSignOut,
  faUser,
  fasChevronDown,
  fasChevronUp
} from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
import { AuthContext, User } from '../auth/state'
import { Lang, langs, useLang, useTranslation } from '../localization'

import AttentionIndicator from './AttentionIndicator'
import { getLogoutUri } from './const'

interface Props {
  unreadMessagesCount: number
  unreadChildren: number
  unreadApplications: number
  hideLoginButton: boolean
}

export default React.memo(function DesktopNav({
  unreadMessagesCount,
  unreadChildren,
  unreadApplications,
  hideLoginButton
}: Props) {
  const { user } = useContext(AuthContext)

  const t = useTranslation()
  return (
    <UnwrapResult result={user} loading={() => null}>
      {(user) => {
        const isEnduser = user?.authLevel === 'STRONG'
        const maybeLockElem = !isEnduser && (
          <FontAwesomeIcon icon={faLockAlt} size="xs" />
        )
        return (
          <Container data-qa="desktop-nav">
            <Nav>
              {user ? (
                <>
                  {user.accessibleFeatures.reservations && (
                    <HeaderNavLink
                      to="/calendar"
                      data-qa="nav-calendar"
                      text={t.header.nav.calendar}
                    />
                  )}
                  {user.accessibleFeatures.messages && (
                    <HeaderNavLink
                      to="/messages"
                      data-qa="nav-messages"
                      text={t.header.nav.messages}
                      notificationCount={unreadMessagesCount}
                    />
                  )}
                  <HeaderNavLink
                    to="/children"
                    data-qa="nav-children"
                    text={t.header.nav.children}
                    notificationCount={unreadChildren}
                    lockElem={maybeLockElem}
                  />
                  <HeaderNavLink
                    to="/applying"
                    data-qa="nav-applying"
                    text={t.header.nav.applications}
                    notificationCount={unreadApplications}
                  />
                </>
              ) : null}
            </Nav>
            <FixedSpaceRow spacing="zero">
              <LanguageMenu />
              {user ? (
                <UserMenu user={user} />
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
      }}
    </UnwrapResult>
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

export const CircledChar = styled.div.attrs({
  className: 'circled-char'
})`
  width: ${defaultMargins.s};
  height: ${defaultMargins.s};
  border: 1px solid ${colors.grayscale.g100};
  padding: 11px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
  letter-spacing: 0;
`

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
        <DropDown data-qa="select-lang">
          {langs.map((l: Lang) => (
            <DropDownItem
              key={l}
              selected={lang === l}
              onClick={() => {
                setLang(l)
                setOpen(false)
              }}
              data-qa={`lang-${l}`}
              lang={l}
            >
              <span>{t.header.lang[l]}</span>
            </DropDownItem>
          ))}
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

const UserMenu = React.memo(function UserMenu({ user }: { user: User }) {
  const t = useTranslation()
  const navigate = useNavigate()
  const theme = useTheme()
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
      <DropDownButton onClick={toggleOpen} data-qa="user-menu-title-desktop">
        <AttentionIndicator
          toggled={showUserAttentionIndicator}
          data-qa="attention-indicator-desktop"
        >
          <Icon icon={faUser} />
        </AttentionIndicator>
        <Gap size="s" horizontal />
        {formatPreferredName(user)} {user.lastName}
        <DropDownIcon icon={open ? fasChevronUp : fasChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown data-qa="user-menu">
          <DropDownItem
            selected={window.location.pathname.includes('/personal-details')}
            data-qa="user-menu-personal-details"
            onClick={() => {
              setOpen(false)
              navigate('/personal-details')
            }}
          >
            {t.header.nav.personalDetails}
            {maybeLockElem}
            {showUserAttentionIndicator && (
              <RoundIcon
                content={faExclamation}
                color={theme.colors.status.warning}
                size="s"
                data-qa="personal-details-attention-indicator-desktop"
              />
            )}
          </DropDownItem>
          <DropDownItem
            selected={window.location.pathname.includes('/income')}
            data-qa="user-menu-income"
            onClick={() => {
              setOpen(false)
              navigate('/income')
            }}
          >
            {t.header.nav.income} {maybeLockElem}
          </DropDownItem>
          <DropDownItem
            key="user-menu-logout"
            selected={false}
            onClick={() => {
              location.href = getLogoutUri(user)
            }}
          >
            {t.header.logout}
            <FontAwesomeIcon icon={faSignOut} />
          </DropDownItem>
        </DropDown>
      ) : null}
    </DropDownContainer>
  )
})

const DropDownContainer = styled.nav`
  position: relative;
`

const DropDownButton = styled.button`
  color: inherit;
  background: transparent;
  font-size: 1.125rem;
  font-weight: ${fontWeights.semibold};
  line-height: 2rem;
  padding: ${defaultMargins.xs} ${defaultMargins.m};
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  flex-direction: row;
  align-items: center;

  &:hover {
    color: ${colors.main.m2Hover};
  }
`

const DropDownIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
  margin-left: 8px;
`

const DropDown = styled.ul`
  position: absolute;
  z-index: 30;
  list-style: none;
  margin: 0;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-end;
  background: ${colors.grayscale.g0};
  box-shadow: 0 2px 6px 0 ${colors.grayscale.g15};
  right: 0;
  min-width: 240px;
`

const DropDownItem = styled.button<{ selected: boolean }>`
  display: inline-flex;
  flex-direction: row;
  gap: ${defaultMargins.xs};
  align-items: center;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: Open Sans;
  color: ${({ selected }) =>
    selected ? colors.main.m2 : colors.grayscale.g100};
  font-size: 1.125rem;
  font-weight: ${fontWeights.semibold};
  line-height: 2rem;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-bottom: 4px solid transparent;
  border-bottom-color: ${({ selected }) =>
    selected ? colors.main.m2 : 'transparent'};

  &:hover {
    color: ${colors.main.m2Hover};
  }
`

const HeaderNavLink = React.memo(function HeaderNavLink({
  notificationCount,
  text,
  lockElem,
  ...props
}: {
  onClick?: () => void
  to: string
  notificationCount?: number
  text: string
  lockElem?: React.ReactNode
  'data-qa'?: string
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
          data-qa={props['data-qa'] && `${props['data-qa']}-notification-count`}
        >
          {notificationCount}
        </CircledChar>
      )}
    </StyledNavLink>
  )
})
