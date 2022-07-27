// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, {
  lazy,
  Suspense,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'

import type { AuthStatus, User } from 'lib-common/api-types/employee-auth'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import ReloadNotification from 'lib-components/molecules/ReloadNotification'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'

import { getAuthStatus } from './api/auth'
import { client } from './api/client'
import EmployeeRoute from './components/EmployeeRoute'
import { Footer } from './components/Footer'
import Header from './components/Header'
import MobilePairingModal from './components/MobilePairingModal'
import ErrorMessage from './components/common/ErrorMessage'
import StateProvider from './state/StateProvider'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { UIContext } from './state/ui'
import { UserContext, UserContextProvider } from './state/user'
import { hasRole } from './utils/roles'

export default function App() {
  const { i18n } = useTranslation()

  return (
    <I18nContextProvider>
      <ThemeProvider theme={theme}>
        <ErrorBoundary
          fallback={() => (
            <ErrorPage basePath="/employee" labels={i18n.errorPage} />
          )}
        >
          <Content />
        </ErrorBoundary>
      </ThemeProvider>
    </I18nContextProvider>
  )
}

const ChildInformation = lazy(() => import('./components/ChildInformation'))
const FinancePage = lazy(() => import('./components/FinancePage'))
const GroupCaretakers = lazy(() => import('./components/GroupCaretakers'))
const IncomeStatementPage = lazy(
  () => import('./components/IncomeStatementPage')
)
const LoginPage = lazy(
  () => import(/* webpackChunkName: "LoginPage" */ './components/LoginPage')
)
const PersonProfile = lazy(() => import('./components/PersonProfile'))
const PersonalMobileDevicesPage = lazy(
  () => import('./components/PersonalMobileDevicesPage')
)
const Reports = lazy(() => import('./components/Reports'))
const Search = lazy(() => import('./components/Search'))
const SettingsPage = lazy(() => import('./components/SettingsPage'))
const UnitFeaturesPage = lazy(() => import('./components/UnitFeaturesPage'))
const UnitPage = lazy(() => import('./components/UnitPage'))
const Units = lazy(() => import('./components/Units'))
const WelcomePage = lazy(() => import('./components/WelcomePage'))
const AIPage = lazy(() => import('./components/ai/AIPage'))
const ApplicationPage = lazy(() => import('./components/ApplicationPage'))
const ApplicationsPage = lazy(
  () => import('./components/applications/ApplicationsPage')
)
const AssistanceNeedDecisionPage = lazy(
  () =>
    import(
      './components/child-information/assistance-need/decision/AssistanceNeedDecisionPage'
    )
)
const AssistanceNeedDecisionEditPage = lazy(
  () =>
    import(
      './components/child-information/assistance-need/decision/AssistanceNeedDecisionEditPage'
    )
)
const AssistanceNeedDecisionsReportDecision = lazy(
  () => import('./components/reports/AssistanceNeedDecisionsReportDecision')
)
const AssistanceNeedDecisionsReport = lazy(
  () => import('./components/reports/AssistanceNeedDecisionsReport')
)
const DecisionPage = lazy(
  () => import('./components/decision-draft/DecisionDraft')
)
const EmployeePinCodePage = lazy(
  () => import('./components/employee/EmployeePinCodePage')
)
const EmployeePage = lazy(() => import('./components/employees/EmployeePage'))
const EmployeesPage = lazy(() => import('./components/employees/EmployeesPage'))
const FeeDecisionDetailsPage = lazy(
  () => import('./components/fee-decision-details/FeeDecisionDetailsPage')
)
const FinanceBasicsPage = lazy(
  () => import('./components/finance-basics/FinanceBasicsPage')
)
const HolidayPeriodEditor = lazy(
  () => import('./components/holiday-periods/HolidayPeriodEditor')
)
const HolidayPeriodsPage = lazy(
  () => import('./components/holiday-periods/HolidayPeriodsPage')
)
const QuestionnaireEditor = lazy(
  () => import('./components/holiday-periods/QuestionnaireEditor')
)
const InvoicePage = lazy(() => import('./components/invoice/InvoicePage'))
const MessagesPage = lazy(() => import('./components/messages/MessagesPage'))
const PlacementDraftPage = lazy(
  () => import('./components/placement-draft/PlacementDraft')
)
const ReportApplications = lazy(
  () => import('./components/reports/Applications')
)
const ReportAssistanceNeedsAndActions = lazy(
  () => import('./components/reports/AssistanceNeedsAndActions')
)
const ReportChildAgeLanguage = lazy(
  () => import('./components/reports/ChildAgeLanguage')
)
const ReportChildrenInDifferentAddress = lazy(
  () => import('./components/reports/ChildrenInDifferentAddress')
)
const ReportDecisions = lazy(() => import('./components/reports/Decisions'))
const ReportDuplicatePeople = lazy(
  () => import('./components/reports/DuplicatePeople')
)
const ReportEndedPlacements = lazy(
  () => import('./components/reports/EndedPlacements')
)
const ReportFamilyConflicts = lazy(
  () => import('./components/reports/FamilyConflicts')
)
const ReportFamilyContacts = lazy(
  () => import('./components/reports/FamilyContacts')
)
const ReportInvoices = lazy(() => import('./components/reports/Invoices'))
const ReportMissingHeadOfFamily = lazy(
  () => import('./components/reports/MissingHeadOfFamily')
)
const ReportMissingServiceNeed = lazy(
  () => import('./components/reports/MissingServiceNeed')
)
const ReportOccupancies = lazy(() => import('./components/reports/Occupancies'))
const ReportPartnersInDifferentAddress = lazy(
  () => import('./components/reports/PartnersInDifferentAddress')
)
const PlacementSketching = lazy(
  () => import('./components/reports/PlacementSketching')
)
const ReportPresences = lazy(
  () => import('./components/reports/PresenceReport')
)
const ReportRaw = lazy(() => import('./components/reports/Raw'))
const ReportServiceNeeds = lazy(
  () => import('./components/reports/ServiceNeeds')
)
const ReportSextet = lazy(() => import('./components/reports/Sextet'))
const ReportStartingPlacements = lazy(
  () => import('./components/reports/StartingPlacements')
)
const VardaErrors = lazy(() => import('./components/reports/VardaErrors'))
const VoucherServiceProviderUnit = lazy(
  () => import('./components/reports/VoucherServiceProviderUnit')
)
const VoucherServiceProviders = lazy(
  () => import('./components/reports/VoucherServiceProviders')
)
const CreateUnitPage = lazy(
  () => import('./components/unit/unit-details/CreateUnitPage')
)
const UnitDetailsPage = lazy(
  () => import('./components/unit/unit-details/UnitDetailsPage')
)
const VasuEditPage = lazy(() => import('./components/vasu/VasuEditPage'))
const VasuPage = lazy(() => import('./components/vasu/VasuPage'))
const VasuTemplateEditor = lazy(
  () => import('./components/vasu/templates/VasuTemplateEditor')
)
const VasuTemplatesPage = lazy(
  () => import('./components/vasu/templates/VasuTemplatesPage')
)
const VoucherValueDecisionPage = lazy(
  () => import('./components/voucher-value-decision/VoucherValueDecisionPage')
)

const Content = React.memo(function Content() {
  const { i18n } = useTranslation()
  const authStatus = useAuthStatus()

  if (authStatus === undefined) {
    return (
      <>
        <Header />
        <SpinnerSegment />
      </>
    )
  }

  return (
    <UserContextProvider user={authStatus.user} roles={authStatus.roles}>
      <StateProvider>
        <Header />
        <Suspense fallback={<SpinnerSegment />}>
          <Routes>
            <Route
              path="/login"
              element={
                <EmployeeRoute requireAuth={false} title={i18n.titles.login}>
                  <LoginPage />
                </EmployeeRoute>
              }
            />
            {featureFlags.experimental?.ai && (
              <Route
                path="/ai"
                element={
                  <EmployeeRoute title={i18n.titles.ai}>
                    <AIPage />
                  </EmployeeRoute>
                }
              />
            )}
            <Route
              path="/settings"
              element={
                <EmployeeRoute>
                  <SettingsPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/unit-features"
              element={
                <EmployeeRoute>
                  <UnitFeaturesPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/units"
              element={
                <EmployeeRoute title={i18n.titles.units}>
                  <Units />
                </EmployeeRoute>
              }
            />
            <Route
              path="/units/new"
              element={
                <EmployeeRoute title={i18n.titles.createUnit}>
                  <CreateUnitPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/units/:id/details"
              element={
                <EmployeeRoute>
                  <UnitDetailsPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/units/:unitId/family-contacts"
              element={
                <EmployeeRoute>
                  <ReportFamilyContacts />
                </EmployeeRoute>
              }
            />
            <Route
              path="/units/:unitId/groups/:groupId/caretakers"
              element={
                <EmployeeRoute>
                  <GroupCaretakers />
                </EmployeeRoute>
              }
            />
            <Route
              path="/units/:id/*"
              element={
                <EmployeeRoute>
                  <UnitPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/search"
              element={
                <EmployeeRoute title={i18n.titles.customers}>
                  <Search />
                </EmployeeRoute>
              }
            />
            <Route
              path="/profile/:id"
              element={
                <EmployeeRoute>
                  <PersonProfile />
                </EmployeeRoute>
              }
            />
            <Route
              path="/profile/:personId/income-statement/:incomeStatementId"
              element={
                <EmployeeRoute>
                  <IncomeStatementPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/child-information/:id"
              element={
                <EmployeeRoute>
                  <ChildInformation />
                </EmployeeRoute>
              }
            />
            <Route
              path="/child-information/:childId/assistance-need-decision/:id"
              element={
                <EmployeeRoute title={i18n.titles.assistanceNeedDecision}>
                  <AssistanceNeedDecisionPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/child-information/:childId/assistance-need-decision/:id/edit"
              element={
                <EmployeeRoute title={i18n.titles.assistanceNeedDecision}>
                  <AssistanceNeedDecisionEditPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/applications"
              element={
                <EmployeeRoute title={i18n.titles.applications}>
                  <ApplicationsPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/applications/:id"
              element={
                <EmployeeRoute title={i18n.titles.applications}>
                  <ApplicationPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/applications/:id/placement"
              element={
                <EmployeeRoute title={i18n.titles.placementDraft}>
                  <PlacementDraftPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/applications/:id/decisions"
              element={
                <EmployeeRoute title={i18n.titles.decision}>
                  <DecisionPage />
                </EmployeeRoute>
              }
            />
            {featureFlags.financeBasicsPage ? (
              <Route
                path="/finance/basics"
                element={
                  <EmployeeRoute>
                    <FinanceBasicsPage />
                  </EmployeeRoute>
                }
              />
            ) : null}
            <Route
              path="/finance/fee-decisions/:id"
              element={
                <EmployeeRoute>
                  <FeeDecisionDetailsPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/finance/value-decisions/:id"
              element={
                <EmployeeRoute>
                  <VoucherValueDecisionPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/finance/invoices/:id"
              element={
                <EmployeeRoute>
                  <InvoicePage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/finance/*"
              element={
                <EmployeeRoute>
                  <FinancePage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <Reports />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/family-conflicts"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportFamilyConflicts />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/missing-head-of-family"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportMissingHeadOfFamily />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/missing-service-need"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportMissingServiceNeed />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/applications"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportApplications />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/decisions"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportDecisions />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/partners-in-different-address"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportPartnersInDifferentAddress />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/children-in-different-address"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportChildrenInDifferentAddress />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/child-age-language"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportChildAgeLanguage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/assistance-needs-and-actions"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportAssistanceNeedsAndActions />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/occupancies"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportOccupancies />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/invoices"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportInvoices />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/starting-placements"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportStartingPlacements />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/ended-placements"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportEndedPlacements />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/duplicate-people"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportDuplicatePeople />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/presences"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportPresences />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/service-needs"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportServiceNeeds />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/sextet"
              element={
                <EmployeeRoute>
                  <ReportSextet />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/voucher-service-providers"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <VoucherServiceProviders />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/voucher-service-providers/:unitId"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <VoucherServiceProviderUnit />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/varda-errors"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <VardaErrors />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/placement-sketching"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <PlacementSketching />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/raw"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <ReportRaw />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/assistance-need-decisions/:id"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <AssistanceNeedDecisionsReportDecision />
                </EmployeeRoute>
              }
            />
            <Route
              path="/reports/assistance-need-decisions"
              element={
                <EmployeeRoute title={i18n.titles.reports}>
                  <AssistanceNeedDecisionsReport />
                </EmployeeRoute>
              }
            />
            <Route
              path="/messages"
              element={
                <EmployeeRoute title={i18n.titles.messages}>
                  <MessagesPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/personal-mobile-devices"
              element={
                <EmployeeRoute title={i18n.titles.personalMobileDevices}>
                  <PersonalMobileDevicesPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/pin-code"
              element={
                <EmployeeRoute title={i18n.titles.employeePinCode}>
                  <EmployeePinCodePage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/employees"
              element={
                <EmployeeRoute title={i18n.employees.title}>
                  <EmployeesPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/employees/:id"
              element={
                <EmployeeRoute title={i18n.employees.title}>
                  <EmployeePage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/welcome"
              element={
                <EmployeeRoute title={i18n.titles.welcomePage}>
                  <WelcomePage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/vasu/:id"
              element={
                <EmployeeRoute title={i18n.titles.vasuPage}>
                  <VasuPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/vasu/:id/edit"
              element={
                <EmployeeRoute title={i18n.titles.vasuPage}>
                  <VasuEditPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/vasu-templates"
              element={
                <EmployeeRoute title={i18n.titles.vasuTemplates}>
                  <VasuTemplatesPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/vasu-templates/:id"
              element={
                <EmployeeRoute title={i18n.titles.vasuTemplates}>
                  <VasuTemplateEditor />
                </EmployeeRoute>
              }
            />
            <Route
              path="/holiday-periods"
              element={
                <EmployeeRoute title={i18n.titles.holidayPeriods}>
                  <HolidayPeriodsPage />
                </EmployeeRoute>
              }
            />
            <Route
              path="/holiday-periods/:id"
              element={
                <EmployeeRoute title={i18n.titles.holidayPeriods}>
                  <HolidayPeriodEditor />
                </EmployeeRoute>
              }
            />
            <Route
              path="/holiday-periods/questionnaire/:id"
              element={
                <EmployeeRoute title={i18n.titles.holidayQuestionnaire}>
                  <QuestionnaireEditor />
                </EmployeeRoute>
              }
            />
            <Route
              index
              element={
                <EmployeeRoute requireAuth={false}>
                  <RedirectToMainPage />
                </EmployeeRoute>
              }
            />
          </Routes>
        </Suspense>
        <Footer />
        <ErrorMessage />
        <ReloadNotification
          i18n={i18n.reloadNotification}
          apiVersion={authStatus?.apiVersion}
        />
        <LoginErrorModal translations={i18n.login.failedModal} />
        <PairingModal />
      </StateProvider>
    </UserContextProvider>
  )
})

function RedirectToMainPage() {
  const { loggedIn, roles } = useContext(UserContext)

  if (!loggedIn) {
    return <Navigate replace to="/login" />
  }

  if (
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'SPECIAL_EDUCATION_TEACHER')
  ) {
    return <Navigate replace to="/applications" />
  } else if (hasRole(roles, 'UNIT_SUPERVISOR') || hasRole(roles, 'STAFF')) {
    return <Navigate replace to="/units" />
  } else if (hasRole(roles, 'DIRECTOR') || hasRole(roles, 'REPORT_VIEWER')) {
    return <Navigate replace to="/reports" />
  } else if (roles.length === 0) {
    return <Navigate replace to="/welcome" />
  } else {
    return <Navigate replace to="/search" />
  }
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
