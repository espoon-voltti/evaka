// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { desktopMin } from 'lib-components/breakpoints'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faChevronDown,
  faChevronUp,
  faLockAlt,
  faSignIn,
  faSignOut
} from 'lib-icons'
import React, { useCallback, useState } from 'react'
import { NavLink } from 'react-router-dom'
import styled from 'styled-components'
import { featureFlags } from 'lib-customizations/employee'
import { useUser } from '../auth'
import { Lang, langs, useLang, useTranslation } from '../localization'
import { getLoginUri, getLogoutUri } from './const'

interface Props {
  unreadMessagesCount: number
}

export default React.memo(function DesktopNav({ unreadMessagesCount }: Props) {
  const user = useUser()
  const t = useTranslation()

  const maybeLockElem = user?.userType !== 'ENDUSER' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )
  return (
    <Container data-qa="desktop-nav">
      <Nav>
        {user && (
          <>
            <StyledNavLink to="/" exact data-qa="nav-map">
              {t.header.nav.map}
            </StyledNavLink>
            <StyledNavLink to="/applications" data-qa="nav-applications">
              {t.header.nav.applications} {maybeLockElem}
            </StyledNavLink>
            <StyledNavLink to="/decisions" data-qa="nav-decisions">
              {t.header.nav.decisions} {maybeLockElem}
            </StyledNavLink>
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
          </>
        )}
      </Nav>
      <LanguageMenu />
      {user ? (
        <UserMenu />
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
  font-family: Montserrat, sans-serif;
  font-size: 0.9375rem;
  text-transform: uppercase;
  min-width: 140px;
  padding: 0 ${defaultMargins.s};
  border-bottom: 3px solid transparent;

  &.active {
    border-color: ${colors.greyscale.white};
    font-weight: 700;
  }

  & > :last-child {
    margin-left: ${defaultMargins.xs};
  }
`

const Login = styled.a`
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  color: inherit;
  text-decoration: none;
  font-weight: 600;
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
  margin-left: ${defaultMargins.xs};
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
      <DropDownButton onClick={toggleOpen} data-qa={'button-select-language'}>
        {lang.toUpperCase()}
        <DropDownIcon icon={open ? faChevronUp : faChevronDown} />
      </DropDownButton>
      {open ? (
        <DropDown data-qa={'select-lang'}>
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

const UserMenu = React.memo(function UserMenu() {
  const t = useTranslation()
  const user = useUser()
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])
  const dropDownRef = useCloseOnOutsideClick<HTMLDivElement>(() =>
    setOpen(false)
  )

  return (
    <div ref={dropDownRef} style={{ position: 'relative' }}>
      <DropDownButton onClick={toggleOpen} data-qa={'user-menu-title'}>
        {user?.firstName} {user?.lastName}
        <DropDownIcon icon={open ? faChevronUp : faChevronDown} />
      </DropDownButton>
      {open && user ? (
        <DropDown data-qa={'user-menu'} style={{ width: '200px', right: '0' }}>
          {featureFlags.experimental?.incomeStatements && (
            <DropDownItem
              selected={window.location.pathname.includes('/income')}
              data-qa={'user-menu-income'}
              onClick={() => (location.href = '/income')}
            >
              {t.header.nav.income}
            </DropDownItem>
          )}
          <DropDownItem
            key={'user-menu-logout'}
            selected={false}
            onClick={() => {
              location.href = getLogoutUri(user)
            }}
          >
            <div>
              <span style={{ marginRight: '10px' }}>{t.header.logout}</span>{' '}
              <FontAwesomeIcon icon={faSignOut} />
            </div>
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
  display: block;
`

const DropDownIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
  margin-left: 8px;
`

const DropDown = styled.ul`
  position: absolute;
  z-index: 9999;
  list-style: none;
  margin: 0;
  padding: 0;
  background: ${colors.greyscale.white};
  box-shadow: 0 2px 6px 0 ${colors.greyscale.lighter};
`

const DropDownItem = styled.button<{ selected: boolean }>`
  display: flex;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: Open Sans;
  color: ${({ selected }) =>
    selected ? colors.brandEspoo.espooBlue : colors.greyscale.darkest};
  font-size: 1em;
  font-weight: ${({ selected }) => (selected ? 600 : 400)};
  padding-top: 15px;
  padding-bottom: 15px;
  padding-left: 10px;
  padding-right: 50px;
  width: 100%;

  &:hover {
    background: ${colors.blues.light};
  }
`

const LanguageShort = styled.span`
  width: 1.8rem;
  text-transform: uppercase;
  text-align: left;
`
