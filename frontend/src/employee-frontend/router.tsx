// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Redirect, Route, Router, Switch } from 'wouter'

import type { Translations } from 'lib-customizations/employee'
import { featureFlags } from 'lib-customizations/employee'

import { App, CloseAfterLogin, RedirectToMainPage } from './App'
import EmployeeRoute from './components/EmployeeRoute'
import FinancePage from './components/FinancePage'
import LoginPage from './components/LoginPage'
import Reports from './components/Reports'
import UnitPage from './components/UnitPage'
import Units from './components/Units'
import WelcomePage from './components/WelcomePage'
import ApplicationPage from './components/application-page/ApplicationPage'
import ApplicationsPage from './components/applications/ApplicationsPage'
import ChildDocumentEditView from './components/child-documents/ChildDocumentEditView'
import ChildDocumentReadView from './components/child-documents/ChildDocumentReadView'
import ChildInformation from './components/child-information/ChildInformation'
import AssistanceNeedDecisionEditPage from './components/child-information/assistance-need/decision/AssistanceNeedDecisionEditPage'
import AssistanceNeedDecisionPage from './components/child-information/assistance-need/decision/AssistanceNeedDecisionPage'
import AssistanceNeedPreschoolDecisionEditPage from './components/child-information/assistance-need/decision/AssistanceNeedPreschoolDecisionEditPage'
import AssistanceNeedPreschoolDecisionReadPage from './components/child-information/assistance-need/decision/AssistanceNeedPreschoolDecisionReadPage'
import DecisionPage from './components/decision-draft/DecisionDraft'
import DocumentTemplatesPage from './components/document-templates/template-editor/DocumentTemplatesPage'
import TemplateEditorPage from './components/document-templates/template-editor/TemplateEditorPage'
import EmployeePinCodePage from './components/employee-pin-code/EmployeePinCodePage'
import EmployeePreferredFirstNamePage from './components/employee-preferred-first-name/EmployeePreferredFirstNamePage'
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
import IncomeStatementPage from './components/income-statements/IncomeStatementPage'
import IncomeStatementsPage from './components/income-statements/IncomeStatementsPage'
import InvoicePage from './components/invoice/InvoicePage'
import InvoicesPage from './components/invoices/InvoicesPage'
import MessagesPage from './components/messages/MessagesPage'
import OutOfOfficePage from './components/out-of-office/OutOfOfficePage'
import PaymentsPage from './components/payments/PaymentsPage'
import PersonProfile from './components/person-profile/PersonProfile'
import Search from './components/person-search/Search'
import PersonalMobileDevicesPage from './components/personal-mobile-devices/PersonalMobileDevicesPage'
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
import ReportChildDocumentDecisions from './components/reports/ChildDocumentDecisionsReport'
import ReportChildDocuments from './components/reports/ChildDocumentsReport'
import ReportChildrenInDifferentAddress from './components/reports/ChildrenInDifferentAddress'
import CitizenDocumentResponseReport from './components/reports/CitizenDocumentResponseReport'
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
import HolidayQuestionnaireReport from './components/reports/HolidayQuestionnaireReport'
import IncompleteIncomes from './components/reports/IncompleteIncomeReport'
import ReportInvoices from './components/reports/Invoices'
import MealReport from './components/reports/MealReport'
import ReportMissingHeadOfFamily from './components/reports/MissingHeadOfFamily'
import ReportMissingServiceNeed from './components/reports/MissingServiceNeed'
import NekkuOrders from './components/reports/NekkuOrders'
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
import TampereRegionalSurvey from './components/reports/TampereRegionalSurvey'
import TitaniaErrors from './components/reports/TitaniaErrors'
import ReportUnits from './components/reports/Units'
import VardaChildErrors from './components/reports/VardaChildErrors'
import VardaUnitErrors from './components/reports/VardaUnitErrors'
import VoucherServiceProviderUnit from './components/reports/VoucherServiceProviderUnit'
import VoucherServiceProviders from './components/reports/VoucherServiceProviders'
import SettingsPage from './components/settings/SettingsPage'
import SystemNotificationsPage from './components/system-notifications/SystemNotificationsPage'
import TimelinePage from './components/timeline/TimelinePage'
import GroupCaretakers from './components/unit/group-caretakers/GroupCaretakers'
import DiscussionReservationSurveyWrapper from './components/unit/tab-calendar/discussion-surveys/DiscussionSurveyWrapper'
import DiscussionReservationSurveysPage from './components/unit/tab-calendar/discussion-surveys/DiscussionSurveysPage'
import CreateUnitPage from './components/unit/unit-details/CreateUnitPage'
import UnitDetailsPage from './components/unit/unit-details/UnitDetailsPage'
import UnitFeaturesPage from './components/unit-features/UnitFeaturesPage'
import VoucherValueDecisionPage from './components/voucher-value-decision/VoucherValueDecisionPage'
import VoucherValueDecisionsPage from './components/voucher-value-decisions/VoucherValueDecisionsPage'

interface EmployeeRoute {
  path: string
  component: React.FunctionComponent
  title?: keyof Translations['titles']
  auth?: boolean
  hideDefaultTitle?: boolean
  disabled?: boolean
}

const routes: EmployeeRoute[] = [
  { path: '/login', component: LoginPage, auth: false, title: 'login' },
  { path: '/settings', component: SettingsPage, title: 'settings' },
  {
    path: '/system-notifications',
    component: SystemNotificationsPage,
    title: 'systemNotifications'
  },
  {
    path: '/unit-features',
    component: UnitFeaturesPage,
    title: 'unitFeatures'
  },
  { path: '/units', component: Units, title: 'units' },
  { path: '/units/new', component: CreateUnitPage, title: 'createUnit' },
  { path: '/units/:id/details', component: UnitDetailsPage },
  { path: '/units/:unitId/family-contacts', component: ReportFamilyContacts },
  {
    path: '/units/:unitId/groups/:groupId/caretakers',
    component: GroupCaretakers,
    title: 'groupCaretakers'
  },
  {
    path: '/units/:unitId/groups/:groupId/discussion-reservation-surveys',
    component: DiscussionReservationSurveysPage,
    disabled: !featureFlags.discussionReservations
  },
  {
    path: '/units/:unitId/groups/:groupId/discussion-reservation-surveys/:eventId',
    component: () => <DiscussionReservationSurveyWrapper mode="VIEW" />,
    disabled: !featureFlags.discussionReservations
  },
  {
    path: '/units/:unitId/groups/:groupId/discussion-reservation-surveys/:eventId/edit',
    component: () => <DiscussionReservationSurveyWrapper mode="EDIT" />,
    disabled: !featureFlags.discussionReservations
  },
  { path: '/units/:id/*?', component: UnitPage },
  { path: '/search', component: Search, title: 'customers' },
  { path: '/profile/:id', component: PersonProfile, title: 'personProfile' },
  {
    path: '/profile/:personId/timeline',
    component: TimelinePage,
    title: 'personTimeline'
  },
  {
    path: '/profile/:personId/income-statement/:incomeStatementId',
    component: IncomeStatementPage,
    title: 'incomeStatement'
  },
  {
    path: '/child-information/:id',
    component: ChildInformation,
    title: 'childInformation'
  },
  {
    path: '/child-information/:childId/assistance-need-decision/:id',
    component: AssistanceNeedDecisionPage,
    title: 'assistanceNeedDecision',
    hideDefaultTitle: true
  },
  {
    path: '/child-information/:childId/assistance-need-decision/:id/edit',
    component: AssistanceNeedDecisionEditPage,
    title: 'assistanceNeedDecision',
    hideDefaultTitle: true
  },
  {
    path: '/child-information/:childId/assistance-need-preschool-decisions/:decisionId',
    component: AssistanceNeedPreschoolDecisionReadPage,
    title: 'assistanceNeedPreschoolDecision',
    hideDefaultTitle: true
  },
  {
    path: '/child-information/:childId/assistance-need-preschool-decisions/:decisionId/edit',
    component: AssistanceNeedPreschoolDecisionEditPage,
    title: 'assistanceNeedPreschoolDecision',
    hideDefaultTitle: true
  },
  { path: '/applications', component: ApplicationsPage, title: 'applications' },
  {
    path: '/applications/:id',
    component: ApplicationPage,
    title: 'applications'
  },
  {
    path: '/applications/:id/placement',
    component: PlacementDraftPage,
    title: 'placementDraft'
  },
  {
    path: '/applications/:id/decisions',
    component: DecisionPage,
    title: 'decision'
  },
  {
    path: '/finance/basics',
    component: FinanceBasicsPage,
    title: 'financeBasics'
  },
  { path: '/finance/fee-decisions/:id', component: FeeDecisionDetailsPage },
  { path: '/finance/value-decisions/:id', component: VoucherValueDecisionPage },
  { path: '/finance/invoices/:id', component: InvoicePage },
  { path: '/reports', component: Reports, title: 'reports' },
  {
    path: '/reports/family-conflicts',
    component: ReportFamilyConflicts,
    title: 'reports'
  },
  {
    path: '/reports/missing-head-of-family',
    component: ReportMissingHeadOfFamily,
    title: 'reports'
  },
  {
    path: '/reports/missing-service-need',
    component: ReportMissingServiceNeed,
    title: 'reports'
  },
  {
    path: '/reports/non-ssn-children',
    component: ReportNonSsnChildren,
    title: 'reports'
  },
  {
    path: '/reports/applications',
    component: ReportApplications,
    title: 'reports'
  },
  { path: '/reports/decisions', component: ReportDecisions, title: 'reports' },
  {
    path: '/reports/partners-in-different-address',
    component: ReportPartnersInDifferentAddress,
    title: 'reports'
  },
  {
    path: '/reports/children-in-different-address',
    component: ReportChildrenInDifferentAddress,
    title: 'reports'
  },
  {
    path: '/reports/child-age-language',
    component: ReportChildAgeLanguage,
    title: 'reports'
  },
  {
    path: '/reports/child-document-decisions',
    component: ReportChildDocumentDecisions,
    title: 'reports'
  },
  {
    path: '/reports/child-documents',
    component: ReportChildDocuments,
    title: 'reports'
  },
  {
    path: '/reports/assistance-needs-and-actions',
    component: ReportAssistanceNeedsAndActions,
    title: 'reports'
  },
  {
    path: '/reports/occupancies',
    component: ReportOccupancies,
    title: 'reports'
  },
  { path: '/reports/invoices', component: ReportInvoices, title: 'reports' },
  {
    path: '/reports/customer-fees',
    component: ReportCustomerFees,
    title: 'reports'
  },
  {
    path: '/reports/starting-placements',
    component: ReportStartingPlacements,
    title: 'reports'
  },
  {
    path: '/reports/ended-placements',
    component: ReportEndedPlacements,
    title: 'reports'
  },
  {
    path: '/reports/exceeded-service-needs',
    component: ReportExceededServiceNeeds,
    title: 'reports'
  },
  {
    path: '/reports/duplicate-people',
    component: ReportDuplicatePeople,
    title: 'reports'
  },
  { path: '/reports/presences', component: ReportPresences, title: 'reports' },
  {
    path: '/reports/service-needs',
    component: ReportServiceNeeds,
    title: 'reports'
  },
  { path: '/reports/sextet', component: ReportSextet, title: 'reports' },
  {
    path: '/reports/voucher-service-providers',
    component: VoucherServiceProviders,
    title: 'reports'
  },
  {
    path: '/reports/voucher-service-providers/:unitId',
    component: VoucherServiceProviderUnit,
    title: 'reports'
  },
  {
    path: '/reports/attendance-reservation',
    component: AttendanceReservation,
    title: 'reports'
  },
  {
    path: '/reports/attendance-reservation-by-child',
    component: AttendanceReservationByChild,
    title: 'reports'
  },
  {
    path: '/reports/varda-child-errors',
    component: VardaChildErrors,
    title: 'reports'
  },
  {
    path: '/reports/varda-unit-errors',
    component: VardaUnitErrors,
    title: 'reports'
  },
  {
    path: '/reports/placement-count',
    component: PlacementCount,
    title: 'reports'
  },
  {
    path: '/reports/placement-sketching',
    component: PlacementSketching,
    title: 'reports'
  },
  { path: '/reports/raw', component: ReportRaw, title: 'reports' },
  {
    path: '/reports/assistance-need-decisions/:id',
    component: AssistanceNeedDecisionsReportDecision,
    title: 'reports'
  },
  {
    path: '/reports/assistance-need-preschool-decisions/:decisionId',
    component: AssistanceNeedDecisionsReportPreschoolDecision,
    title: 'reports'
  },
  {
    path: '/reports/assistance-need-decisions',
    component: AssistanceNeedDecisionsReport,
    title: 'reports'
  },
  {
    path: '/reports/family-daycare-meal-count',
    component: FamilyDaycareMealCount,
    title: 'reports'
  },
  {
    path: '/reports/preschool-absence',
    component: PreschoolAbsenceReport,
    title: 'reports'
  },
  {
    path: '/reports/preschool-application',
    component: PreschoolApplicationReport,
    title: 'reports'
  },
  {
    path: '/reports/future-preschoolers',
    component: FuturePreschoolersReport,
    title: 'reports'
  },
  {
    path: '/reports/placement-guarantee',
    component: PlacementGuarantee,
    title: 'reports'
  },
  { path: '/reports/units', component: ReportUnits, title: 'reports' },
  { path: '/reports/meals', component: MealReport, title: 'reports' },
  { path: '/reports/nekkuorders', component: NekkuOrders, title: 'reports' },
  {
    path: '/reports/child-attendance/:childId',
    component: ChildAttendanceReport,
    title: 'reports'
  },
  {
    path: '/reports/holiday-period-attendance',
    component: HolidayPeriodAttendanceReport,
    title: 'reports'
  },
  {
    path: '/reports/holiday-questionnaire',
    component: HolidayQuestionnaireReport,
    title: 'reports'
  },
  {
    path: '/reports/titania-errors',
    component: TitaniaErrors,
    title: 'reports'
  },
  {
    path: '/reports/incomplete-income',
    component: IncompleteIncomes,
    title: 'reports'
  },
  {
    path: '/reports/tampere-regional-survey',
    component: TampereRegionalSurvey,
    title: 'reports'
  },
  {
    path: '/reports/citizen-document-response',
    component: CitizenDocumentResponseReport,
    title: 'reports'
  },
  { path: '/messages', component: MessagesPage, title: 'messages' },
  {
    path: '/messages/send',
    component: () => <MessagesPage showEditor />,
    title: 'messages'
  },
  {
    path: '/personal-mobile-devices',
    component: PersonalMobileDevicesPage,
    title: 'personalMobileDevices'
  },
  {
    path: '/pin-code',
    component: EmployeePinCodePage,
    title: 'employeePinCode'
  },
  {
    path: '/preferred-first-name',
    component: EmployeePreferredFirstNamePage,
    title: 'preferredFirstName'
  },
  { path: '/employees', component: EmployeesPage, title: 'employees' },
  { path: '/employees/:id', component: EmployeePage, title: 'employees' },
  { path: '/welcome', component: WelcomePage, title: 'welcomePage' },
  {
    path: '/document-templates',
    component: DocumentTemplatesPage,
    title: 'documentTemplates'
  },
  {
    path: '/document-templates/:templateId',
    component: TemplateEditorPage,
    title: 'documentTemplates'
  },
  { path: '/child-documents/:documentId', component: ChildDocumentReadView },
  {
    path: '/child-documents/:documentId/edit',
    component: ChildDocumentEditView
  },
  {
    path: '/holiday-periods',
    component: HolidayAndTermPeriodsPage,
    title: 'holidayAndTermPeriods'
  },
  {
    path: '/holiday-periods/:id',
    component: HolidayPeriodEditor,
    title: 'holidayAndTermPeriods'
  },
  {
    path: '/holiday-periods/questionnaire/:id',
    component: QuestionnaireEditor,
    title: 'holidayQuestionnaire'
  },
  {
    path: '/holiday-periods/club-term/:termId',
    component: ClubTermPeriodEditor,
    title: 'clubTerm'
  },
  {
    path: '/holiday-periods/preschool-term/:termId',
    component: PreschoolTermPeriodEditor,
    title: 'preschoolTerm'
  },
  {
    path: '/placement-tool',
    component: PlacementToolPage,
    title: 'placementTool'
  },
  { path: '/out-of-office', component: OutOfOfficePage, title: 'outOfOffice' },
  {
    path: '/close-after-login',
    component: CloseAfterLogin,
    auth: false,
    title: 'login'
  }
]

const financeRoutes: EmployeeRoute[] = [
  {
    path: '/finance/fee-decisions',
    component: FeeDecisionsPage,
    title: 'feeDecisions'
  },
  {
    path: '/finance/value-decisions',
    component: VoucherValueDecisionsPage,
    title: 'valueDecisions'
  },
  {
    path: '/finance/invoices',
    component: InvoicesPage,
    title: 'invoices'
  },
  {
    path: '/finance/payments',
    component: PaymentsPage,
    title: 'payments',
    disabled: !featureFlags.voucherUnitPayments
  },
  {
    path: '/finance/income-statements',
    component: IncomeStatementsPage,
    title: 'incomeStatements'
  }
]

function renderRoute({
  path,
  component: Component,
  title,
  auth = true,
  hideDefaultTitle,
  disabled
}: EmployeeRoute) {
  if (disabled) return null
  return (
    <Route key={path} path={path}>
      <EmployeeRoute
        title={title}
        requireAuth={auth}
        hideDefaultTitle={hideDefaultTitle}
      >
        <Component />
      </EmployeeRoute>
    </Route>
  )
}

export function Root() {
  return (
    <Router base="/employee">
      <App>
        <Switch>
          {routes.map(renderRoute)}
          <Route path="/finance/*?">
            <FinancePage />
            <Switch>
              {financeRoutes.map(renderRoute)}
              <Route>
                <Redirect replace to="/finance/fee-decisions" />
              </Route>
            </Switch>
          </Route>
          <Route>
            <RedirectToMainPage />
          </Route>
        </Switch>
      </App>
    </Router>
  )
}
