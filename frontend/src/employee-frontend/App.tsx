// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Redirect,
  Switch,
  useParams
} from 'react-router-dom'
import { ThemeProvider } from 'styled-components'

import { AuthStatus, User } from 'lib-common/api-types/employee-auth'
import { idleTracker } from 'lib-common/utils/idleTracker'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import ReloadNotification from 'lib-components/molecules/ReloadNotification'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'

import { getAuthStatus } from './api/auth'
import { client } from './api/client'
import ApplicationPage from './components/ApplicationPage'
import ChildInformation from './components/ChildInformation'
import EmployeeRoute from './components/EmployeeRoute'
import FinancePage from './components/FinancePage'
import { Footer } from './components/Footer'
import GroupCaretakers from './components/GroupCaretakers'
import Header from './components/Header'
import IncomeStatementPage from './components/IncomeStatementPage'
import LoginPage from './components/LoginPage'
import MobilePairingModal from './components/MobilePairingModal'
import PersonProfile from './components/PersonProfile'
import PersonalMobileDevicesPage from './components/PersonalMobileDevicesPage'
import Reports from './components/Reports'
import Search from './components/Search'
import SettingsPage from './components/SettingsPage'
import UnitFeaturesPage from './components/UnitFeaturesPage'
import UnitPage from './components/UnitPage'
import Units from './components/Units'
import WelcomePage from './components/WelcomePage'
import AIPage from './components/ai/AIPage'
import ApplicationsPage from './components/applications/ApplicationsPage'
import ErrorMessage from './components/common/ErrorMessage'
import DecisionPage from './components/decision-draft/DecisionDraft'
import EmployeePinCodePage from './components/employee/EmployeePinCodePage'
import EmployeePage from './components/employees/EmployeePage'
import EmployeesPage from './components/employees/EmployeesPage'
import FeeDecisionDetailsPage from './components/fee-decision-details/FeeDecisionDetailsPage'
import FinanceBasicsPage from './components/finance-basics/FinanceBasicsPage'
import HolidayPeriodEditor from './components/holiday-periods/HolidayPeriodEditor'
import HolidayPeriodsPage from './components/holiday-periods/HolidayPeriodsPage'
import QuestionnaireEditor from './components/holiday-periods/QuestionnaireEditor'
import InvoicePage from './components/invoice/InvoicePage'
import MessagesPage from './components/messages/MessagesPage'
import PlacementDraftPage from './components/placement-draft/PlacementDraft'
import ReportApplications from './components/reports/Applications'
import ReportAssistanceNeedsAndActions from './components/reports/AssistanceNeedsAndActions'
import ReportChildAgeLanguage from './components/reports/ChildAgeLanguage'
import ReportChildrenInDifferentAddress from './components/reports/ChildrenInDifferentAddress'
import ReportDecisions from './components/reports/Decisions'
import ReportDuplicatePeople from './components/reports/DuplicatePeople'
import ReportEndedPlacements from './components/reports/EndedPlacements'
import ReportFamilyConflicts from './components/reports/FamilyConflicts'
import ReportFamilyContacts from './components/reports/FamilyContacts'
import ReportInvoices from './components/reports/Invoices'
import ReportMissingHeadOfFamily from './components/reports/MissingHeadOfFamily'
import ReportMissingServiceNeed from './components/reports/MissingServiceNeed'
import ReportOccupancies from './components/reports/Occupancies'
import ReportPartnersInDifferentAddress from './components/reports/PartnersInDifferentAddress'
import PlacementSketching from './components/reports/PlacementSketching'
import ReportPresences from './components/reports/PresenceReport'
import ReportRaw from './components/reports/Raw'
import ReportServiceNeeds from './components/reports/ServiceNeeds'
import ReportSextet from './components/reports/Sextet'
import ReportStartingPlacements from './components/reports/StartingPlacements'
import VardaErrors from './components/reports/VardaErrors'
import VoucherServiceProviderUnit from './components/reports/VoucherServiceProviderUnit'
import VoucherServiceProviders from './components/reports/VoucherServiceProviders'
import CreateUnitPage from './components/unit/unit-details/CreateUnitPage'
import UnitDetailsPage from './components/unit/unit-details/UnitDetailsPage'
import VasuEditPage from './components/vasu/VasuEditPage'
import VasuPage from './components/vasu/VasuPage'
import VasuTemplateEditor from './components/vasu/templates/VasuTemplateEditor'
import VasuTemplatesPage from './components/vasu/templates/VasuTemplatesPage'
import VoucherValueDecisionPage from './components/voucher-value-decision/VoucherValueDecisionPage'
import StateProvider from './state/StateProvider'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { UIContext } from './state/ui'
import { UserContext, UserContextProvider } from './state/user'
import { hasRole } from './utils/roles'

export default function App() {
  const { i18n } = useTranslation()
  const authStatus = useAuthStatus()

  if (authStatus === undefined) {
    return null
  }

  return (
    <I18nContextProvider>
      <ThemeProvider theme={theme}>
        <ErrorBoundary
          fallback={() => (
            <ErrorPage basePath="/employee" labels={i18n.errorPage} />
          )}
        >
          <UserContextProvider user={authStatus.user} roles={authStatus.roles}>
            <StateProvider>
              <Router basename="/employee">
                <Header />
                <Switch>
                  <Route
                    exact
                    path="/login"
                    element={
                      <EmployeeRoute
                        requireAuth={false}
                        title={i18n.titles.login}
                      >
                        <LoginPage />
                      </EmployeeRoute>
                    }
                  />
                  {featureFlags.experimental?.ai && (
                    <Route
                      exact
                      path="/ai"
                      element={
                        <EmployeeRoute title={i18n.titles.ai}>
                          <AIPage />
                        </EmployeeRoute>
                      }
                    />
                  )}
                  <Route
                    exact
                    path="/settings"
                    element={
                      <EmployeeRoute>
                        <SettingsPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/unit-features"
                    element={
                      <EmployeeRoute>
                        <UnitFeaturesPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/units"
                    element={
                      <EmployeeRoute title={i18n.titles.units}>
                        <Units />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/units/new"
                    element={
                      <EmployeeRoute title={i18n.titles.createUnit}>
                        <CreateUnitPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/units/:id/details"
                    element={
                      <EmployeeRoute>
                        <UnitDetailsPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/units/:unitId/family-contacts"
                    element={
                      <EmployeeRoute>
                        <ReportFamilyContacts />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/units/:unitId/groups/:groupId/caretakers"
                    element={
                      <EmployeeRoute>
                        <GroupCaretakers />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    path="/units/:id"
                    element={
                      <EmployeeRoute>
                        <UnitPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/search"
                    element={
                      <EmployeeRoute title={i18n.titles.customers}>
                        <Search />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/profile/:id"
                    element={
                      <EmployeeRoute>
                        <PersonProfile />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/profile/:personId/income-statement/:incomeStatementId"
                    element={
                      <EmployeeRoute>
                        <IncomeStatementPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/child-information/:id"
                    element={
                      <EmployeeRoute>
                        <ChildInformation />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/applications"
                    element={
                      <EmployeeRoute title={i18n.titles.applications}>
                        <ApplicationsPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/applications/:id"
                    element={
                      <EmployeeRoute title={i18n.titles.applications}>
                        <ApplicationPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/applications/:id/placement"
                    element={
                      <EmployeeRoute title={i18n.titles.placementDraft}>
                        <PlacementDraftPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/applications/:id/decisions"
                    element={
                      <EmployeeRoute title={i18n.titles.decision}>
                        <DecisionPage />
                      </EmployeeRoute>
                    }
                  />
                  {featureFlags.financeBasicsPage ? (
                    <Route
                      exact
                      path="/finance/basics"
                      element={
                        <EmployeeRoute>
                          <FinanceBasicsPage />
                        </EmployeeRoute>
                      }
                    />
                  ) : null}
                  <Route
                    exact
                    path="/finance/fee-decisions/:id"
                    element={
                      <EmployeeRoute>
                        <FeeDecisionDetailsPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/finance/value-decisions/:id"
                    element={
                      <EmployeeRoute>
                        <VoucherValueDecisionPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/finance/invoices/:id"
                    element={
                      <EmployeeRoute>
                        <InvoicePage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    path="/finance"
                    element={
                      <EmployeeRoute>
                        <FinancePage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <Reports />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/family-conflicts"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportFamilyConflicts />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/missing-head-of-family"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportMissingHeadOfFamily />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/missing-service-need"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportMissingServiceNeed />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/applications"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportApplications />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/decisions"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportDecisions />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/partners-in-different-address"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportPartnersInDifferentAddress />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/children-in-different-address"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportChildrenInDifferentAddress />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/child-age-language"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportChildAgeLanguage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/assistance-needs-and-actions"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportAssistanceNeedsAndActions />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/occupancies"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportOccupancies />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/invoices"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportInvoices />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/starting-placements"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportStartingPlacements />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/ended-placements"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportEndedPlacements />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/duplicate-people"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportDuplicatePeople />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/presences"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportPresences />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/service-needs"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportServiceNeeds />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/sextet"
                    element={
                      <EmployeeRoute>
                        <ReportSextet />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/voucher-service-providers"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <VoucherServiceProviders />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/voucher-service-providers/:unitId"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <VoucherServiceProviderUnit />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/varda-errors"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <VardaErrors />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/placement-sketching"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <PlacementSketching />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/reports/raw"
                    element={
                      <EmployeeRoute title={i18n.titles.reports}>
                        <ReportRaw />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/messages"
                    element={
                      <EmployeeRoute title={i18n.titles.messages}>
                        <MessagesPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/personal-mobile-devices"
                    element={
                      <EmployeeRoute title={i18n.titles.personalMobileDevices}>
                        <PersonalMobileDevicesPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/pin-code"
                    element={
                      <EmployeeRoute title={i18n.titles.employeePinCode}>
                        <EmployeePinCodePage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/employees"
                    element={
                      <EmployeeRoute title={i18n.employees.title}>
                        <EmployeesPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/employees/:id"
                    element={
                      <EmployeeRoute title={i18n.employees.title}>
                        <EmployeePage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/welcome"
                    element={
                      <EmployeeRoute title={i18n.titles.welcomePage}>
                        <WelcomePage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/vasu/:id"
                    element={
                      <EmployeeRoute title={i18n.titles.vasuPage}>
                        <VasuPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/vasu/:id/edit"
                    element={
                      <EmployeeRoute title={i18n.titles.vasuPage}>
                        <VasuEditPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/vasu-templates"
                    element={
                      <EmployeeRoute title={i18n.titles.vasuTemplates}>
                        <VasuTemplatesPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/vasu-templates/:id"
                    element={
                      <EmployeeRoute title={i18n.titles.vasuTemplates}>
                        <VasuTemplateEditor />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/holiday-periods"
                    element={
                      <EmployeeRoute title={i18n.titles.holidayPeriods}>
                        <HolidayPeriodsPage />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/holiday-periods/:id"
                    element={
                      <EmployeeRoute title={i18n.titles.holidayPeriods}>
                        <HolidayPeriodEditor />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/holiday-periods/questionnaire/:id"
                    element={
                      <EmployeeRoute title={i18n.titles.holidayQuestionnaire}>
                        <QuestionnaireEditor />
                      </EmployeeRoute>
                    }
                  />
                  <Route
                    exact
                    path="/fee-decisions"
                    element={<Redirect to="/finance/fee-decisions" />}
                  />
                  <Route
                    exact
                    path="/fee-decisions/:id"
                    element={
                      <RedirectByParams
                        to={(params: { id: string }) =>
                          `/finance/fee-decisions/${params.id}`
                        }
                      />
                    }
                  />
                  <Route
                    exact
                    path="/invoices"
                    element={<Redirect to="/finance/invoices" />}
                  />
                  <Route
                    exact
                    path="/invoices/:id"
                    element={
                      <RedirectByParams
                        to={(params: { id: string }) =>
                          `/finance/invoices/${params.id}`
                        }
                      />
                    }
                  />
                  <Route
                    exact
                    path="/"
                    element={
                      <EmployeeRoute requireAuth={false}>
                        <RedirectToMainPage />
                      </EmployeeRoute>
                    }
                  />
                </Switch>
                <Footer />
                <ErrorMessage />
                <ReloadNotification
                  i18n={i18n.reloadNotification}
                  apiVersion={authStatus?.apiVersion}
                />
                <LoginErrorModal translations={i18n.login.failedModal} />
                <PairingModal />
              </Router>
            </StateProvider>
          </UserContextProvider>
        </ErrorBoundary>
      </ThemeProvider>
    </I18nContextProvider>
  )
}

function RedirectToMainPage() {
  const { loggedIn, roles } = useContext(UserContext)

  if (!loggedIn) {
    return <Redirect to="/login" />
  }

  if (
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'SPECIAL_EDUCATION_TEACHER')
  ) {
    return <Redirect to="/applications" />
  } else if (hasRole(roles, 'UNIT_SUPERVISOR') || hasRole(roles, 'STAFF')) {
    return <Redirect to="/units" />
  } else if (hasRole(roles, 'DIRECTOR') || hasRole(roles, 'REPORT_VIEWER')) {
    return <Redirect to="/reports" />
  } else if (roles.length === 0) {
    return <Redirect to="/welcome" />
  } else {
    return <Redirect to="/search" />
  }
}

function RedirectByParams<T>({ to }: { to: (params: T) => string }) {
  const params = useParams<T>()
  return <Redirect to={to(params)} />
}

function useAuthStatus(): AuthStatus<User> | undefined {
  const [authStatus, setAuthStatus] = useState<AuthStatus<User>>()

  const refreshAuthStatus = useCallback(
    () => getAuthStatus().then(setAuthStatus),
    []
  )

  useEffect(() => {
    void refreshAuthStatus()
  }, [refreshAuthStatus])

  useEffect(() => {
    return idleTracker(
      client,
      () => {
        void refreshAuthStatus()
      },
      { thresholdInMinutes: 20 }
    )
  }, [refreshAuthStatus])

  return authStatus
}

const PairingModal = React.memo(function GlobalModals() {
  const { uiMode, pairingState, closePairingModal } = useContext(UIContext)

  if (uiMode !== 'pair-mobile-device' || !pairingState) {
    return null
  }

  return (
    <MobilePairingModal closeModal={closePairingModal} {...pairingState.id} />
  )
})
