// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { useContext } from 'react'
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom'
import { StyleSheetManager, ThemeProvider } from 'styled-components'

import { Notifications } from 'lib-components/Notifications'
import { EnvironmentLabel } from 'lib-components/atoms/EnvironmentLabel'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'

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
import ApplicationsPage from './components/applications/ApplicationsPage'
import {
  ChildDocumentEditView,
  ChildDocumentReadView
} from './components/child-documents/ChildDocumentEditor'
import AssistanceNeedDecisionEditPage from './components/child-information/assistance-need/decision/AssistanceNeedDecisionEditPage'
import AssistanceNeedDecisionPage from './components/child-information/assistance-need/decision/AssistanceNeedDecisionPage'
import AssistanceNeedPreschoolDecisionEditPage from './components/child-information/assistance-need/decision/AssistanceNeedPreschoolDecisionEditPage'
import AssistanceNeedPreschoolDecisionReadPage from './components/child-information/assistance-need/decision/AssistanceNeedPreschoolDecisionReadPage'
import ErrorMessage from './components/common/ErrorMessage'
import DecisionPage from './components/decision-draft/DecisionDraft'
import DocumentTemplatesPage from './components/document-templates/template-editor/DocumentTemplatesPage'
import TemplateEditorPage from './components/document-templates/template-editor/TemplateEditorPage'
import EmployeePinCodePage from './components/employee/EmployeePinCodePage'
import EmployeePreferredFirstNamePage from './components/employee/EmployeePreferredFirstNamePage'
import EmployeePage from './components/employees/EmployeePage'
import EmployeesPage from './components/employees/EmployeesPage'
import FeeDecisionDetailsPage from './components/fee-decision-details/FeeDecisionDetailsPage'
import FeeDecisionsPage from './components/fee-decisions/FeeDecisionsPage'
import FinanceBasicsPage from './components/finance-basics/FinanceBasicsPage'
import ClubTermPeriodEditor from './components/holiday-term-periods/ClubTermPeriodEditor'
import HolidayAndTermPeriodsPage from './components/holiday-term-periods/HolidayAndTermPeriodsPage'
import HolidayPeriodEditor from './components/holiday-term-periods/HolidayPeriodEditor'
import PreschoolTermPeriodEditor from './components/holiday-term-periods/PreschoolTermPeriodEditor'
import QuestionnaireEditor from './components/holiday-term-periods/QuestionnaireEditor'
import IncomeStatementsPage from './components/income-statements/IncomeStatementsPage'
import IncompleteIncomes from './components/reports/IncompleteIncomeReport'
import InvoicePage from './components/invoice/InvoicePage'
import InvoicesPage from './components/invoices/InvoicesPage'
import MessagesPage from './components/messages/MessagesPage'
import PaymentsPage from './components/payments/PaymentsPage'
import PlacementDraftPage from './components/placement-draft/PlacementDraft'
import PlacementToolPage from './components/placement-tool/PlacementToolPage'
import ReportApplications from './components/reports/Applications'
import AssistanceNeedDecisionsReport from './components/reports/AssistanceNeedDecisionsReport'
import AssistanceNeedDecisionsReportDecision from './components/reports/AssistanceNeedDecisionsReportDecision'
import AssistanceNeedDecisionsReportPreschoolDecision from './components/reports/AssistanceNeedDecisionsReportPreschoolDecision'
import ReportAssistanceNeedsAndActions from './components/reports/AssistanceNeedsAndActions'
import AttendanceReservation from './components/reports/AttendanceReservation'
import AttendanceReservationByChild from './components/reports/AttendanceReservationByChild'
import ReportChildAgeLanguage from './components/reports/ChildAgeLanguage'
import ChildAttendanceReport from './components/reports/ChildAttendanceReport'
import ReportChildrenInDifferentAddress from './components/reports/ChildrenInDifferentAddress'
import ReportCustomerFees from './components/reports/CustomerFees'
import ReportDecisions from './components/reports/Decisions'
import ReportDuplicatePeople from './components/reports/DuplicatePeople'
import ReportEndedPlacements from './components/reports/EndedPlacements'
import ReportExceededServiceNeeds from './components/reports/ExceededServiceNeeds'
import ReportFamilyConflicts from './components/reports/FamilyConflicts'
import ReportFamilyContacts from './components/reports/FamilyContacts'
import FamilyDaycareMealCount from './components/reports/FamilyDaycareMealCount'
import FuturePreschoolersReport from './components/reports/FuturePreschoolersReport'
import HolidayPeriodAttendanceReport from './components/reports/HolidayPeriodAttendanceReport'
import ReportInvoices from './components/reports/Invoices'
import ManualDuplicationReport from './components/reports/ManualDuplicationReport'
import MealReport from './components/reports/MealReport'
import ReportMissingHeadOfFamily from './components/reports/MissingHeadOfFamily'
import ReportMissingServiceNeed from './components/reports/MissingServiceNeed'
import ReportNonSsnChildren from './components/reports/NonSsnChildren'
import ReportOccupancies from './components/reports/Occupancies'
import ReportPartnersInDifferentAddress from './components/reports/PartnersInDifferentAddress'
import PlacementCount from './components/reports/PlacementCount'
import PlacementGuarantee from './components/reports/PlacementGuarantee'
import PlacementSketching from './components/reports/PlacementSketching'
import PreschoolAbsenceReport from './components/reports/PreschoolAbsenceReport'
import PreschoolApplicationReport from './components/reports/PreschoolApplicationReport'
import ReportPresences from './components/reports/PresenceReport'
import ReportRaw from './components/reports/Raw'
import ReportServiceNeeds from './components/reports/ServiceNeeds'
import ReportSextet from './components/reports/Sextet'
import ReportStartingPlacements from './components/reports/StartingPlacements'
import TitaniaErrors from './components/reports/TitaniaErrors'
import ReportUnits from './components/reports/Units'
import VardaChildErrors from './components/reports/VardaChildErrors'
import VardaUnitErrors from './components/reports/VardaUnitErrors'
import VoucherServiceProviderUnit from './components/reports/VoucherServiceProviderUnit'
import VoucherServiceProviders from './components/reports/VoucherServiceProviders'
import SystemNotificationsPage from './components/system-notifications/SystemNotificationsPage'
import TimelinePage from './components/timeline/TimelinePage'
import DiscussionReservationSurveyWrapper from './components/unit/tab-calendar/discussion-surveys/DiscussionSurveyWrapper'
import DiscussionReservationSurveysPage from './components/unit/tab-calendar/discussion-surveys/DiscussionSurveysPage'
import CreateUnitPage from './components/unit/unit-details/CreateUnitPage'
import UnitDetailsPage from './components/unit/unit-details/UnitDetailsPage'
import VoucherValueDecisionPage from './components/voucher-value-decision/VoucherValueDecisionPage'
import VoucherValueDecisionsPage from './components/voucher-value-decisions/VoucherValueDecisionsPage'
import { queryClient, QueryClientProvider } from './query'
import StateProvider from './state/StateProvider'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { UIContext } from './state/ui'
import { UserContext, UserContextProvider } from './state/user'
import { hasRole } from './utils/roles'

function App() {
  const { i18n } = useTranslation()

  return (
    <QueryClientProvider client={queryClient}>
      <I18nContextProvider>
        <StyleSheetManager shouldForwardProp={shouldForwardProp}>
          <ThemeProvider theme={theme}>
            <ErrorBoundary
              fallback={() => (
                <ErrorPage basePath="/employee" labels={i18n.errorPage} />
              )}
            >
              <UserContextProvider>
                <StateProvider>
                  <Content />
                </StateProvider>
              </UserContextProvider>
            </ErrorBoundary>
          </ThemeProvider>
        </StyleSheetManager>
      </I18nContextProvider>
    </QueryClientProvider>
  )
}

const Content = React.memo(function Content() {
  const { apiVersion, loaded } = useContext(UserContext)

  if (!loaded) return null

  return (
    <>
      <Header />
      <Notifications apiVersion={apiVersion} />

      {/* the matched route element will be inserted at <Outlet /> */}
      <Outlet />

      <Footer />
      {!!featureFlags.environmentLabel && (
        <EnvironmentLabel>{featureFlags.environmentLabel}</EnvironmentLabel>
      )}
      <ErrorMessage />
      <LoginErrorModal />
      <PairingModal />
    </>
  )
})

// This implements the default behavior from styled-components v5
// TODO: Prefix all custom props with $, then remove this
function shouldForwardProp(propName: string, target: unknown) {
  if (typeof target === 'string') {
    // For HTML elements, forward the prop if it is a valid HTML attribute
    return isPropValid(propName)
  }
  // For other elements, forward all props
  return true
}

export default createBrowserRouter(
  [
    {
      path: '/',
      element: <App />,
      children: [
        {
          path: '/login',
          element: (
            <EmployeeRoute requireAuth={false} title="login">
              <LoginPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/settings',
          element: (
            <EmployeeRoute title="settings">
              <SettingsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/system-notifications',
          element: (
            <EmployeeRoute title="systemNotifications">
              <SystemNotificationsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/unit-features',
          element: (
            <EmployeeRoute title="unitFeatures">
              <UnitFeaturesPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/units',
          element: (
            <EmployeeRoute title="units">
              <Units />
            </EmployeeRoute>
          )
        },
        {
          path: '/units/new',
          element: (
            <EmployeeRoute title="createUnit">
              <CreateUnitPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/units/:id/details',
          element: (
            <EmployeeRoute>
              <UnitDetailsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/units/:unitId/family-contacts',
          element: (
            <EmployeeRoute>
              <ReportFamilyContacts />
            </EmployeeRoute>
          )
        },
        {
          path: '/units/:unitId/groups/:groupId/caretakers',
          element: (
            <EmployeeRoute title="groupCaretakers">
              <GroupCaretakers />
            </EmployeeRoute>
          )
        },
        {
          path: '/units/:unitId/groups/:groupId/discussion-reservation-surveys',
          element: featureFlags.discussionReservations ? (
            <EmployeeRoute>
              <DiscussionReservationSurveysPage />
            </EmployeeRoute>
          ) : null
        },
        {
          path: '/units/:unitId/groups/:groupId/discussion-reservation-surveys/:eventId',
          element: featureFlags.discussionReservations ? (
            <EmployeeRoute>
              <DiscussionReservationSurveyWrapper mode="VIEW" />
            </EmployeeRoute>
          ) : null
        },
        {
          path: '/units/:unitId/groups/:groupId/discussion-reservation-surveys/:eventId/edit',
          element: featureFlags.discussionReservations ? (
            <EmployeeRoute>
              <DiscussionReservationSurveyWrapper mode="EDIT" />
            </EmployeeRoute>
          ) : null
        },
        {
          path: '/units/:id/*',
          element: (
            <EmployeeRoute>
              <UnitPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/search',
          element: (
            <EmployeeRoute title="customers">
              <Search />
            </EmployeeRoute>
          )
        },
        {
          path: '/profile/:id',
          element: (
            <EmployeeRoute title="personProfile">
              <PersonProfile />
            </EmployeeRoute>
          )
        },
        {
          path: '/profile/:personId/timeline',
          element: (
            <EmployeeRoute title="personTimeline">
              <TimelinePage />
            </EmployeeRoute>
          )
        },
        {
          path: '/profile/:personId/income-statement/:incomeStatementId',
          element: (
            <EmployeeRoute title="incomeStatement">
              <IncomeStatementPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-information/:id',
          element: (
            <EmployeeRoute title="childInformation">
              <ChildInformation />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-information/:childId/assistance-need-decision/:id',
          element: (
            <EmployeeRoute
              title="assistanceNeedDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedDecisionPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-information/:childId/assistance-need-decision/:id/edit',
          element: (
            <EmployeeRoute
              title="assistanceNeedDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedDecisionEditPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-information/:childId/assistance-need-preschool-decisions/:decisionId',
          element: (
            <EmployeeRoute
              title="assistanceNeedPreschoolDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedPreschoolDecisionReadPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-information/:childId/assistance-need-preschool-decisions/:decisionId/edit',
          element: (
            <EmployeeRoute
              title="assistanceNeedPreschoolDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedPreschoolDecisionEditPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/applications',
          element: (
            <EmployeeRoute title="applications">
              <ApplicationsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/applications/:id',
          element: (
            <EmployeeRoute title="applications">
              <ApplicationPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/applications/:id/placement',
          element: (
            <EmployeeRoute title="placementDraft">
              <PlacementDraftPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/applications/:id/decisions',
          element: (
            <EmployeeRoute title="decision">
              <DecisionPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/finance/basics',
          element: (
            <EmployeeRoute title="financeBasics">
              <FinanceBasicsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/finance/fee-decisions/:id',
          element: (
            <EmployeeRoute>
              <FeeDecisionDetailsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/finance/value-decisions/:id',
          element: (
            <EmployeeRoute>
              <VoucherValueDecisionPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/finance/invoices/:id',
          element: (
            <EmployeeRoute>
              <InvoicePage />
            </EmployeeRoute>
          )
        },
        {
          path: '/finance/*',
          element: (
            <EmployeeRoute>
              <FinancePage />
            </EmployeeRoute>
          ),
          children: [
            {
              path: 'fee-decisions',
              element: (
                <EmployeeRoute title="feeDecisions">
                  <FeeDecisionsPage />
                </EmployeeRoute>
              )
            },
            {
              path: 'value-decisions',
              element: (
                <EmployeeRoute title="valueDecisions">
                  <VoucherValueDecisionsPage />
                </EmployeeRoute>
              )
            },
            {
              path: 'invoices',
              element: (
                <EmployeeRoute title="invoices">
                  <InvoicesPage />
                </EmployeeRoute>
              )
            },
            {
              path: 'payments',
              element: featureFlags.voucherUnitPayments ? (
                <EmployeeRoute title="payments">
                  <PaymentsPage />
                </EmployeeRoute>
              ) : (
                <Navigate replace to="/finance/fee-decisions" />
              )
            },
            {
              path: 'income-statements',
              element: (
                <EmployeeRoute title="incomeStatements">
                  <IncomeStatementsPage />
                </EmployeeRoute>
              )
            },
            {
              path: '*',
              element: <Navigate replace to="/finance/fee-decisions" />
            },
            {
              index: true,
              element: <Navigate replace to="/finance/fee-decisions" />
            }
          ]
        },
        {
          path: '/reports',
          element: (
            <EmployeeRoute title="reports">
              <Reports />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/family-conflicts',
          element: (
            <EmployeeRoute title="reports">
              <ReportFamilyConflicts />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/missing-head-of-family',
          element: (
            <EmployeeRoute title="reports">
              <ReportMissingHeadOfFamily />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/missing-service-need',
          element: (
            <EmployeeRoute title="reports">
              <ReportMissingServiceNeed />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/non-ssn-children',
          element: (
            <EmployeeRoute title="reports">
              <ReportNonSsnChildren />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/applications',
          element: (
            <EmployeeRoute title="reports">
              <ReportApplications />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/decisions',
          element: (
            <EmployeeRoute title="reports">
              <ReportDecisions />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/partners-in-different-address',
          element: (
            <EmployeeRoute title="reports">
              <ReportPartnersInDifferentAddress />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/children-in-different-address',
          element: (
            <EmployeeRoute title="reports">
              <ReportChildrenInDifferentAddress />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/child-age-language',
          element: (
            <EmployeeRoute title="reports">
              <ReportChildAgeLanguage />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/assistance-needs-and-actions',
          element: (
            <EmployeeRoute title="reports">
              <ReportAssistanceNeedsAndActions />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/occupancies',
          element: (
            <EmployeeRoute title="reports">
              <ReportOccupancies />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/invoices',
          element: (
            <EmployeeRoute title="reports">
              <ReportInvoices />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/customer-fees',
          element: (
            <EmployeeRoute title="reports">
              <ReportCustomerFees />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/starting-placements',
          element: (
            <EmployeeRoute title="reports">
              <ReportStartingPlacements />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/ended-placements',
          element: (
            <EmployeeRoute title="reports">
              <ReportEndedPlacements />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/exceeded-service-needs',
          element: (
            <EmployeeRoute title="reports">
              <ReportExceededServiceNeeds />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/duplicate-people',
          element: (
            <EmployeeRoute title="reports">
              <ReportDuplicatePeople />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/presences',
          element: (
            <EmployeeRoute title="reports">
              <ReportPresences />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/service-needs',
          element: (
            <EmployeeRoute title="reports">
              <ReportServiceNeeds />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/sextet',
          element: (
            <EmployeeRoute title="reports">
              <ReportSextet />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/voucher-service-providers',
          element: (
            <EmployeeRoute title="reports">
              <VoucherServiceProviders />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/voucher-service-providers/:unitId',
          element: (
            <EmployeeRoute title="reports">
              <VoucherServiceProviderUnit />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/attendance-reservation',
          element: (
            <EmployeeRoute title="reports">
              <AttendanceReservation />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/attendance-reservation-by-child',
          element: (
            <EmployeeRoute title="reports">
              <AttendanceReservationByChild />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/varda-child-errors',
          element: (
            <EmployeeRoute title="reports">
              <VardaChildErrors />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/varda-unit-errors',
          element: (
            <EmployeeRoute title="reports">
              <VardaUnitErrors />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/placement-count',
          element: (
            <EmployeeRoute title="reports">
              <PlacementCount />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/placement-sketching',
          element: (
            <EmployeeRoute title="reports">
              <PlacementSketching />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/raw',
          element: (
            <EmployeeRoute title="reports">
              <ReportRaw />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/assistance-need-decisions/:id',
          element: (
            <EmployeeRoute title="reports">
              <AssistanceNeedDecisionsReportDecision />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/assistance-need-preschool-decisions/:decisionId',
          element: (
            <EmployeeRoute title="reports">
              <AssistanceNeedDecisionsReportPreschoolDecision />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/assistance-need-decisions',
          element: (
            <EmployeeRoute title="reports">
              <AssistanceNeedDecisionsReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/manual-duplication',
          element: (
            <EmployeeRoute title="reports">
              <ManualDuplicationReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/family-daycare-meal-count',
          element: (
            <EmployeeRoute title="reports">
              <FamilyDaycareMealCount />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/preschool-absence',
          element: (
            <EmployeeRoute title="reports">
              <PreschoolAbsenceReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/preschool-application',
          element: (
            <EmployeeRoute title="reports">
              <PreschoolApplicationReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/future-preschoolers',
          element: (
            <EmployeeRoute title="reports">
              <FuturePreschoolersReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/placement-guarantee',
          element: (
            <EmployeeRoute title="reports">
              <PlacementGuarantee />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/units',
          element: (
            <EmployeeRoute title="reports">
              <ReportUnits />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/meals',
          element: (
            <EmployeeRoute title="reports">
              <MealReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/child-attendance/:childId',
          element: (
            <EmployeeRoute title="reports">
              <ChildAttendanceReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/holiday-period-attendance',
          element: (
            <EmployeeRoute title="reports">
              <HolidayPeriodAttendanceReport />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/titania-errors',
          element: (
            <EmployeeRoute title="reports">
              <TitaniaErrors />
            </EmployeeRoute>
          )
        },
        {
          path: '/reports/incomplete-income',
          element: (
            <EmployeeRoute title="reports">
              <IncompleteIncomes />
            </EmployeeRoute>
          )
        },
        {
          path: '/messages',
          element: (
            <EmployeeRoute title="messages">
              <MessagesPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/messages/send',
          element: (
            <EmployeeRoute title="messages">
              <MessagesPage showEditor />
            </EmployeeRoute>
          )
        },
        {
          path: '/personal-mobile-devices',
          element: (
            <EmployeeRoute title="personalMobileDevices">
              <PersonalMobileDevicesPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/pin-code',
          element: (
            <EmployeeRoute title="employeePinCode">
              <EmployeePinCodePage />
            </EmployeeRoute>
          )
        },
        {
          path: '/preferred-first-name',
          element: (
            <EmployeeRoute title="preferredFirstName">
              <EmployeePreferredFirstNamePage />
            </EmployeeRoute>
          )
        },
        {
          path: '/employees',
          element: (
            <EmployeeRoute title="employees">
              <EmployeesPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/employees/:id',
          element: (
            <EmployeeRoute title="employees">
              <EmployeePage />
            </EmployeeRoute>
          )
        },
        {
          path: '/welcome',
          element: (
            <EmployeeRoute title="welcomePage">
              <WelcomePage />
            </EmployeeRoute>
          )
        },
        {
          path: '/document-templates',
          element: (
            <EmployeeRoute title="documentTemplates">
              <DocumentTemplatesPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/document-templates/:templateId',
          element: (
            <EmployeeRoute title="documentTemplates">
              <TemplateEditorPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-documents/:documentId',
          element: (
            <EmployeeRoute>
              <ChildDocumentReadView />
            </EmployeeRoute>
          )
        },
        {
          path: '/child-documents/:documentId/edit',
          element: (
            <EmployeeRoute>
              <ChildDocumentEditView />
            </EmployeeRoute>
          )
        },
        {
          path: '/holiday-periods',
          element: (
            <EmployeeRoute title="holidayAndTermPeriods">
              <HolidayAndTermPeriodsPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/holiday-periods/:id',
          element: (
            <EmployeeRoute title="holidayAndTermPeriods">
              <HolidayPeriodEditor />
            </EmployeeRoute>
          )
        },
        {
          path: '/holiday-periods/questionnaire/:id',
          element: (
            <EmployeeRoute title="holidayQuestionnaire">
              <QuestionnaireEditor />
            </EmployeeRoute>
          )
        },
        {
          path: '/holiday-periods/club-term/:termId',
          element: (
            <EmployeeRoute title="clubTerm">
              <ClubTermPeriodEditor />
            </EmployeeRoute>
          )
        },
        {
          path: '/holiday-periods/preschool-term/:termId',
          element: (
            <EmployeeRoute title="preschoolTerm">
              <PreschoolTermPeriodEditor />
            </EmployeeRoute>
          )
        },
        {
          path: '/placement-tool',
          element: (
            <EmployeeRoute title="placementTool">
              <PlacementToolPage />
            </EmployeeRoute>
          )
        },
        {
          path: '/*',
          element: (
            <EmployeeRoute requireAuth={false}>
              <RedirectToMainPage />
            </EmployeeRoute>
          )
        },
        {
          index: true,
          element: (
            <EmployeeRoute requireAuth={false}>
              <RedirectToMainPage />
            </EmployeeRoute>
          )
        }
      ]
    }
  ],
  { basename: '/employee' }
)

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
  } else if (hasRole(roles, 'MESSAGING')) {
    return <Navigate replace to="/messages" />
  } else if (roles.length === 0) {
    return <Navigate replace to="/welcome" />
  } else {
    return <Navigate replace to="/search" />
  }
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
