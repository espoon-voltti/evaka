// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import partition from 'lodash/partition'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { combine } from 'lib-common/api'
import { EvakaLogo } from 'lib-components/atoms/EvakaLogo'
import { Button } from 'lib-components/atoms/buttons/Button'
import { desktopMin } from 'lib-components/breakpoints'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { isGroupMessageAccount } from 'lib-components/messages/types'
import { fontWeights, NavLinkText } from 'lib-components/typography'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronDown, faChevronUp, faSignOut } from 'lib-icons'

import { logoutUrl } from '../api/auth'
import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'

import { MessageContext } from './messages/MessageContext'
import { AssistanceNeedDecisionReportContext } from './reports/AssistanceNeedDecisionReportContext'

export const headerHeight = '80px'

const HeaderWrapper = styled.header`
  margin-bottom: ${defaultMargins.xs};
  @media print {
    display: none;
  }
`

const LogoLink = styled(Link)`
  display: none;
  @media screen and (min-width: ${desktopMin}) {
    display: block;
    margin-left: ${defaultMargins.s};
    margin-right: ${defaultMargins.L};
  }

  > svg {
    width: 100%;
    height: 100%;
    max-width: 120px;
  }
`

const NavbarContainer = styled.nav`
  margin: 0 auto;
  padding: 0 ${defaultMargins.s};
  min-height: ${headerHeight};

  @media screen and (min-width: 1216px) {
    max-width: 1152px;
    width: 1152px;
  }
  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }

  position: relative;
  display: flex;
  align-items: center;
  gap: ${defaultMargins.L};
`

interface HeaderContainerProps extends BaseProps {
  children: React.ReactNode
}

function HeaderContainer({
  'data-qa': dataQa,
  children,
  className
}: HeaderContainerProps) {
  const theme = useTheme()

  return (
    <HeaderWrapper data-qa={dataQa} className={className}>
      <NavbarContainer>
        <LogoLink to="/">
          <EvakaLogo color={theme.colors.main.m1} />
        </LogoLink>
        {children}
      </NavbarContainer>
    </HeaderWrapper>
  )
}

const NavLinkWrapper = styled.div`
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  border-bottom: 4px solid transparent;
  margin: 6px 16px;
  padding: 10px 0;
`

const NavbarLink = styled(NavLink)`
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  min-height: 2.5rem;
  border: 2px solid transparent;
  border-radius: 2px;

  &.active {
    ${NavLinkWrapper} {
      border-bottom: 4px solid ${(p) => p.theme.colors.main.m2};
    }
    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.m2};
      font-weight: ${fontWeights.bold};
    }
  }
  &:focus {
    border-color: ${(p) => p.theme.colors.main.m3};
  }
  :hover {
    ${NavLinkText} {
      color: ${(p) => p.theme.colors.main.m2Hover};
    }
  }
`

const LogoutLink = styled.a`
  cursor: pointer;
  text-decoration: none;
  color: ${colors.main.m1};
  display: flex;
  align-items: center;
  gap: ${defaultMargins.m};
`

const UnreadCount = styled.span`
  color: ${colors.main.m1};
  font-weight: ${fontWeights.medium};
  margin-left: ${defaultMargins.xs};
  border: 1px solid ${colors.main.m1};
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
  width: ${defaultMargins.m};
  height: ${defaultMargins.m};
`

const NavBarItems = styled.div`
  flex-grow: 1;
  display: flex;
  justify-content: space-between;
  gap: ${defaultMargins.L};
`
const NavLinks = styled.div`
  display: flex;
  @media screen and (min-width: 1216px) {
    gap: ${defaultMargins.L};
  }
`

const NavbarButton = styled(Button)`
  border-bottom: 4px solid transparent; // align vertically with other navbar links
`

const UserPopup = styled.div`
  position: absolute;
  width: 320px;
  right: 0;
  top: ${headerHeight};
  z-index: 50;
  padding: 24px 16px;
  background: ${colors.grayscale.g0};
  box-shadow: 0 4px 4px rgba(15, 15, 15, 0.25);

  a {
    color: ${colors.grayscale.g100};
  }
`

export default React.memo(function Header() {
  const location = useLocation()
  const { i18n } = useTranslation()
  const { user, loggedIn } = useContext(UserContext)
  const { accounts, unreadCountsByAccount } = useContext(MessageContext)
  const [popupVisible, setPopupVisible] = useState(false)

  const unreadCount = useMemo<number>(
    () =>
      combine(accounts, unreadCountsByAccount)
        .map(([allAccounts, counts]) => {
          const [group, personal] = partition(
            allAccounts,
            isGroupMessageAccount
          )
          return (personal.length > 0 ? personal : group).reduce(
            (sum, { account: { id: accountId } }) => {
              const accountCounts = counts.find(
                (c) => c.accountId === accountId
              )
              return (
                sum +
                (accountCounts?.unreadCount ?? 0) +
                (accountCounts?.unreadCopyCount ?? 0)
              )
            },
            0
          )
        })
        .getOrElse(0),
    [accounts, unreadCountsByAccount]
  )

  const { assistanceNeedDecisionCounts } = useContext(
    AssistanceNeedDecisionReportContext
  )

  const path = location.pathname
  const atCustomerInfo =
    path.includes('/profile') || path.includes('/child-information')

  const toggleUserPopup = useCallback(
    () => setPopupVisible((prev) => !prev),
    []
  )
  const closeUserPopup = useCallback(() => setPopupVisible(false), [])

  return (
    <HeaderContainer data-qa="header">
      <NavBarItems>
        {loggedIn && user && (
          <NavLinks>
            {user.accessibleFeatures.applications && (
              <NavbarLink
                onClick={closeUserPopup}
                className="navbar-item is-tab"
                to="/applications"
                data-qa="applications-nav"
              >
                <NavLinkWrapper>
                  <NavLinkText>{i18n.header.applications}</NavLinkText>
                </NavLinkWrapper>
              </NavbarLink>
            )}

            {user.accessibleFeatures.units && (
              <NavbarLink
                onClick={closeUserPopup}
                className="navbar-item is-tab"
                to="/units"
                data-qa="units-nav"
              >
                <NavLinkWrapper>
                  <NavLinkText>{i18n.header.units}</NavLinkText>
                </NavLinkWrapper>
              </NavbarLink>
            )}

            {user.accessibleFeatures.personSearch && (
              <NavbarLink
                onClick={closeUserPopup}
                className={classNames('navbar-item is-tab', {
                  active: atCustomerInfo
                })}
                to="/search"
                data-qa="search-nav"
              >
                <NavLinkWrapper>
                  <NavLinkText>{i18n.header.search}</NavLinkText>
                </NavLinkWrapper>
              </NavbarLink>
            )}

            {user.accessibleFeatures.finance && (
              <NavbarLink
                onClick={closeUserPopup}
                className="navbar-item is-tab"
                to="/finance"
                data-qa="finance-nav"
              >
                <NavLinkWrapper>
                  <NavLinkText>{i18n.header.finance}</NavLinkText>
                </NavLinkWrapper>
              </NavbarLink>
            )}

            {user.accessibleFeatures.reports && (
              <NavbarLink
                onClick={closeUserPopup}
                className="navbar-item is-tab"
                to="/reports"
                data-qa="reports-nav"
              >
                <NavLinkWrapper>
                  <NavLinkText>{i18n.header.reports}</NavLinkText>
                  {assistanceNeedDecisionCounts
                    .map(
                      (unread) =>
                        unread > 0 && (
                          <UnreadCount key="unread">{unread}</UnreadCount>
                        )
                    )
                    .getOrElse(null)}
                </NavLinkWrapper>
              </NavbarLink>
            )}

            {user.accessibleFeatures.messages && (
              <NavbarLink
                onClick={closeUserPopup}
                className="navbar-item is-tab"
                to="/messages"
                data-qa="messages-nav"
              >
                <NavLinkWrapper>
                  <NavLinkText>{i18n.header.messages} </NavLinkText>
                  {unreadCount > 0 && <UnreadCount>{unreadCount}</UnreadCount>}
                </NavLinkWrapper>
              </NavbarLink>
            )}
          </NavLinks>
        )}

        {loggedIn && user && (
          <NavbarButton
            appearance="inline"
            order="text-icon"
            data-qa="username"
            onClick={toggleUserPopup}
            text={user.name}
            icon={popupVisible ? faChevronUp : faChevronDown}
          />
        )}
        {popupVisible && (
          <UserPopup>
            <FixedSpaceColumn spacing="m">
              {user?.accessibleFeatures.employees && (
                <Link
                  to="/employees"
                  onClick={closeUserPopup}
                  data-qa="user-popup-employees"
                >
                  {i18n.titles.employees}
                </Link>
              )}
              {user?.accessibleFeatures.financeBasics && (
                <Link
                  to="/finance/basics"
                  onClick={closeUserPopup}
                  data-qa="user-popup-finance-basics"
                >
                  {i18n.titles.financeBasics}
                </Link>
              )}
              {user?.accessibleFeatures.documentTemplates && (
                <Link
                  to="/document-templates"
                  onClick={closeUserPopup}
                  data-qa="user-popup-document-templates"
                >
                  {i18n.documentTemplates.title}
                </Link>
              )}
              {user?.accessibleFeatures.vasuTemplates && (
                <Link
                  to="/vasu-templates"
                  onClick={closeUserPopup}
                  data-qa="user-popup-vasu-templates"
                >
                  {i18n.vasuTemplates.title}
                </Link>
              )}
              {user?.accessibleFeatures.holidayAndTermPeriods && (
                <Link
                  to="/holiday-periods"
                  onClick={closeUserPopup}
                  data-qa="user-popup-holiday-periods"
                >
                  {i18n.titles.holidayAndTermPeriods}
                </Link>
              )}
              {user?.accessibleFeatures.settings && (
                <Link
                  to="/settings"
                  onClick={closeUserPopup}
                  data-qa="user-popup-settings"
                >
                  {i18n.titles.settings}
                </Link>
              )}
              {user?.accessibleFeatures.systemNotifications && (
                <Link
                  to="/system-notifications"
                  onClick={closeUserPopup}
                  data-qa="user-popup-system-notifications"
                >
                  {i18n.titles.systemNotifications}
                </Link>
              )}
              {user?.accessibleFeatures.unitFeatures && (
                <Link
                  to="/unit-features"
                  onClick={closeUserPopup}
                  data-qa="user-popup-unit-features"
                >
                  {i18n.titles.unitFeatures}
                </Link>
              )}
              {user?.accessibleFeatures.placementTool && (
                <Link
                  to="/placement-tool"
                  onClick={closeUserPopup}
                  data-qa="user-popup-pacement-tool"
                >
                  {i18n.placementTool.title}
                </Link>
              )}
              {user?.accessibleFeatures.personalMobileDevice && (
                <Link
                  to="/personal-mobile-devices"
                  onClick={closeUserPopup}
                  data-qa="user-popup-personal-mobile-devices"
                >
                  {i18n.personalMobileDevices.title}
                </Link>
              )}
              {user?.accessibleFeatures.pinCode && (
                <Link
                  to="/pin-code"
                  onClick={closeUserPopup}
                  data-qa="user-popup-pin-code"
                >
                  {i18n.pinCode.link}
                </Link>
              )}

              <Link
                to="/preferred-first-name"
                onClick={closeUserPopup}
                data-qa="user-popup-preferred-first-name"
              >
                {i18n.preferredFirstName.popupLink}
              </Link>

              <LogoutLink
                data-qa="logout-btn"
                href={logoutUrl}
                onClick={closeUserPopup}
              >
                <span>{i18n.header.logout}</span>
                <FontAwesomeIcon icon={faSignOut} />
              </LogoutLink>
            </FixedSpaceColumn>
          </UserPopup>
        )}
      </NavBarItems>
    </HeaderContainer>
  )
})
