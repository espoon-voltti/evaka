// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { desktopMin } from 'lib-components/breakpoints'
import colors from 'lib-customizations/common'
import { defaultMargins, Gap } from 'lib-components/white-space'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import {
  faCheck,
  faChevronDown,
  faChevronUp,
  faLockAlt,
  faSignIn
} from 'lib-icons'
import { useUser } from '../auth'
import { Lang, langs, useLang, useTranslation } from '../localization'
import { getLoginUri, getLogoutUri } from './const'
import { featureFlags } from '../config'

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
    <Container>
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
            {featureFlags.messaging && (
              <StyledNavLink to="/messages" data-qa="nav-messages">
                {t.header.nav.messages}{' '}
                {unreadMessagesCount > 0 ? (
                  <CircledChar>{unreadMessagesCount}</CircledChar>
                ) : (
                  ''
                )}
              </StyledNavLink>
            )}
          </>
        )}
      </Nav>
      <LanguageMenu />
      {user ? (
        <Logout href={getLogoutUri(user)} data-qa="logout-btn">
          {user ? (
            <UserName>
              <span>{user.firstName}</span> <span>{user.lastName}</span>
            </UserName>
          ) : null}
          <LogoutText>{t.header.logout}</LogoutText>
        </Logout>
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

const Logout = styled(Login)`
  flex-direction: column;
`

const LogoutText = styled.div`
  font-size: 0.75rem;
  text-transform: uppercase;
`

const UserName = styled.span`
  text-align: center;
  text-transform: none;

  span {
    white-space: nowrap;
  }
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
      <LanguageButton onClick={toggleOpen} data-qa={'button-select-language'}>
        {lang}
        <LanguageIcon icon={open ? faChevronUp : faChevronDown} />
      </LanguageButton>
      {open ? (
        <LanguageDropDown data-qa={'select-lang'}>
          {langs.map((l: Lang) => (
            <LanguageListElement key={l}>
              <LanguageDropDownButton
                selected={lang === l}
                onClick={() => {
                  setLang(l)
                  setOpen(false)
                }}
                data-qa={`lang-${l}`}
              >
                <LanguageShort>{l}</LanguageShort>
                <span>{t.header.lang[l]}</span>
                {lang === l ? <LanguageCheck icon={faCheck} /> : null}
              </LanguageDropDownButton>
            </LanguageListElement>
          ))}
        </LanguageDropDown>
      ) : null}
    </div>
  )
})

const LanguageButton = styled.button`
  color: inherit;
  text-transform: uppercase;
  font-family: Montserrat, sans-serif;
  font-size: 1rem;
  background: transparent;
  height: 100%;
  padding: ${defaultMargins.s};
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  white-space: nowrap;
`

const LanguageIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
  margin-left: 8px;
`

const LanguageDropDown = styled.ul`
  position: absolute;
  z-index: 9999;
  top: 64px;
  list-style: none;
  margin: 0;
  padding: 0;
  background: ${colors.greyscale.white};
  box-shadow: 0 2px 6px 0 ${colors.greyscale.lighter};
`

const LanguageListElement = styled.li`
  display: block;
  width: 10.5em;
`

const LanguageDropDownButton = styled.button<{ selected: boolean }>`
  display: flex;
  border: none;
  background: transparent;
  cursor: pointer;
  color: ${colors.greyscale.dark};
  font-size: 1em;
  font-weight: ${({ selected }) => (selected ? 600 : 400)};
  padding: 10px;
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

const LanguageCheck = styled(FontAwesomeIcon)`
  margin-left: 8px;
`
