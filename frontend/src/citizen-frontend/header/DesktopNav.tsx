// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { desktopMin } from 'lib-components/breakpoints'
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
import React, { useCallback, useContext, useState } from 'react'
import { NavLink, useHistory } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'
import { UnwrapResult } from '../async-rendering'
import { AuthContext, User } from '../auth/state'
import { Lang, langs, useLang, useTranslation } from '../localization'
import AttentionIndicator from './AttentionIndicator'
import { getLoginUri, getLogoutUri } from './const'

interface Props {
  unreadMessagesCount: number
  unreadPedagogicalDocuments: number
}

export default React.memo(function DesktopNav({
  unreadMessagesCount,
  unreadPedagogicalDocuments
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
                  <StyledNavLink to="/applying" data-qa="nav-applying">
                    {t.header.nav.applying} {maybeLockElem}
                  </StyledNavLink>
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
                  {user.accessibleFeatures.messages && (
                    <StyledNavLink to="/messages" data-qa="nav-messages">
                      {t.header.nav.messages}{' '}
                      {unreadMessagesCount > 0 ? (
                        <CircledChar>{unreadMessagesCount}</CircledChar>
                      ) : (
                        ''
                      )}
                    </StyledNavLink>
                  )}
                  {user.accessibleFeatures.reservations && (
                    <StyledNavLink to="/calendar" data-qa="nav-calendar">
                      {t.header.nav.calendar}
                    </StyledNavLink>
                  )}
                  <StyledNavLink to="/children" data-qa="nav-children">
                    {t.header.nav.children} {maybeLockElem}
                  </StyledNavLink>
                </>
              ) : null}
            </Nav>
            <LanguageMenu />
            {user ? (
              <UserMenu user={user} />
            ) : (
              <Login href={getLoginUri()} data-qa="login-btn">
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
  min-width: 140px;
  padding: 0 ${defaultMargins.s};
  border-bottom: 3px solid transparent;

  &.active {
    border-color: ${colors.greyscale.white};
    font-weight: ${fontWeights.bold};
  }
`

const Login = styled.a`
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
  border: 1px solid ${colors.greyscale.white};
  padding: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
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
    <div ref={dropDownRef}>
      <DropDownButton onClick={toggleOpen} data-qa="button-select-language">
        {lang.toUpperCase()}
        <DropDownIcon icon={open ? faChevronUp : faChevronDown} />
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
            >
              <LanguageShort>{l}</LanguageShort>
              <span>{t.header.lang[l]}</span>
            </DropDownItem>
          ))}
        </DropDown>
      ) : null}
    </div>
  )
})

const UserMenu = React.memo(function UserMenu({ user }: { user: User }) {
  const t = useTranslation()
  const history = useHistory()
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
    <div ref={dropDownRef}>
      <DropDownButton onClick={toggleOpen} data-qa="user-menu-title-desktop">
        <AttentionIndicator
          toggled={showUserAttentionIndicator}
          data-qa="attention-indicator-desktop"
        >
          <Icon icon={faUser} />
        </AttentionIndicator>
        <Gap size="s" horizontal />
        {user.preferredName || user.firstName} {user.lastName}
        <DropDownIcon icon={open ? faChevronUp : faChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown data-qa="user-menu">
          <DropDownItem
            selected={window.location.pathname.includes('/personal-details')}
            data-qa="user-menu-personal-details"
            onClick={() => {
              setOpen(false)
              history.push('/personal-details')
            }}
          >
            {t.header.nav.personalDetails}
            {maybeLockElem}
            {showUserAttentionIndicator && (
              <RoundIcon
                content={faExclamation}
                color={theme.colors.accents.orange}
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
              history.push('/income')
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
    </div>
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

const DropDown = styled.ul`
  position: absolute;
  z-index: 10;
  list-style: none;
  margin: 0;
  padding: 0;
  background: ${colors.greyscale.white};
  box-shadow: 0 2px 6px 0 ${colors.greyscale.lighter};
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
    selected ? colors.brandEspoo.espooBlue : colors.greyscale.darkest};
  font-size: 1em;
  font-weight: ${({ selected }) =>
    selected ? fontWeights.semibold : fontWeights.normal};
  padding: 15px 50px 15px 10px;
  width: 100%;

  &:hover {
    background: ${colors.blues.light};
  }
`

const LanguageShort = styled.span`
  text-transform: uppercase;
`
