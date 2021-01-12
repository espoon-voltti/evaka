// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faBars, faSignOut, faTimes } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import useCloseOnOutsideClick from '@evaka/lib-components/src/utils/useCloseOnOutsideClick'
import { Gap, defaultMargins } from '@evaka/lib-components/src/white-space'
import { tabletMin } from '@evaka/lib-components/src/breakpoints'
import { useUser } from '../auth'
import { langs, useLang, useTranslation } from '../localization'

type Props = {
  enduserBaseUrl: string
}

export default React.memo(function DesktopNav({ enduserBaseUrl }: Props) {
  const user = useUser()
  const t = useTranslation()
  const [showMenu, setShowMenu] = useState(false)
  const ref = useCloseOnOutsideClick<HTMLDivElement>(() => setShowMenu(false))
  const toggleMenu = useCallback(() => setShowMenu((show) => !show), [
    setShowMenu
  ])
  const close = useCallback(() => setShowMenu(false), [setShowMenu])

  return (
    <div ref={ref}>
      <MenuButton>
        <FontAwesomeIcon
          icon={showMenu ? faTimes : faBars}
          onClick={toggleMenu}
        />
      </MenuButton>
      {showMenu ? (
        <MenuContainer>
          <LanguageMenu close={close} />
          <Gap size="s" />
          <Navigation enduserBaseUrl={enduserBaseUrl} />
          <Spacer />
          <UserContainer>
            <UserName>{`${user?.firstName ?? ''} ${
              user?.lastName ?? ''
            }`}</UserName>
            <Gap size="s" />
            <LogoutButton>
              <FontAwesomeIcon icon={faSignOut} size="lg" />
              <Gap size="xs" horizontal />
              {t.header.logout}
            </LogoutButton>
          </UserContainer>
        </MenuContainer>
      ) : null}
    </div>
  )
})

const MenuButton = styled.button`
  background: transparent;
  color: ${colors.greyscale.white};
  border: none;
  padding: 1rem 1.2rem;
  height: 100%;
  cursor: pointer;

  svg {
    width: 20px !important;
    height: 20px !important;
  }
`

const MenuContainer = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  background: ${colors.primaryColors.medium};
  box-sizing: border-box;
  width: 100vw;
  height: 100vh;
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
  font-weight: ${(props) => (props.active ? 700 : 500)};
  cursor: pointer;
  border: none;
  border-bottom: 2px solid;
  border-color: ${(props) =>
    props.active ? colors.greyscale.white : 'transparent'};
`

const Navigation = React.memo(function Navigation({
  enduserBaseUrl
}: {
  enduserBaseUrl: string
}) {
  const t = useTranslation()

  return (
    <Nav>
      <NavItem href={enduserBaseUrl} data-qa={'nav-old-map'}>
        {t.header.nav.map}
      </NavItem>
      <NavItem
        href={`${enduserBaseUrl}/applications`}
        data-qa={'nav-old-applications'}
      >
        {t.header.nav.applications}
      </NavItem>
      <NavItem
        href={`${enduserBaseUrl}/decisions`}
        data-qa={'nav-old-decisions'}
      >
        {t.header.nav.decisions}
      </NavItem>
      <StyledNavLink to="/decisions">{t.header.nav.newDecisions}</StyledNavLink>
    </Nav>
  )
})

const Nav = styled.nav`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
`

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const StyledNavItem = (component: any) => styled(component)`
  color: inherit;
  font-family: Montserrat;
  font-weight: 500;
  text-decoration: none;
  text-transform: uppercase;
  padding: ${defaultMargins.xs} 0;
  margin: ${defaultMargins.xxs} 0;
  border-bottom: 2px solid transparent;

  &:hover {
    font-weight: 700;
    border-color: ${colors.greyscale.white};
  }

  &.active {
    font-weight: 700;
    border-color: ${colors.greyscale.white};
  }
`

const NavItem = StyledNavItem('a')

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

const LogoutButton = styled.button`
  background: ${colors.primaryColors.dark};
  color: ${colors.greyscale.white};
  border: none;
  font-family: Open Sans;
  font-size: 1em;
  font-weight: 600;
  text-transform: uppercase;
  padding: ${defaultMargins.s};
  width: 100%;

  @media (min-width: ${tabletMin}) {
    width: auto;
  }
`

const UserName = styled.span`
  font-weight: 600;
`
