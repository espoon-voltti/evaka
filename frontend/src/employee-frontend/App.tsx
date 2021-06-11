// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect,
  useParams
} from 'react-router-dom'
import ChildInformation from './/components/ChildInformation'
import StateProvider from './/state/StateProvider'
import PersonProfile from './components/PersonProfile'
import ErrorMessage from './components/common/ErrorMessage'
import UnitPage from './components/UnitPage'
import Header from './components/Header'
import Search from './components/Search'
import ensureAuthenticated from './components/ensureAuthenticated'
import LoginPage from './components/LoginPage'
import Units from './components/Units'
import ApplicationsPage from './components/applications/ApplicationsPage'
import FinanceBasicsPage from './components/finance-basics/FinanceBasicsPage'
import FinancePage from './components/FinancePage'
import InvoicePage from './components/invoice/InvoicePage'
import FeeDecisionDetailsPage from './components/fee-decision-details/FeeDecisionDetailsPage'
import EmployeesPage from './components/employees/EmployeesPage'
import EmployeePage from './components/employees/EmployeePage'
import VoucherValueDecisionPage from './components/voucher-value-decision/VoucherValueDecisionPage'
import Absences from './components/absences/Absences'
import GroupCaretakers from './components/GroupCaretakers'
import PlacementDraftPage from './components/placement-draft/PlacementDraft'
import DecisionPage from './components/decision-draft/DecisionDraft'
import Reports from './components/Reports'
import ReportDuplicatePeople from './components/reports/DuplicatePeople'
import ReportFamilyConflicts from './components/reports/FamilyConflicts'
import ReportFamilyContacts from './components/reports/FamilyContacts'
import ReportMissingHeadOfFamily from './components/reports/MissingHeadOfFamily'
import ReportMissingServiceNeed from './components/reports/MissingServiceNeed'
import ReportInvalidServiceNeed from './components/reports/InvalidServiceNeed'
import ReportPartnersInDifferentAddress from './components/reports/PartnersInDifferentAddress'
import ReportChildrenInDifferentAddress from './components/reports/ChildrenInDifferentAddress'
import ReportChildAgeLanguage from './components/reports/ChildAgeLanguage'
import ReportApplications from './components/reports/Applications'
import ReportDecisions from './components/reports/Decisions'
import ReportAssistanceNeedsAndActions from './components/reports/AssistanceNeedsAndActions'
import ReportOccupancies from './components/reports/Occupancies'
import ReportInvoices from './components/reports/Invoices'
import ReportEndedPlacements from './components/reports/EndedPlacements'
import ReportStartingPlacements from './components/reports/StartingPlacements'
import ReportPresences from './components/reports/PresenceReport'
import ReportServiceNeeds from './components/reports/ServiceNeeds'
import ReportRaw from './components/reports/Raw'
import { RouteWithTitle } from './components/RouteWithTitle'
import { useTranslation } from './state/i18n'
import { UserContext, UserContextProvider } from './state/user'
import CreateUnitPage from './components/unit/unit-details/CreateUnitPage'
import UnitDetailsPage from './components/unit/unit-details/UnitDetailsPage'
import ApplicationPage from './components/ApplicationPage'
import { hasRole } from './utils/roles'
import { getAuthStatus, AuthStatus } from './api/auth'
import VoucherServiceProviders from './components/reports/VoucherServiceProviders'
import VoucherServiceProviderUnit from './components/reports/VoucherServiceProviderUnit'
import { featureFlags } from './config'
import PlacementSketching from './components/reports/PlacementSketching'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { client } from './api/client'
import MessagesPage from './components/messages/MessagesPage'
import EmployeePinCodePage from './components/employee/EmployeePinCodePage'
import WelcomePage from './components/WelcomePage'
import VasuPage from './components/vasu/VasuPage'

export default function App() {
  const { i18n } = useTranslation()
  const [authStatus, setAuthStatus] = useState<AuthStatus>()

  useEffect(() => {
    void getAuthStatus()
      .then(setAuthStatus)
      .catch(() =>
        setAuthStatus({ loggedIn: false, user: undefined, roles: undefined })
      )
  }, [])

  useEffect(() => {
    return idleTracker(
      client,
      () => {
        void getAuthStatus()
          .then(setAuthStatus)
          .catch(() =>
            setAuthStatus({
              loggedIn: false,
              user: undefined,
              roles: undefined
            })
          )
      },
      { thresholdInMinutes: 20 }
    )
  }, [])

  if (authStatus === undefined) {
    return null
  }

  return (
    <UserContextProvider user={authStatus.user} roles={authStatus.roles}>
      <StateProvider>
        <Router basename="/employee">
          <Header />
          <Switch>
            <RouteWithTitle
              exact
              path="/login"
              component={LoginPage}
              title={i18n.titles.login}
            />
            <RouteWithTitle
              exact
              path="/units"
              component={ensureAuthenticated(Units)}
              title={i18n.titles.units}
            />
            <RouteWithTitle
              exact
              path="/units/new"
              component={ensureAuthenticated(CreateUnitPage)}
              title={i18n.titles.createUnit}
            />
            <Route
              exact
              path="/units/:id/details"
              component={ensureAuthenticated(UnitDetailsPage)}
            />
            <Route
              exact
              path="/units/:unitId/family-contacts"
              component={ensureAuthenticated(ReportFamilyContacts)}
            />
            <RouteWithTitle
              exact
              path="/units/:unitId/groups/:groupId/caretakers"
              component={ensureAuthenticated(GroupCaretakers)}
            />
            <RouteWithTitle
              path="/units/:id"
              component={ensureAuthenticated(UnitPage)}
            />
            <RouteWithTitle
              exact
              path="/search"
              component={ensureAuthenticated(Search)}
              title={i18n.titles.customers}
            />
            <RouteWithTitle
              exact
              path="/profile/:id"
              component={ensureAuthenticated(PersonProfile)}
            />
            <RouteWithTitle
              exact
              path="/child-information/:id"
              component={ensureAuthenticated(ChildInformation)}
            />
            <RouteWithTitle
              exact
              path="/applications"
              component={ensureAuthenticated(ApplicationsPage)}
              title={i18n.titles.applications}
            />
            <RouteWithTitle
              exact
              path="/applications/:id"
              component={ensureAuthenticated(ApplicationPage)}
              title={i18n.titles.applications}
            />
            <RouteWithTitle
              exact
              path="/applications/:id/placement"
              component={ensureAuthenticated(PlacementDraftPage)}
              title={i18n.titles.placementDraft}
            />
            <RouteWithTitle
              exact
              path="/applications/:id/decisions"
              component={ensureAuthenticated(DecisionPage)}
              title={i18n.titles.decision}
            />
            {featureFlags.financeBasicsPage ? (
              <Route
                exact
                path="/finance/basics"
                component={ensureAuthenticated(FinanceBasicsPage)}
              />
            ) : null}
            <Route
              exact
              path="/finance/fee-decisions/:id"
              component={ensureAuthenticated(FeeDecisionDetailsPage)}
            />
            <Route
              exact
              path="/finance/value-decisions/:id"
              component={ensureAuthenticated(VoucherValueDecisionPage)}
            />
            <Route
              exact
              path="/finance/invoices/:id"
              component={ensureAuthenticated(InvoicePage)}
            />
            <Route
              path="/finance"
              component={ensureAuthenticated(FinancePage)}
            />
            <RouteWithTitle
              exact
              path="/absences/:groupId"
              component={ensureAuthenticated(Absences)}
            />
            <RouteWithTitle
              exact
              path="/reports"
              component={ensureAuthenticated(Reports)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/family-conflicts"
              component={ensureAuthenticated(ReportFamilyConflicts)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/missing-head-of-family"
              component={ensureAuthenticated(ReportMissingHeadOfFamily)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/missing-service-need"
              component={ensureAuthenticated(ReportMissingServiceNeed)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/invalid-service-need"
              component={ensureAuthenticated(ReportInvalidServiceNeed)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/applications"
              component={ensureAuthenticated(ReportApplications)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/decisions"
              component={ensureAuthenticated(ReportDecisions)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/partners-in-different-address"
              component={ensureAuthenticated(ReportPartnersInDifferentAddress)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/children-in-different-address"
              component={ensureAuthenticated(ReportChildrenInDifferentAddress)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/child-age-language"
              component={ensureAuthenticated(ReportChildAgeLanguage)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/assistance-needs-and-actions"
              component={ensureAuthenticated(ReportAssistanceNeedsAndActions)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/occupancies"
              component={ensureAuthenticated(ReportOccupancies)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/invoices"
              component={ensureAuthenticated(ReportInvoices)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/starting-placements"
              component={ensureAuthenticated(ReportStartingPlacements)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/ended-placements"
              component={ensureAuthenticated(ReportEndedPlacements)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/duplicate-people"
              component={ensureAuthenticated(ReportDuplicatePeople)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/presences"
              component={ensureAuthenticated(ReportPresences)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/service-needs"
              component={ensureAuthenticated(ReportServiceNeeds)}
              title={i18n.titles.reports}
            />
            {featureFlags.voucherValueDecisionsPage && (
              <RouteWithTitle
                exact
                path="/reports/voucher-service-providers"
                component={ensureAuthenticated(VoucherServiceProviders)}
                title={i18n.titles.reports}
              />
            )}
            {featureFlags.voucherValueDecisionsPage && (
              <RouteWithTitle
                exact
                path="/reports/voucher-service-providers/:unitId"
                component={ensureAuthenticated(VoucherServiceProviderUnit)}
                title={i18n.titles.reports}
              />
            )}
            {featureFlags.messaging && (
              <RouteWithTitle
                exact
                path="/messages"
                component={ensureAuthenticated(MessagesPage)}
                title={i18n.titles.messages}
              />
            )}
            <RouteWithTitle
              exact
              path="/reports/placement-sketching"
              component={ensureAuthenticated(PlacementSketching)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/raw"
              component={ensureAuthenticated(ReportRaw)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/pin-code"
              component={ensureAuthenticated(EmployeePinCodePage)}
              title={i18n.titles.employeePinCode}
            />
            <RouteWithTitle
              exact
              path="/employees"
              component={ensureAuthenticated(EmployeesPage)}
              title={i18n.employees.title}
            />
            <RouteWithTitle
              exact
              path="/employees/:id"
              component={ensureAuthenticated(EmployeePage)}
              title={i18n.employees.title}
            />
            <RouteWithTitle
              exact
              path="/welcome"
              component={ensureAuthenticated(WelcomePage)}
              title={i18n.titles.welcomePage}
            />
            <RouteWithTitle
              exact
              path="/vasu/:id"
              component={ensureAuthenticated(VasuPage)}
              title={i18n.titles.vasuPage}
            />
            {redirectRoutes([
              {
                from: '/fee-decisions',
                to: () => `/finance/fee-decisions`
              },
              {
                from: '/fee-decisions/:id',
                to: ({ id }) => `/finance/fee-decisions/${id}`
              },
              {
                from: '/invoices',
                to: () => `/finance/invoices`
              },
              {
                from: '/invoices/:id',
                to: ({ id }) => `/finance/invoices/${id}`
              }
            ])}
            <Route exact path="/" component={RedirectToMainPage} />
          </Switch>
          <ErrorMessage />
        </Router>
      </StateProvider>
    </UserContextProvider>
  )
}

function RedirectToMainPage() {
  const { loggedIn, roles } = useContext(UserContext)

  if (!loggedIn) {
    return <Redirect to={'/login'} />
  }

  if (
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'SPECIAL_EDUCATION_TEACHER')
  ) {
    return <Redirect to={'/applications'} />
  } else if (hasRole(roles, 'UNIT_SUPERVISOR') || hasRole(roles, 'STAFF')) {
    return <Redirect to={'/units'} />
  } else if (hasRole(roles, 'DIRECTOR')) {
    return <Redirect to={'/reports'} />
  } else if (roles.length === 0) {
    return <Redirect to={'/welcome'} />
  } else {
    return <Redirect to={'/search'} />
  }
}

function redirectRoutes(
  routes: Array<{
    from: string
    to: (params: { [k: string]: string }) => string
  }>
) {
  return routes.map(({ from, to }) => (
    <Route key={from} exact path={from} component={redirectTo(to)} />
  ))
}

const redirectTo = (urlMapper: (params: { [k: string]: string }) => string) =>
  function RedirectTo() {
    const routeParams = useParams()
    return <Redirect to={urlMapper(routeParams)} />
  }
