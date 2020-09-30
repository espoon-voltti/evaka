// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import { NavLink, RouteComponentProps, withRouter } from 'react-router-dom'
import {
  Header as StyleguideHeader,
  NavbarItem
} from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { UserContext } from '~state/user'
import EspooLogo from '../assets/EspooLogo.png'
import { logoutUrl } from '~api/auth'
import { formatName } from '~utils'
import { RequireRole } from '~utils/roles'
import '~components/Header.scss'

const Header = React.memo(function Header({ location }: RouteComponentProps) {
  const { i18n } = useTranslation()
  const { user, loggedIn } = useContext(UserContext)

  const path = location.pathname
  const atCustomerInfo =
    path.includes('/profile') || path.includes('/child-information')

  return (
    <StyledHeader
      dataQa="header"
      title={i18n.header.title}
      logo={
        <img
          data-qa="espoo-logo"
          src={EspooLogo}
          alt="Espoo"
          className="logo"
        />
      }
    >
      {loggedIn && user && (
        <div className="navbar-start">
          <RequireRole
            oneOf={[
              'SERVICE_WORKER',
              'UNIT_SUPERVISOR',
              'FINANCE_ADMIN',
              'ADMIN'
            ]}
          >
            <NavLink
              className="navbar-item is-tab"
              activeClassName="is-active"
              to="/applications"
              data-qa="applications-nav"
            >
              {i18n.header.applications}
            </NavLink>
          </RequireRole>

          <RequireRole
            oneOf={[
              'SERVICE_WORKER',
              'UNIT_SUPERVISOR',
              'STAFF',
              'FINANCE_ADMIN',
              'ADMIN'
            ]}
          >
            <NavLink
              className="navbar-item is-tab"
              activeClassName="is-active"
              to="/units"
              data-qa="units-nav"
            >
              {i18n.header.units}
            </NavLink>
          </RequireRole>

          <RequireRole oneOf={['SERVICE_WORKER', 'FINANCE_ADMIN']}>
            <NavLink
              className={`navbar-item is-tab ${
                atCustomerInfo ? 'is-active' : ''
              }`}
              activeClassName="is-active"
              to="/search"
              data-qa="search-nav"
            >
              {i18n.header.search}
            </NavLink>
          </RequireRole>

          <RequireRole oneOf={['FINANCE_ADMIN']}>
            <>
              <NavLink
                className="navbar-item is-tab"
                activeClassName="is-active"
                to="/fee-decisions"
                data-qa="fee-decisions-nav"
              >
                {i18n.header.feeDecisions}
              </NavLink>
              <NavLink
                className="navbar-item is-tab"
                activeClassName="is-active"
                to="/invoices"
                data-qa="invoices-nav"
              >
                {i18n.header.invoices}
              </NavLink>
            </>
          </RequireRole>

          <RequireRole
            oneOf={[
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR',
              'DIRECTOR'
            ]}
          >
            <NavLink
              className="navbar-item is-tab"
              activeClassName="is-active"
              to="/reports"
              data-qa="reports-nav"
            >
              {i18n.header.reports}
            </NavLink>
          </RequireRole>
        </div>
      )}

      {loggedIn && user && (
        <NavbarItem>
          <div className="navbar-end">
            <span data-qa="username">
              {formatName(user.firstName, user.lastName, i18n)}
            </span>
            <a
              data-qa="logout-btn"
              style={{ marginLeft: '1rem' }}
              href={logoutUrl}
            >
              {i18n.header.logout}
            </a>
          </div>
        </NavbarItem>
      )}
    </StyledHeader>
  )
})

const StyledHeader = styled(StyleguideHeader)`
  @media print {
    display: none;
  }
`

export default withRouter(Header)
