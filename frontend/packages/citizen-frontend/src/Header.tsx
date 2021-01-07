// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import colors from '@evaka/lib-components/src/colors'
import useCloseOnOutsideClick from '@evaka/lib-components/src/utils/useCloseOnOutsideClick'
import {
  faCheck,
  faChevronDown,
  faChevronUp,
  faSignOut,
  farFileAlt,
  farGavel,
  farMap
} from '@evaka/lib-icons'
import { useUser } from './auth'
import { Lang, useLang, useTranslation } from './localization'
import EspooLogo from './espoo-logo.svg'

const enduserBaseUrl =
  window.location.host === 'localhost:9094' ? 'http://localhost:9091' : ''

export default React.memo(function Header() {
  const user = useUser()
  const t = useTranslation()

  return (
    <HeaderContainer>
      <LogoContainer>
        <Logo src={EspooLogo} alt="Espoo logo" />
      </LogoContainer>
      <Nav>
        <NavItem href={enduserBaseUrl} data-qa={'nav-old-map'}>
          <Icon icon={farMap} />
          {t.header.nav.map}
        </NavItem>
        <NavItem
          href={`${enduserBaseUrl}/applications`}
          data-qa={'nav-old-applications'}
        >
          <Icon icon={farFileAlt} />
          {t.header.nav.applications}
        </NavItem>
        <NavItem
          href={`${enduserBaseUrl}/decisions`}
          data-qa={'nav-old-decisions'}
        >
          <Icon icon={farGavel} />
          {t.header.nav.decisions}
        </NavItem>
        <StyledNavLink to="/decisions" data-qa={'nav-decisions'}>
          <Icon icon={farGavel} />
          {t.header.nav.newDecisions}
        </StyledNavLink>
      </Nav>
      <Spacer />
      <LanguageMenu />
      <NavItem href="/api/application/auth/saml/logout" data-qa="logout-btn">
        <RoundIconBackground>
          <RoundIcon icon={faSignOut} />
        </RoundIconBackground>
        <LogoutText>
          {user ? (
            <UserName>
              {user.firstName} {user.lastName}
            </UserName>
          ) : null}
          <span>{t.header.logout}</span>
        </LogoutText>
      </NavItem>
    </HeaderContainer>
  )
})

const langs: Lang[] = ['fi', 'sv', 'en']

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

const HeaderContainer = styled.header`
  z-index: 10;
  color: ${colors.greyscale.white};
  background-color: ${colors.brandEspoo.espooBlue};
  position: relative;
  display: flex;
  flex-direction: row;
  height: 64px;
`

const LogoContainer = styled.div`
  flex-grow: 0;
  flex-shrink: 1;
  flex-basis: 20%;
  min-width: 180px;
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
  min-width: 2.5rem;
  min-height: 2.5rem;
  border-radius: 100%;
  margin-right: 10px;
`

const RoundIcon = styled(Icon)`
  margin: 0;
`

const LogoutText = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
`

const UserName = styled.span`
  text-transform: none;
  white-space: nowrap;
`

const LanguageButton = styled.button`
  color: inherit;
  text-transform: uppercase;
  font-size: 1rem;
  background: transparent;
  height: 100%;
  padding: 1rem 1.2rem;
  border: none;
  border-bottom: 4px solid transparent;
  cursor: pointer;
  white-space: nowrap;
`

const LanguageIcon = styled(FontAwesomeIcon)`
  height: 1em !important;
  width: 0.625em !important;
  margin-left: 0.5rem;
`

const LanguageDropDown = styled.ul`
  position: absolute;
  top: 64px;
  list-style: none;
  margin: 0;
  padding: 0;
  background: ${colors.greyscale.white};
  box-shadow: 0 2px 6px 0 ${colors.greyscale.lighter};
`

const LanguageListElement = styled.li`
  display: block;
  width: 10.5rem;
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
  margin-left: 0.5rem;
`
