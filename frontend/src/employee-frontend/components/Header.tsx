// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import { NestedMessageAccount } from 'lib-common/generated/api-types/messaging'
import { fontWeights } from 'lib-components/typography'
import { isNestedGroupMessageAccount } from './messages/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Title from 'lib-components/atoms/Title'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { cityLogo, featureFlags } from 'lib-customizations/employee'
import { faChevronDown, faChevronUp, faSignOut } from 'lib-icons'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import {
  Link,
  NavLink,
  RouteComponentProps,
  withRouter
} from 'react-router-dom'
import styled from 'styled-components'
import { logoutUrl } from '../api/auth'
import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'
import { MessageContext } from './messages/MessageContext'

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
  color: ${colors.blues.medium};
`

const UnreadCount = styled.span`
  color: ${colors.blues.medium};
  font-weight: ${fontWeights.medium};
  margin-left: ${defaultMargins.xs};
  border: 1px solid ${colors.blues.medium};
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
  width: ${defaultMargins.m};
  height: ${defaultMargins.m};
`

const Header = React.memo(function Header({ location }: RouteComponentProps) {
  const { i18n } = useTranslation()
  const { user, loggedIn } = useContext(UserContext)
  const { nestedAccounts, unreadCountsByAccount } = useContext(MessageContext)
  const [popupVisible, setPopupVisible] = useState(false)

  const unreadCount = useMemo<number>(() => {
    if (nestedAccounts.isSuccess && unreadCountsByAccount.isSuccess) {
      const countUnreads = (nestedAccounts: NestedMessageAccount[]) =>
        nestedAccounts.reduce<number>(
          (sum, account) =>
            (
              unreadCountsByAccount.value.find(
                (x) => x.accountId === account.account.id
              ) || { unreadCount: 0 }
            ).unreadCount + sum,
          0
        )
      if (
        nestedAccounts.value.find((acc) => !isNestedGroupMessageAccount(acc))
      ) {
        return countUnreads(
          nestedAccounts.value.filter(
            (acc) => !isNestedGroupMessageAccount(acc)
          )
        )
      } else {
        return countUnreads(
          nestedAccounts.value.filter(isNestedGroupMessageAccount)
        )
      }
    } else {
      return 0
    }
  }, [nestedAccounts, unreadCountsByAccount])

  const path = location.pathname
  const atCustomerInfo =
    path.includes('/profile') || path.includes('/child-information')

  const toggleUserPopup = useCallback(
    () => setPopupVisible((prev) => !prev),
    []
  )
  const closeUserPopup = useCallback(() => setPopupVisible(false), [])

  return (
    <StyledHeader
      data-qa="header"
      title={i18n.header.title}
      logo={
        <Img
          data-qa="city-logo"
          src={cityLogo.src}
          alt={cityLogo.alt}
          className="logo"
        />
      }
    >
      {loggedIn && user && (
        <NavbarStart>
          {user.accessibleFeatures.applications && (
            <NavbarLink
              onClick={closeUserPopup}
              className="navbar-item is-tab"
              to="/applications"
              data-qa="applications-nav"
              $noMargin
            >
              {i18n.header.applications}
            </NavbarLink>
          )}

          {user.accessibleFeatures.units && (
            <NavbarLink
              onClick={closeUserPopup}
              className="navbar-item is-tab"
              to="/units"
              data-qa="units-nav"
            >
              {i18n.header.units}
            </NavbarLink>
          )}

          {user.accessibleFeatures.personSearch && (
            <NavbarLink
              onClick={closeUserPopup}
              className={classNames('navbar-item is-tab', {
                'is-active': atCustomerInfo
              })}
              to="/search"
              data-qa="search-nav"
            >
              {i18n.header.search}
            </NavbarLink>
          )}

          {user.accessibleFeatures.finance && (
            <NavbarLink
              onClick={closeUserPopup}
              className="navbar-item is-tab"
              to="/finance"
              data-qa="finance-nav"
            >
              {i18n.header.finance}
            </NavbarLink>
          )}

          {user.accessibleFeatures.reports && (
            <NavbarLink
              onClick={closeUserPopup}
              className="navbar-item is-tab"
              to="/reports"
              data-qa="reports-nav"
            >
              {i18n.header.reports}
            </NavbarLink>
          )}

          {user.accessibleFeatures.messages && (
            <NavbarLink
              onClick={closeUserPopup}
              className="navbar-item is-tab"
              to="/messages"
              data-qa="messages-nav"
            >
              {i18n.header.messages}
              {unreadCount > 0 && <UnreadCount>{unreadCount}</UnreadCount>}
            </NavbarLink>
          )}
        </NavbarStart>
      )}

      {loggedIn && user && (
        <NavbarItem>
          <NavbarEnd>
            <InlineButton
              data-qa="username"
              onClick={toggleUserPopup}
              text={user.name}
              iconRight
              icon={popupVisible ? faChevronUp : faChevronDown}
            />
          </NavbarEnd>
        </NavbarItem>
      )}
      {popupVisible && (
        <UserPopup>
          <FixedSpaceColumn spacing={'m'}>
            {user?.accessibleFeatures.employees && (
              <Link
                to={`/employees`}
                onClick={closeUserPopup}
                data-qa="user-popup-employees"
              >
                {i18n.employees.title}
              </Link>
            )}
            {featureFlags.financeBasicsPage &&
              user?.accessibleFeatures.financeBasics && (
                <Link
                  to="/finance/basics"
                  onClick={closeUserPopup}
                  data-qa="user-popup-finance-basics"
                >
                  {i18n.financeBasics.title}
                </Link>
              )}
            {featureFlags.experimental?.vasu &&
              user?.accessibleFeatures.vasuTemplates && (
                <Link
                  to="/vasu-templates"
                  onClick={closeUserPopup}
                  data-qa="user-popup-vasu-templates"
                >
                  {i18n.vasuTemplates.title}
                </Link>
              )}
            {user?.accessibleFeatures.settings && (
              <Link
                to="/settings"
                onClick={closeUserPopup}
                data-qa="user-popup-settings"
              >
                {i18n.settings.title}
              </Link>
            )}
            {user?.accessibleFeatures.unitFeatures && (
              <Link
                to="/unit-features"
                onClick={closeUserPopup}
                data-qa="user-popup-unit-features"
              >
                {i18n.unitFeatures.title}
              </Link>
            )}
            <Link
              to={`/pin-code`}
              onClick={closeUserPopup}
              data-qa="user-popup-pin-code"
            >
              {i18n.pinCode.link}
            </Link>
            <LogoutLink
              data-qa="logout-btn"
              href={logoutUrl}
              onClick={closeUserPopup}
            >
              <LogoutText>{i18n.header.logout}</LogoutText>
              <FontAwesomeIcon icon={faSignOut} />
            </LogoutLink>
          </FixedSpaceColumn>
        </UserPopup>
      )}
    </StyledHeader>
  )
})

const StyledHeader = styled(Header2)`
  @media print {
    display: none;
  }
`

const LogoutText = styled.span`
  margin-right: 16px;
`

const UserPopup = styled.div`
  font-size: 16px;
  line-height: 24px;
  font-family: 'Open Sans', sans-serif;
  position: absolute;
  width: 320px;
  right: 0px;
  top: 148px;
  z-index: 5;
  padding: 24px 16px;
  background: ${colors.greyscale.white};
  box-shadow: 0px 4px 4px rgba(15, 15, 15, 0.25);

  a {
    color: ${colors.greyscale.darkest};
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
  font-weight: ${fontWeights.normal};
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
    font-weight: ${fontWeights.bold};
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
  font-weight: ${fontWeights.light};
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
  'data-qa'?: string
  link?: string
}

function Header2({
  'data-qa': dataQa,
  logo,
  title,
  children,
  link,
  className
}: Header2Props) {
  const [menuActive, setMenuActive] = useState(false)
  const toggleMenu = useCallback(() => setMenuActive((prev) => !prev), [])

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
              onClick={toggleMenu}
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
