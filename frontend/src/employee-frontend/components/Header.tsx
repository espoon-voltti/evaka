// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import partition from 'lodash/partition'
import sum from 'lodash/sum'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled, { css, useTheme } from 'styled-components'
import { Link } from 'wouter'

import { combine } from 'lib-common/api'
import type { Action } from 'lib-common/generated/action'
import { ChipWrapper, SelectionChip } from 'lib-components/atoms/Chip'
import { EvakaLogo, evakaLogoWidth } from 'lib-components/atoms/EvakaLogo'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import NavLink, { useIsRouteActive } from 'lib-components/atoms/NavLink'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { isGroupMessageAccount } from 'lib-components/messages/types'
import { fontWeights, NavLinkText } from 'lib-components/typography'
import type { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import type { Lang } from 'lib-customizations/employee'
import {
  faChevronDown,
  faChevronUp,
  faGlobe,
  faSignOut,
  faUser
} from 'lib-icons'

import { logoutUrl } from '../api/auth'
import { I18nContext, useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'
import { hasGlobalAction } from '../utils/roles'

import { useMcpTranslation } from './mcp/translations'
import { MessageContext } from './messages/MessageContext'
import { ReportNotificationContext } from './reports/ReportNotificationContext'

export const headerHeight = '80px'

const headerGap = defaultMargins.L
const logoMargin = defaultMargins.s
const navLinkBorderWidth = 2
const navLinkCompactMargin = 8
const navLinkSpaciousMargin = 16
const denseNavFontSize = 13
const unreadCountMargin = defaultMargins.xs
const unreadCountSize = defaultMargins.m

const LogoLink = styled(Link)`
  flex: 0 1 auto;
  min-width: 70px;
  margin-left: ${logoMargin};

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
  gap: ${headerGap};
  container-type: inline-size;
`

const NavLinkWrapper = styled.div`
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  border-bottom: 4px solid transparent;
  margin: 6px ${navLinkCompactMargin}px;
  padding: 10px 0;
`

const NavbarLink = styled(NavLink)`
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  min-height: 2.5rem;
  border: ${navLinkBorderWidth}px solid transparent;
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
  margin-left: ${unreadCountMargin};
  border: 1px solid ${colors.main.m1};
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
  width: ${unreadCountSize};
  height: ${unreadCountSize};
`

const NavBarItems = styled.div`
  flex-grow: 1;
  display: flex;
  justify-content: space-between;
  gap: ${headerGap};
`
const NavLinks = styled.div`
  display: flex;
`

const UserNameButton = styled(Button)`
  border-bottom: 4px solid transparent; // align vertically with other navbar links
`

const UserIconButton = styled(IconOnlyButton)`
  display: none;
  align-self: center;
`

const UserPopupName = styled.div`
  display: none;
  font-weight: ${fontWeights.semibold};
  margin-bottom: ${defaultMargins.s};
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

const navItems = [
  'applications',
  'units',
  'search',
  'finance',
  'reports',
  'messages'
] as const
type NavItem = (typeof navItems)[number]

const navItemActions: Record<NavItem, Action.Global> = {
  applications: 'APPLICATIONS_PAGE',
  units: 'UNITS_PAGE',
  search: 'PERSON_SEARCH_PAGE',
  finance: 'FINANCE_PAGE',
  reports: 'REPORTS_PAGE',
  messages: 'MESSAGES_PAGE'
}

// Measured rendered widths (px) of the header labels at the regular nav font
// size, in the bold weight of the active link. Re-measure when a label changes.
const navLinkTextWidths: Record<Lang, Record<NavItem, number>> = {
  fi: {
    applications: 122,
    units: 78,
    search: 143,
    finance: 71,
    reports: 90,
    messages: 68
  },
  sv: {
    applications: 134,
    units: 83,
    search: 176,
    finance: 87,
    reports: 109,
    messages: 138
  }
}
// NavLinkText's font size from tabletMin up
const regularNavFontSize = 15
// A typical name; longer names wrap
const userNameWidth = 162
// IconOnlyButton with the default size
const userIconWidth = 32

const gapWidth = parseInt(headerGap)
const logoWidth = parseInt(logoMargin) + evakaLogoWidth
const navLinkCompactSpacing = 2 * (navLinkCompactMargin + navLinkBorderWidth)
const navLinkSpaciousSpacing = 2 * (navLinkSpaciousMargin + navLinkBorderWidth)
const unreadCountWidth = parseInt(unreadCountMargin) + parseInt(unreadCountSize)
const denseNavFontScale = denseNavFontSize / regularNavFontSize

interface HeaderBreakpoints {
  spacious: number
  userName: number
  regularNavFont: number
  logo: number
}

function headerBreakpoints(
  lang: Lang,
  items: NavItem[],
  unreadCountBadges: number
): HeaderBreakpoints {
  const textWidth = sum(items.map((item) => navLinkTextWidths[lang][item]))
  const compactLinksWidth = textWidth + items.length * navLinkCompactSpacing
  const fixedWidth =
    logoWidth + 2 * gapWidth + unreadCountBadges * unreadCountWidth
  return {
    spacious:
      fixedWidth +
      textWidth +
      items.length * navLinkSpaciousSpacing +
      Math.max(items.length - 1, 0) * gapWidth +
      userNameWidth,
    userName: fixedWidth + compactLinksWidth + userNameWidth,
    regularNavFont: fixedWidth + compactLinksWidth + userIconWidth,
    logo:
      fixedWidth +
      Math.ceil(textWidth * denseNavFontScale) +
      items.length * navLinkCompactSpacing +
      userIconWidth
  }
}

const HeaderWrapper = styled.header<{ $breakpoints: HeaderBreakpoints }>`
  margin-bottom: ${defaultMargins.xs};
  @media print {
    display: none;
  }

  ${({ $breakpoints: breakpoints }) => css`
    @container (width >= ${breakpoints.spacious}px) {
      ${NavLinks} {
        gap: ${headerGap};
      }
      ${NavLinkWrapper} {
        margin: 6px ${navLinkSpaciousMargin}px;
      }
    }
    @container (width < ${breakpoints.userName}px) {
      ${UserNameButton} {
        display: none;
      }
      ${UserIconButton} {
        display: flex;
      }
      ${UserPopupName} {
        display: block;
      }
    }
    @container (width < ${breakpoints.regularNavFont}px) {
      ${NavLinks} ${NavLinkText} {
        font-size: ${denseNavFontSize}px;
        letter-spacing: 0.04em;
      }
    }
    @container (width < ${breakpoints.logo}px) {
      ${LogoLink} {
        display: none;
      }
    }
  `}
`

interface HeaderContainerProps extends BaseProps {
  breakpoints: HeaderBreakpoints
  children: React.ReactNode
}

function HeaderContainer({
  'data-qa': dataQa,
  breakpoints,
  children,
  className
}: HeaderContainerProps) {
  const theme = useTheme()

  return (
    <HeaderWrapper
      data-qa={dataQa}
      className={className}
      $breakpoints={breakpoints}
    >
      <NavbarContainer>
        <LogoLink to="/">
          <EvakaLogo color={theme.colors.main.m1} />
        </LogoLink>
        {children}
      </NavbarContainer>
    </HeaderWrapper>
  )
}

export default React.memo(function Header() {
  const { i18n } = useTranslation()
  const { lang, selectLang } = useContext(I18nContext)
  const { user, loggedIn, featureConfig } = useContext(UserContext)
  const mcpTranslations = useMcpTranslation()
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
              return sum + (accountCounts?.totalUnreadCount ?? 0)
            },
            0
          )
        })
        .getOrElse(0),
    [accounts, unreadCountsByAccount]
  )

  const { childDocumentDecisionNotificationCount } = useContext(
    ReportNotificationContext
  )

  const profileIsActive = useIsRouteActive(['/profile', '/child-information'])

  const toggleUserPopup = useCallback(
    () => setPopupVisible((prev) => !prev),
    []
  )
  const closeUserPopup = useCallback(() => setPopupVisible(false), [])

  const visibleNavItems = useMemo(
    () =>
      loggedIn && user
        ? navItems.filter((item) => hasGlobalAction(user, navItemActions[item]))
        : [],
    [loggedIn, user]
  )
  const navItemUnreadCounts = useMemo<Partial<Record<NavItem, number | null>>>(
    () => ({
      reports: childDocumentDecisionNotificationCount.getOrElse(null),
      messages: unreadCount
    }),
    [childDocumentDecisionNotificationCount, unreadCount]
  )
  const breakpoints = useMemo(
    () =>
      headerBreakpoints(
        lang,
        visibleNavItems,
        visibleNavItems.filter((item) => (navItemUnreadCounts[item] ?? 0) > 0)
          .length
      ),
    [lang, visibleNavItems, navItemUnreadCounts]
  )

  return (
    <HeaderContainer data-qa="header" breakpoints={breakpoints}>
      <NavBarItems>
        {loggedIn && user && (
          <NavLinks>
            {visibleNavItems.map((item) => {
              const unread = navItemUnreadCounts[item]
              return (
                <NavbarLink
                  key={item}
                  onClick={closeUserPopup}
                  className={classNames('navbar-item is-tab', {
                    active: item === 'search' && profileIsActive
                  })}
                  to={`/${item}`}
                  data-qa={`${item}-nav`}
                >
                  <NavLinkWrapper>
                    <NavLinkText>{i18n.header[item]}</NavLinkText>
                    {unread !== undefined &&
                      unread !== null &&
                      (unread > 0 ? (
                        <UnreadCount data-qa="notifications">
                          {unread}
                        </UnreadCount>
                      ) : (
                        <span data-qa="no-notifications" />
                      ))}
                  </NavLinkWrapper>
                </NavbarLink>
              )
            })}
          </NavLinks>
        )}

        {loggedIn && user && (
          <>
            <UserNameButton
              appearance="inline"
              order="text-icon"
              data-qa="username"
              onClick={toggleUserPopup}
              text={user.name}
              icon={popupVisible ? faChevronUp : faChevronDown}
            />
            <UserIconButton
              icon={faUser}
              aria-label={user.name}
              data-qa="user-icon"
              onClick={toggleUserPopup}
            />
          </>
        )}
        {popupVisible && (
          <UserPopup>
            {user && (
              <UserPopupName data-qa="user-popup-name">
                {user.name}
              </UserPopupName>
            )}
            {featureFlags.employeeLanguageSelection && (
              <>
                <FixedSpaceRow
                  $spacing="s"
                  $alignItems="center"
                  $justifyContent="space-between"
                >
                  <FixedSpaceRow $spacing="s" $alignItems="center">
                    <FontAwesomeIcon
                      icon={faGlobe}
                      color={colors.main.m2}
                      fontSize="20px"
                    />
                    <span>{i18n.language.title}</span>
                  </FixedSpaceRow>
                  <ChipWrapper data-qa="language-selection" $margin="zero">
                    <SelectionChip
                      text="FI"
                      selected={lang === 'fi'}
                      onChange={selectLang('fi')}
                      data-qa="lang-fi"
                      translate="no"
                    />
                    <SelectionChip
                      text="SV"
                      selected={lang === 'sv'}
                      onChange={selectLang('sv')}
                      data-qa="lang-sv"
                      translate="no"
                    />
                  </ChipWrapper>
                </FixedSpaceRow>
                <HorizontalLine $slim />
              </>
            )}
            <FixedSpaceColumn $spacing="m">
              {hasGlobalAction(user, 'EMPLOYEES_PAGE') && (
                <Link
                  to="/employees"
                  onClick={closeUserPopup}
                  data-qa="user-popup-employees"
                >
                  {i18n.titles.employees}
                </Link>
              )}
              {hasGlobalAction(user, 'FINANCE_BASICS_PAGE') && (
                <Link
                  to="/finance/basics"
                  onClick={closeUserPopup}
                  data-qa="user-popup-finance-basics"
                >
                  {i18n.titles.financeBasics}
                </Link>
              )}
              {hasGlobalAction(user, 'DOCUMENT_TEMPLATES_PAGE') && (
                <Link
                  to="/document-templates"
                  onClick={closeUserPopup}
                  data-qa="user-popup-document-templates"
                >
                  {i18n.documentTemplates.title}
                </Link>
              )}
              {hasGlobalAction(user, 'WRITE_DECISION_REASONINGS') && (
                <Link
                  to="/decision-reasonings"
                  onClick={closeUserPopup}
                  data-qa="user-popup-decision-reasonings"
                >
                  {i18n.titles.decisionReasonings}
                </Link>
              )}
              {hasGlobalAction(user, 'HOLIDAY_AND_TERM_PERIODS_PAGE') && (
                <Link
                  to="/holiday-periods"
                  onClick={closeUserPopup}
                  data-qa="user-popup-holiday-periods"
                >
                  {i18n.titles.holidayAndTermPeriods}
                </Link>
              )}
              {hasGlobalAction(user, 'SETTINGS_PAGE') && (
                <Link
                  to="/settings"
                  onClick={closeUserPopup}
                  data-qa="user-popup-settings"
                >
                  {i18n.titles.settings}
                </Link>
              )}
              {hasGlobalAction(user, 'READ_SYSTEM_NOTIFICATIONS') && (
                <Link
                  to="/system-notifications"
                  onClick={closeUserPopup}
                  data-qa="user-popup-system-notifications"
                >
                  {i18n.titles.systemNotifications}
                </Link>
              )}
              {hasGlobalAction(user, 'UNIT_FEATURES_PAGE') && (
                <Link
                  to="/unit-features"
                  onClick={closeUserPopup}
                  data-qa="user-popup-unit-features"
                >
                  {i18n.titles.unitFeatures}
                </Link>
              )}
              {featureConfig?.mcpServerEnabled &&
                hasGlobalAction(user, 'MCP_PAGE') && (
                  <Link
                    to="/mcp"
                    onClick={closeUserPopup}
                    data-qa="user-popup-mcp"
                  >
                    {mcpTranslations.title}
                  </Link>
                )}
              {hasGlobalAction(user, 'PLACEMENT_TOOL') && (
                <Link
                  to="/placement-tool"
                  onClick={closeUserPopup}
                  data-qa="user-popup-pacement-tool"
                >
                  {i18n.placementTool.title}
                </Link>
              )}
              {hasGlobalAction(user, 'PERSONAL_MOBILE_DEVICE_PAGE') && (
                <Link
                  to="/personal-mobile-devices"
                  onClick={closeUserPopup}
                  data-qa="user-popup-personal-mobile-devices"
                >
                  {i18n.personalMobileDevices.title}
                </Link>
              )}
              {hasGlobalAction(user, 'PIN_CODE_PAGE') && (
                <Link
                  to="/pin-code"
                  onClick={closeUserPopup}
                  data-qa="user-popup-pin-code"
                >
                  {i18n.pinCode.link}
                </Link>
              )}
              {hasGlobalAction(user, 'OUT_OF_OFFICE_PAGE') && (
                <Link
                  to="/out-of-office"
                  onClick={closeUserPopup}
                  data-qa="user-popup-out-of-office"
                >
                  {i18n.outOfOffice.menu}
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
