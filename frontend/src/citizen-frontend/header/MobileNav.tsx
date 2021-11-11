// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useCallback, useState } from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faBars,
  faChevronDown,
  faChevronUp,
  faCircleExclamation,
  faLockAlt,
  faSignIn,
  faSignOut,
  faTimes,
  faUser
} from 'lib-icons'
import { desktopMin } from 'lib-components/breakpoints'
import colors from 'lib-customizations/common'
import useCloseOnOutsideClick from 'lib-components/utils/useCloseOnOutsideClick'
import { Gap, defaultMargins } from 'lib-components/white-space'
import { fontWeights } from 'lib-components/typography'
import { User, useUser } from '../auth/state'
import { langs, useLang, useTranslation } from '../localization'
import { getLoginUri, getLogoutUri } from './const'
import { CircledChar } from './DesktopNav'
import AttentionIndicator from './AttentionIndicator'

type Props = {
  showMenu: boolean
  setShowMenu: Dispatch<SetStateAction<boolean>>
  unreadMessagesCount: number
  unreadPedagogicalDocumentsCount: number
}

export default React.memo(function MobileNav({
  showMenu,
  setShowMenu,
  unreadMessagesCount,
  unreadPedagogicalDocumentsCount
}: Props) {
  const user = useUser()
  const t = useTranslation()
  const ref = useCloseOnOutsideClick<HTMLDivElement>(() => setShowMenu(false))
  const toggleMenu = useCallback(
    () => setShowMenu((show) => !show),
    [setShowMenu]
  )
  const close = useCallback(() => setShowMenu(false), [setShowMenu])
  const showAttentionIndicator =
    !showMenu &&
    (unreadMessagesCount > 0 ||
      unreadPedagogicalDocumentsCount > 0 ||
      user?.email === null)

  return (
    <Container ref={ref} data-qa="mobile-nav">
      <MenuButton onClick={toggleMenu} data-qa="menu-button">
        <AttentionIndicator toggled={showAttentionIndicator}>
          <FontAwesomeIcon icon={showMenu ? faTimes : faBars} />
        </AttentionIndicator>
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
          <VerticalSpacer />
          <UserContainer>
            {user && <UserNameSubMenu user={user} close={close} />}
            <Gap size="L" />
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

const LanguageMenu = React.memo(function LanguageMenu({
  close
}: {
  close: () => void
}) {
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

const Navigation = React.memo(function Navigation({
  close,
  unreadMessagesCount,
  unreadPedagogicalDocumentsCount
}: {
  close: () => void
  unreadMessagesCount: number
  unreadPedagogicalDocumentsCount: number
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
          {user.accessibleFeatures.pedagogicalDocumentation && (
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

const StyledNavLink = styled(NavLink)`
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

const VerticalSpacer = styled.div`
  margin: auto 0;
`

const UserContainer = styled.div`
  display: flex;
  flex-direction: column;
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
`

const UserNameSubMenu = React.memo(function UserNameSubMenu({
  user,
  close
}: {
  user: User
  close: () => void
}) {
  const t = useTranslation()
  const [show, setShow] = useState(false)
  const toggleShow = useCallback(
    () => setShow((previous) => !previous),
    [setShow]
  )
  const lock = user.userType !== 'ENDUSER' && (
    <FontAwesomeIcon icon={faLockAlt} size="xs" />
  )

  return (
    <>
      <SubMenuButton onClick={toggleShow}>
        <AttentionIndicator toggled={user.email === null}>
          <FontAwesomeIcon icon={faUser} size="lg" />
        </AttentionIndicator>
        <Gap size="s" horizontal />
        <UserName>
          {user.firstName} {user.lastName}
        </UserName>
        <Gap size="s" horizontal />
        <HorizontalSpacer />
        <FontAwesomeIcon icon={show ? faChevronUp : faChevronDown} size="lg" />
      </SubMenuButton>
      {show && (
        <Nav>
          <SubMenuLink
            to="/personal-details"
            onClick={close}
            data-qa="nav-personal-details"
          >
            {t.header.nav.personalDetails}
            {user.email === null && (
              <FontAwesomeIcon icon={faCircleExclamation} size="lg" />
            )}
          </SubMenuLink>
          <SubMenuLink to="/income" onClick={close} data-qa="nav-income">
            {t.header.nav.income} {lock}
          </SubMenuLink>
        </Nav>
      )}
    </>
  )
})

const SubMenuButton = styled(MenuButton)`
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 0;
`

const HorizontalSpacer = styled.div`
  margin: 0 auto;
`

const SubMenuLink = styled(StyledNavLink)`
  margin-left: ${defaultMargins.L};
  text-transform: none;
`

const UserName = styled.span`
  font-weight: ${fontWeights.semibold};
`
