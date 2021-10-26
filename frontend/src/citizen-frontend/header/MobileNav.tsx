// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faBars, faLockAlt, faSignIn, faSignOut, faTimes } from 'lib-icons'
import { memo, useCallback } from 'lib-common/memo'
import { desktopMin } from 'lib-components/breakpoints'
import colors from 'lib-customizations/common'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { Gap, defaultMargins } from 'lib-components/white-space'
import { tabletMin } from 'lib-components/breakpoints'
import { featureFlags } from 'lib-customizations/employee'
import { fontWeights } from 'lib-components/typography'
import { useUser } from '../auth'
import { langs, useLang, useTranslation } from '../localization'
import { getLoginUri, getLogoutUri } from './const'
import { CircledChar } from './DesktopNav'

interface Props {
  showMenu: boolean
  setShowMenu: Dispatch<SetStateAction<boolean>>
  unreadMessagesCount: number
  unreadPedagogicalDocumentsCount: number
}

export default memo<Props>(function MobileNav({
  showMenu,
  setShowMenu,
  unreadMessagesCount,
  unreadPedagogicalDocumentsCount
}) {
  const user = useUser()
  const t = useTranslation()
  const ref = useCloseOnOutsideClick<HTMLDivElement>(() => setShowMenu(false))
  const toggleMenu = useCallback(() => setShowMenu((show) => !show), [])
  const close = () => setShowMenu(false)

  return (
    <Container ref={ref} data-qa="mobile-nav">
      <MenuButton onClick={toggleMenu} data-qa="menu-button">
        <FontAwesomeIcon icon={showMenu ? faTimes : faBars} />
      </MenuButton>
      {showMenu ? (
        <MenuContainer>
          <LanguageMenu close={close} />
          <Gap size="s" />
          <Navigation
            close={close}
            unreadMessagesCount={unreadMessagesCount}
            unreadPedagogicalDocumentsCount={unreadPedagogicalDocumentsCount}
          />
          <Spacer />
          <UserContainer>
            <UserName>{`${user?.firstName ?? ''} ${
              user?.lastName ?? ''
            }`}</UserName>
            <Gap size="s" />
            {user ? (
              <a href={getLogoutUri(user)} data-qa="logout-btn">
                <LogInLogOutButton>
                  <FontAwesomeIcon icon={faSignOut} size="lg" />
                  <Gap size="xs" horizontal />
                  {t.header.logout}
                </LogInLogOutButton>
              </a>
            ) : (
              <a href={getLoginUri()} data-qa="login-btn">
                <LogInLogOutButton>
                  <FontAwesomeIcon icon={faSignIn} size="lg" />
                  <Gap size="xs" horizontal />
                  {t.header.login}
                </LogInLogOutButton>
              </a>
            )}
          </UserContainer>
        </MenuContainer>
      ) : null}
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;

  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

const MenuButton = styled.button`
  background: transparent;
  color: ${colors.greyscale.white};
  border: none;
  padding: 16px 18px;
  height: 100%;
  cursor: pointer;

  svg {
    width: 20px !important;
    height: 20px !important;
  }
`

const MenuContainer = styled.div`
  position: fixed;
  overflow-y: scroll;
  top: 0;
  right: 0;
  background: ${colors.blues.primary};
  box-sizing: border-box;
  width: 100vw;
  height: 100%;
  padding: calc(52px + ${defaultMargins.s}) ${defaultMargins.s}
    ${defaultMargins.s} ${defaultMargins.s};
  z-index: -1;
  display: flex;
  flex-direction: column;
`

const LanguageMenu = memo<{
  close: () => void
}>(function LanguageMenu({ close }) {
  const [lang, setLang] = useLang()

  return (
    <LangList>
      {langs.map((l) => (
        <LangListElement key={l}>
          <LangButton
            active={l === lang}
            onClick={() => {
              setLang(l)
              close()
            }}
          >
            {l}
          </LangButton>
        </LangListElement>
      ))}
    </LangList>
  )
})

const LangList = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
`

const LangListElement = styled.li`
  margin: 0 ${defaultMargins.xxs};
`

const LangButton = styled.button<{ active: boolean }>`
  background: transparent;
  color: ${colors.greyscale.white};
  padding: ${defaultMargins.xs};
  font-family: Montserrat;
  font-size: 1em;
  text-transform: uppercase;
  font-weight: ${(props) =>
    props.active ? fontWeights.bold : fontWeights.medium};
  cursor: pointer;
  border: none;
  border-bottom: 2px solid;
  border-color: ${(props) =>
    props.active ? colors.greyscale.white : 'transparent'};
`

const Navigation = memo<{
  close: () => void
  unreadMessagesCount: number
  unreadPedagogicalDocumentsCount: number
}>(function Navigation({
  close,
  unreadMessagesCount,
  unreadPedagogicalDocumentsCount
}) {
  const t = useTranslation()
  const user = useUser()

  const isEnduser = user?.userType === 'ENDUSER'
  const maybeLockElem = !isEnduser && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )
  return (
    <Nav>
      {user && (
        <>
          <StyledNavLink
            to="/applying"
            onClick={close}
            data-qa="nav-applications"
          >
            {t.header.nav.applying} {maybeLockElem}
          </StyledNavLink>
          <StyledNavLink to="/income" onClick={close} data-qa="nav-income">
            {t.header.nav.income} {maybeLockElem}
          </StyledNavLink>
          {featureFlags.pedagogicalDocumentsEnabled && (
            <StyledNavLink
              to="/pedagogical-documents"
              data-qa="nav-pedagogical-documents"
              onClick={close}
            >
              {t.header.nav.pedagogicalDocuments}{' '}
              {isEnduser && unreadPedagogicalDocumentsCount > 0 && (
                <FloatingCircledChar>
                  {unreadPedagogicalDocumentsCount}
                </FloatingCircledChar>
              )}
              {maybeLockElem}
            </StyledNavLink>
          )}
          {user.accessibleFeatures.messages && (
            <StyledNavLink
              to="/messages"
              onClick={close}
              data-qa="nav-messages"
            >
              {t.header.nav.messages}{' '}
              {unreadMessagesCount > 0 ? (
                <FloatingCircledChar>{unreadMessagesCount}</FloatingCircledChar>
              ) : (
                ''
              )}
            </StyledNavLink>
          )}
          {user.accessibleFeatures.reservations && (
            <StyledNavLink
              to="/calendar"
              onClick={close}
              data-qa="nav-calendar"
            >
              {t.header.nav.calendar}
            </StyledNavLink>
          )}
        </>
      )}
    </Nav>
  )
})

const Nav = styled.nav`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
`

const FloatingCircledChar = styled(CircledChar)`
  float: right;
`

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const StyledNavItem = (component: any) => styled(component)`
  color: inherit;
  font-family: Montserrat;
  font-weight: ${fontWeights.medium};
  text-decoration: none;
  text-transform: uppercase;
  padding: ${defaultMargins.xs} 0;
  margin: ${defaultMargins.xxs} 0;
  border-bottom: 2px solid transparent;

  &:hover {
    font-weight: ${fontWeights.bold};
    border-color: ${colors.greyscale.white};
  }

  &.active {
    font-weight: ${fontWeights.bold};
    border-color: ${colors.greyscale.white};
  }

  & > :last-child {
    margin-left: ${defaultMargins.xs};
  }
`

const StyledNavLink = StyledNavItem(NavLink)

const Spacer = styled.div`
  margin: auto 0;
`

const UserContainer = styled.div`
  display: flex;
  flex-direction: column;

  @media (min-width: ${tabletMin}) {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
`

const LogInLogOutButton = styled.button`
  background: ${colors.blues.dark};
  color: ${colors.greyscale.white};
  border: none;
  font-family: 'Open Sans', sans-serif;
  font-size: 1em;
  font-weight: ${fontWeights.semibold};
  text-transform: uppercase;
  padding: ${defaultMargins.s};
  width: 100%;

  @media (min-width: ${tabletMin}) {
    width: auto;
  }
`

const UserName = styled.span`
  font-weight: ${fontWeights.semibold};
`
