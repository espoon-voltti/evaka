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
import { fontWeights } from 'lib-components/typography'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faChevronDown,
  faChevronUp,
  faExclamation,
  faLockAlt,
  faSignIn,
  faSignOut,
  faUser
} from 'lib-icons'

import { UnwrapResult } from '../async-rendering'
import { AuthContext, User } from '../auth/state'
import { Lang, langs, useLang, useTranslation } from '../localization'

import AttentionIndicator from './AttentionIndicator'
import { getLogoutUri } from './const'

interface Props {
  unreadMessagesCount: number
  unreadPedagogicalDocuments: number
  hideLoginButton: boolean
}

export default React.memo(function DesktopNav({
  unreadMessagesCount,
  unreadPedagogicalDocuments,
  hideLoginButton
}: Props) {
  const { user } = useContext(AuthContext)

  const t = useTranslation()
  return (
    <UnwrapResult result={user} loading={() => null}>
      {(user) => {
        const isEnduser = user?.userType === 'ENDUSER'
        const maybeLockElem = !isEnduser && (
          <FontAwesomeIcon icon={faLockAlt} size="xs" />
        )
        return (
          <Container data-qa="desktop-nav">
            <Nav>
              {user ? (
                <>
                  {user.accessibleFeatures.reservations && (
                    <StyledNavLink to="/calendar" data-qa="nav-calendar">
                      {t.header.nav.calendar}
                    </StyledNavLink>
                  )}
                  {user.accessibleFeatures.messages && (
                    <StyledNavLink to="/messages" data-qa="nav-messages">
                      {t.header.nav.messages}{' '}
                      {unreadMessagesCount > 0 ? (
                        <CircledChar
                          aria-label={`${t.header.nav.messageCount(
                            unreadMessagesCount
                          )}`}
                        >
                          {unreadMessagesCount}
                        </CircledChar>
                      ) : (
                        ''
                      )}
                    </StyledNavLink>
                  )}
                  {user.accessibleFeatures.pedagogicalDocumentation && (
                    <StyledNavLink
                      to="/pedagogical-documents"
                      data-qa="nav-pedagogical-documents"
                    >
                      {t.header.nav.pedagogicalDocuments}
                      {maybeLockElem}
                      {isEnduser && unreadPedagogicalDocuments > 0 && (
                        <CircledChar data-qa="unread-pedagogical-documents-count">
                          {unreadPedagogicalDocuments}
                        </CircledChar>
                      )}
                    </StyledNavLink>
                  )}
                  <StyledNavLink to="/children" data-qa="nav-children">
                    {t.header.nav.children} {maybeLockElem}
                  </StyledNavLink>
                  <StyledNavLink to="/applying" data-qa="nav-applying">
                    {t.header.nav.applications}
                  </StyledNavLink>
                </>
              ) : null}
            </Nav>
            <LanguageMenu alignRight={hideLoginButton} />
            {user ? (
              <UserMenu user={user} />
            ) : hideLoginButton ? null : (
              <Login to="/login" data-qa="login-btn">
                <Icon icon={faSignIn} />
                <Gap size="xs" horizontal />
                {t.header.login}
              </Login>
            )}
            <Gap size="m" horizontal />
          </Container>
        )
      }}
    </UnwrapResult>
  )
})

const Container = styled.div`
  display: none;

  @media (min-width: ${desktopMin}) {
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
  }
`

const Nav = styled.nav`
  display: flex;
  flex-direction: row;
`

const StyledNavLink = styled(NavLink)`
  color: inherit;
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: ${defaultMargins.xs};
  font-family: Montserrat, sans-serif;
  font-size: 0.9375rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  text-align: center;
  padding: 0 ${defaultMargins.s};
  border-bottom: 4px solid transparent;

  @media screen and (min-width: ${desktopSmall}) {
    padding: 0 ${defaultMargins.m};
  }

  &:hover {
    font-weight: ${fontWeights.bold};
  }

  &.active {
    border-bottom-color: ${colors.grayscale.g0};
    font-weight: ${fontWeights.bold};
  }
`

const Login = styled(Link)`
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  color: inherit;
  text-decoration: none;
  font-weight: ${fontWeights.semibold};
  padding: 0 ${defaultMargins.s};
  border-bottom: 3px solid transparent;
`

const Icon = styled(FontAwesomeIcon)`
  font-size: 1.25rem;
`

export const CircledChar = styled.div`
  width: ${defaultMargins.s};
  height: ${defaultMargins.s};
  border: 1px solid ${colors.grayscale.g0};
  padding: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
`

interface LanguageMenuProps {
  alignRight: boolean
}

const LanguageMenu = React.memo(function LanguageMenu({
  alignRight
}: LanguageMenuProps) {
  const t = useTranslation()
  const [lang, setLang] = useLang()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])
  const dropDownRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )

  return (
    <nav ref={dropDownRef}>
      <DropDownButton onClick={toggleOpen} data-qa="button-select-language">
        {lang.toUpperCase()}
        <DropDownIcon icon={open ? faChevronUp : faChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown data-qa="select-lang" alignRight={alignRight}>
          {langs.map((l: Lang) => (
            <DropDownItem
              key={l}
              selected={lang === l}
              onClick={() => {
                setLang(l)
                setOpen(false)
              }}
              data-qa={`lang-${l}`}
            >
              <LanguageShort>{l}</LanguageShort>
              <span>{t.header.lang[l]}</span>
            </DropDownItem>
          ))}
        </DropDown>
      ) : null}
    </nav>
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
  const maybeLockElem = user.userType !== 'ENDUSER' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )

  return (
    <nav ref={dropDownRef}>
      <DropDownButton onClick={toggleOpen} data-qa="user-menu-title-desktop">
        <AttentionIndicator
          toggled={showUserAttentionIndicator}
          data-qa="attention-indicator-desktop"
        >
          <Icon icon={faUser} />
        </AttentionIndicator>
        <Gap size="s" horizontal />
        {formatPreferredName(user)} {user.lastName}
        <DropDownIcon icon={open ? faChevronUp : faChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown data-qa="user-menu" alignRight={false}>
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
    </nav>
  )
})

const DropDownButton = styled.button`
  color: inherit;
  background: transparent;
  font-size: 1rem;
  height: 100%;
  padding: ${defaultMargins.s};
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  flex-direction: row;
  align-items: center;
`

const DropDownIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
  margin-left: 8px;
`

const DropDown = styled.ul<{ alignRight: boolean }>`
  position: absolute;
  z-index: 10;
  list-style: none;
  margin: 0;
  padding: 0;
  background: ${colors.grayscale.g0};
  box-shadow: 0 2px 6px 0 ${colors.grayscale.g15};
  right: ${(p) => (p.alignRight ? '0' : 'unset')};
`

const DropDownItem = styled.button<{ selected: boolean }>`
  display: flex;
  flex-direction: row;
  gap: ${defaultMargins.xs};
  align-items: center;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: Open Sans;
  color: ${({ selected }) =>
    selected ? colors.main.m1 : colors.grayscale.g100};
  font-size: 1em;
  font-weight: ${({ selected }) =>
    selected ? fontWeights.semibold : fontWeights.normal};
  padding: 15px 50px 15px 10px;
  width: 100%;

  &:hover {
    background: ${colors.main.m4};
  }
`

const LanguageShort = styled.span`
  text-transform: uppercase;
`
