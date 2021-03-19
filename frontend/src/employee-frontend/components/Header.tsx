// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'
import { NavLink, RouteComponentProps, withRouter } from 'react-router-dom'
import classNames from 'classnames'
import colors from 'lib-components/colors'
import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'
import { cityLogo } from 'lib-customizations/employee'
import { logoutUrl } from '../api/auth'
import { RequireRole } from '../utils/roles'
import Title from 'lib-components/atoms/Title'
import { featureFlags } from '../config'

const Img = styled.img`
  color: #0050bb;
  cursor: pointer;
  user-select: none;
  box-sizing: inherit;
  height: auto;
  max-width: 100%;
  width: 150px;
`

const NavbarEnd = styled.div`
  color: #6e6e6e;

  @media screen and (min-width: 1024px) {
    line-height: 1.5;
    box-sizing: inherit;
    align-items: stretch;
    display: flex;
    justify-content: flex-end;
    margin-left: auto;
  }
`

const NavbarItem = styled.div`
  padding: 1rem 1.2rem calc(1rem + 4px);
  font-size: 15px;

  @media screen and (max-width: 1023px) {
    box-sizing: inherit;
    color: #6e6e6e;
    line-height: 1.5;
    position: relative;
    flex-grow: 0;
    flex-shrink: 0;
    align-items: center;
    display: flex;
  }
`

const LogoutLink = styled.a`
  cursor: pointer;
  text-decoration: none;
  margin-left: 1rem;
  color: ${colors.blues.medium};
`

const Header = React.memo(function Header({ location }: RouteComponentProps) {
  const { i18n } = useTranslation()
  const { user, loggedIn } = useContext(UserContext)

  const path = location.pathname
  const atCustomerInfo =
    path.includes('/profile') || path.includes('/child-information')

  const isMobileView =
    location.pathname.includes('/attendance') ||
    location.pathname.includes('/groupselector') ||
    location.pathname.includes('/mobile') ||
    location.pathname.includes('/childattendance')

  return (
    <>
      {!isMobileView && (
        <StyledHeader
          dataQa="header"
          title={i18n.header.title}
          logo={
            <Img
              data-qa="espoo-logo"
              src={cityLogo.src}
              alt={cityLogo.alt}
              className="logo"
            />
          }
        >
          {loggedIn && user && (
            <NavbarStart>
              <RequireRole
                oneOf={['SERVICE_WORKER', 'ADMIN', 'SPECIAL_EDUCATION_TEACHER']}
              >
                <NavbarLink
                  className="navbar-item is-tab"
                  to="/applications"
                  data-qa="applications-nav"
                  $noMargin
                >
                  {i18n.header.applications}
                </NavbarLink>
              </RequireRole>

              <RequireRole
                oneOf={[
                  'SERVICE_WORKER',
                  'UNIT_SUPERVISOR',
                  'STAFF',
                  'FINANCE_ADMIN',
                  'ADMIN',
                  'SPECIAL_EDUCATION_TEACHER'
                ]}
              >
                <NavbarLink
                  className="navbar-item is-tab"
                  to="/units"
                  data-qa="units-nav"
                >
                  {i18n.header.units}
                </NavbarLink>
              </RequireRole>

              <RequireRole oneOf={['SERVICE_WORKER', 'FINANCE_ADMIN']}>
                <NavbarLink
                  className={`navbar-item is-tab ${
                    atCustomerInfo ? 'is-active' : ''
                  }`}
                  to="/search"
                  data-qa="search-nav"
                >
                  {i18n.header.search}
                </NavbarLink>
              </RequireRole>

              <RequireRole oneOf={['FINANCE_ADMIN']}>
                <>
                  <NavbarLink
                    className="navbar-item is-tab"
                    to="/finance"
                    data-qa="finance-nav"
                  >
                    {i18n.header.finance}
                  </NavbarLink>
                </>
              </RequireRole>

              <RequireRole
                oneOf={[
                  'SERVICE_WORKER',
                  'FINANCE_ADMIN',
                  'UNIT_SUPERVISOR',
                  'DIRECTOR',
                  'SPECIAL_EDUCATION_TEACHER'
                ]}
              >
                <NavbarLink
                  className="navbar-item is-tab"
                  to="/reports"
                  data-qa="reports-nav"
                >
                  {i18n.header.reports}
                </NavbarLink>
              </RequireRole>

              {featureFlags.messaging && (
                <RequireRole oneOf={['UNIT_SUPERVISOR']}>
                  <NavbarLink
                    className="navbar-item is-tab"
                    to="/messages"
                    data-qa="messages-nav"
                  >
                    {i18n.header.messages}
                  </NavbarLink>
                </RequireRole>
              )}
            </NavbarStart>
          )}

          {loggedIn && user && (
            <NavbarItem>
              <NavbarEnd>
                <span data-qa="username">{user.name}</span>
                <LogoutLink
                  data-qa="logout-btn"
                  style={{ marginLeft: '1rem' }}
                  href={logoutUrl}
                >
                  {i18n.header.logout}
                </LogoutLink>
              </NavbarEnd>
            </NavbarItem>
          )}
        </StyledHeader>
      )}
    </>
  )
})

const StyledHeader = styled(Header2)`
  @media print {
    display: none;
  }
`

const HeaderWrapper = styled.header`
  padding-top: 1rem;
  background: ${colors.greyscale.white};
  box-shadow: 0 0 4px 6px hsla(0, 0%, 60.8%, 0.2);
`

const NavbarContainer = styled.nav`
  font-family: Montserrat, Arial, sans-serif;

  @media screen and (min-width: 1024px) {
    max-width: 960px;
    width: 960px;
  }

  @media screen and (max-width: 1407px) {
    .header .header-title.is-fullhd {
      max-width: 1344px;
      width: auto;
    }
  }

  @media screen and (min-width: 1216px) {
    max-width: 1152px;
    width: 1152px;
  }

  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }
  color: ${colors.greyscale.darkest};
  font-weight: 400;
  line-height: 1.5;
  box-sizing: inherit;
  margin: 0 auto;
  background-color: #fff;
  position: relative;
  display: flex;
  min-height: 3.25rem;
  flex-direction: column;
  align-items: stretch;
  font-size: 14px;
  z-index: unset;
`

const NavbarBrand = styled.div`
  box-sizing: inherit;
  display: flex;
  flex-shrink: 0;
  min-height: 3.25rem;
  align-items: center;
`

interface NavbarMenuProps {
  visible: boolean
}

const NavbarMenu = styled.div<NavbarMenuProps>`
  display: ${(p) => (p.visible ? 'block' : 'none')};

  @media screen and (min-width: 1024px) {
    display: flex;
    flex-grow: 1;
    flex-shrink: 0;
  }
`

interface NavbarLinkProps {
  $noMargin?: boolean
}

const NavbarLink = styled(NavLink)<NavbarLinkProps>`
  box-sizing: inherit;
  text-decoration: none;
  color: #6e6e6e;
  line-height: 1.5;
  position: relative;
  flex-grow: 0;
  flex-shrink: 0;
  align-items: center;
  display: flex;
  cursor: pointer;
  min-height: 3.25rem;
  padding: 1rem 1.2rem calc(1rem + 4px);
  border-bottom: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-left: ${(p) => (p.$noMargin ? '0' : '1rem')};

  &.active {
    border-bottom: 4px solid ${colors.blues.primary};
    font-weight: 700;
    padding-bottom: 1rem;
  }

  @media screen and (max-width: 1023px) {
    margin-left: 0;
  }
`

const NavbarStart = styled.div`
  @media screen and (min-width: 1024px) {
    align-items: stretch;
    display: flex;
    justify-content: flex-start;
    margin-right: auto;
  }
`

const HeaderTitle = styled.div`
  color: #0050bb;
  cursor: pointer;
  box-sizing: inherit;
  margin: 0 auto;
  position: relative;
  user-select: none;
  display: flex;
  align-items: center;
  @media screen and (min-width: 1024px) {
    max-width: 960px;
    width: 960px;
  }

  @media screen and (min-width: 1216px) {
    max-width: 1152px;
    width: 1152px;
  }

  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }
`

const HeaderTitleWrapper = styled.a`
  box-sizing: inherit;
  color: #0050bb;
  cursor: pointer;
  text-decoration: none;
`

const StyledTitle = styled(Title)`
  font-weight: 300;
  padding-top: 1.4rem;
  margin: 0;
  @media screen and (max-width: 1023px) {
    display: none;
  }
  ::before {
    content: '|';
    color: #c4c4c4;
    margin-right: 0.4em;
  }
`

const Burger = styled.a`
  box-sizing: inherit;
  text-decoration: none;
  color: #6e6e6e;
  cursor: pointer;
  display: block;
  height: 3.25rem;
  position: relative;
  width: 3.25rem;
  margin-left: auto;
  @media screen and (min-width: 1024px) {
    display: none;
  }
  span {
    background-color: currentColor;
    display: block;
    height: 1px;
    left: calc(50% - 8px);
    position: absolute;
    transform-origin: center;
    transition-duration: 86ms;
    transition-property: background-color, opacity, transform;
    transition-timing-function: ease-out;
    width: 16px;
  }
  span:nth-child(1) {
    top: calc(50% - 6px);
  }
  span:nth-child(2) {
    top: calc(50% - 1px);
  }
  span:nth-child(3) {
    top: calc(50% + 4px);
  }
`

interface Header2Props {
  title: React.ReactNode
  children?: React.ReactNode
  logo: React.ReactNode
  className?: string
  dataQa?: string
  link?: string
}

function Header2({
  dataQa,
  logo,
  title,
  children,
  link,
  className
}: Header2Props) {
  const [menuActive, setMenuActive] = useState(false)

  return (
    <HeaderWrapper data-qa={dataQa} className={className}>
      <NavbarContainer>
        <NavbarBrand>
          <HeaderTitleWrapper href={link ? link : '/'}>
            <HeaderTitle>
              {logo}
              <StyledTitle size={1}>{title}</StyledTitle>
            </HeaderTitle>
          </HeaderTitleWrapper>
          {children && (
            <Burger
              role="button"
              className={classNames({
                burger: true,
                'navbar-burger': true,
                'is-active': menuActive
              })}
              aria-label="menu"
              aria-expanded={menuActive}
              onClick={() => setMenuActive(!menuActive)}
            >
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
            </Burger>
          )}
        </NavbarBrand>
        <NavbarMenu visible={menuActive}>{children}</NavbarMenu>
      </NavbarContainer>
    </HeaderWrapper>
  )
}

export default withRouter(Header)
