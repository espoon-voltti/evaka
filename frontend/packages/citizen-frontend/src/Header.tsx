// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import colors from '@evaka/lib-components/src/colors'
import {
  faChevronDown,
  faChevronUp,
  faSignOut,
  farFileAlt,
  farGavel,
  farMap
} from '@evaka/lib-icons'
import { config } from './configs'
import EspooLogo from './espoo-logo.svg'

export default React.memo(function Header() {
  return (
    <HeaderContainer>
      <LogoContainer>
        <Logo src={EspooLogo} alt="Espoo logo" />
      </LogoContainer>
      <Nav>
        <NavItem href={config.enduserBaseUrl}>
          <Icon icon={farMap} />
          Kartta
        </NavItem>
        <NavItem href={`${config.enduserBaseUrl}/applications`}>
          <Icon icon={farFileAlt} />
          Hakemukset
        </NavItem>
        <NavItem href={`${config.enduserBaseUrl}/decisions`}>
          <Icon icon={farGavel} />
          Päätökset
        </NavItem>
        <StyledNavLink to="/decisions">
          <Icon icon={farGavel} />
          Uusi Päätökset
        </StyledNavLink>
      </Nav>
      <Spacer />
      <LanguageMenu />
      <NavItem href="/api/application/auth/saml/logout" data-qa="logout-btn">
        <RoundIconBackground>
          <RoundIcon icon={faSignOut} />
        </RoundIconBackground>
        Kirjaudu ulos
      </NavItem>
    </HeaderContainer>
  )
})

const LanguageMenu = React.memo(function LanguageMenu() {
  const [open, setOpen] = useState(false)
  const toggleOpen = useCallback(() => setOpen((state) => !state), [setOpen])

  return (
    <LanguageButton onClick={toggleOpen}>
      FI
      <LanguageIcon icon={open ? faChevronUp : faChevronDown} />
    </LanguageButton>
  )
})

const HeaderContainer = styled.header`
  color: ${colors.greyscale.white};
  background-color: ${colors.brandEspoo.espooBlue};
  position: relative;
  display: flex;
  flex-direction: row;
  height: 64px;
`

const LogoContainer = styled.div`
  flex-grow: 0;
  flex-basis: 20%;
`

const Logo = styled.img`
  padding: 0 1.5rem;
  margin-left: 1rem;
  max-width: 150px;
  width: auto;
  height: 100%;
`

const Nav = styled.nav`
  display: flex;
  flex-direction: row;
`

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const StyledNavItem = (component: any) => styled(component)`
  color: inherit;
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  text-transform: uppercase;
  min-width: 140px;
  padding: 0 16px;
  border-bottom: 4px solid transparent;

  &:hover {
    border-color: ${colors.blues.primary};
  }

  &.active {
    border-color: ${colors.brandEspoo.espooTurquoise};
  }
`

const NavItem = StyledNavItem('a')

const StyledNavLink = StyledNavItem(NavLink)

const Spacer = styled.div`
  margin: 0 auto;
`

const Icon = styled(FontAwesomeIcon)`
  height: 1rem !important;
  width: 1rem !important;
  margin-right: 10px;
`

const RoundIconBackground = styled.div`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  background: ${colors.brandEspoo.espooTurquoise};
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 100%;
  margin-right: 10px;
`

const RoundIcon = styled(Icon)`
  margin: 0;
`

const LanguageButton = styled.button`
  color: inherit;
  font-size: 1rem;
  background: transparent;
  padding: 1rem 1.2rem;
  border: none;
  border-bottom: 4px solid transparent;
  cursor: pointer;
`

const LanguageIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
  margin-left: 0.5rem;
`
