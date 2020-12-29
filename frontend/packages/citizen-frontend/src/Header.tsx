// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
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
import EspooLogo from './espoo-logo.svg'

export default React.memo(function Header() {
  return (
    <HeaderContainer>
      <LogoContainer>
        <Logo src={EspooLogo} alt="Espoo logo" />
      </LogoContainer>
      <Nav>
        <NavItem>
          <Icon icon={farMap} />
          Kartta
        </NavItem>
        <NavItem>
          <Icon icon={farFileAlt} />
          Hakemukset
        </NavItem>
        <NavItem>
          <Icon icon={farGavel} />
          Päätökset
        </NavItem>
        <NavItem>
          <Icon icon={farGavel} />
          Uusi Päätökset
        </NavItem>
      </Nav>
      <Spacer />
      <LanguageMenu />
      <LogoutButton
        href="/api/application/auth/saml/logout"
        data-qa="logout-btn"
      >
        <RoundIconBackground>
          <RoundIcon icon={faSignOut} />
        </RoundIconBackground>
        KIRJAUDU ULOS
      </LogoutButton>
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

const NavItem = styled.a`
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
`

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

const LogoutButton = styled(NavItem)`
  color: inherit;
  text-decoration: none;
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
