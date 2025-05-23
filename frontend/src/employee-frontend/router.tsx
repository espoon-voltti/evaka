// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Redirect, Route, Router, Switch } from 'wouter'

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
import IncompleteIncomes from './components/reports/IncompleteIncomeReport'
import ReportInvoices from './components/reports/Invoices'
import ManualDuplicationReport from './components/reports/ManualDuplicationReport'
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

export function Root() {
  return (
    <Router base="/employee">
      <App>
        <Switch>
          <Route path="/login">
            <EmployeeRoute requireAuth={false} title="login">
              <LoginPage />
            </EmployeeRoute>
          </Route>
          <Route path="/settings">
            <EmployeeRoute title="settings">
              <SettingsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/system-notifications">
            <EmployeeRoute title="systemNotifications">
              <SystemNotificationsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/unit-features">
            <EmployeeRoute title="unitFeatures">
              <UnitFeaturesPage />
            </EmployeeRoute>
          </Route>
          <Route path="/units">
            <EmployeeRoute title="units">
              <Units />
            </EmployeeRoute>
          </Route>
          <Route path="/units/new">
            <EmployeeRoute title="createUnit">
              <CreateUnitPage />
            </EmployeeRoute>
          </Route>
          <Route path="/units/:id/details">
            <EmployeeRoute>
              <UnitDetailsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/units/:unitId/family-contacts">
            <EmployeeRoute>
              <ReportFamilyContacts />
            </EmployeeRoute>
          </Route>
          <Route path="/units/:unitId/groups/:groupId/caretakers">
            <EmployeeRoute title="groupCaretakers">
              <GroupCaretakers />
            </EmployeeRoute>
          </Route>
          <Route path="/units/:unitId/groups/:groupId/discussion-reservation-surveys">
            {featureFlags.discussionReservations ? (
              <EmployeeRoute>
                <DiscussionReservationSurveysPage />
              </EmployeeRoute>
            ) : null}
          </Route>
          <Route path="/units/:unitId/groups/:groupId/discussion-reservation-surveys/:eventId">
            {featureFlags.discussionReservations ? (
              <EmployeeRoute>
                <DiscussionReservationSurveyWrapper mode="VIEW" />
              </EmployeeRoute>
            ) : null}
          </Route>
          <Route path="/units/:unitId/groups/:groupId/discussion-reservation-surveys/:eventId/edit">
            {featureFlags.discussionReservations ? (
              <EmployeeRoute>
                <DiscussionReservationSurveyWrapper mode="EDIT" />
              </EmployeeRoute>
            ) : null}
          </Route>
          <Route path="/units/:id/*?">
            <EmployeeRoute>
              <UnitPage />
            </EmployeeRoute>
          </Route>
          <Route path="/search">
            <EmployeeRoute title="customers">
              <Search />
            </EmployeeRoute>
          </Route>
          <Route path="/profile/:id">
            <EmployeeRoute title="personProfile">
              <PersonProfile />
            </EmployeeRoute>
          </Route>
          <Route path="/profile/:personId/timeline">
            <EmployeeRoute title="personTimeline">
              <TimelinePage />
            </EmployeeRoute>
          </Route>
          <Route path="/profile/:personId/income-statement/:incomeStatementId">
            <EmployeeRoute title="incomeStatement">
              <IncomeStatementPage />
            </EmployeeRoute>
          </Route>
          <Route path="/child-information/:id">
            <EmployeeRoute title="childInformation">
              <ChildInformation />
            </EmployeeRoute>
          </Route>
          <Route path="/child-information/:childId/assistance-need-decision/:id">
            <EmployeeRoute
              title="assistanceNeedDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedDecisionPage />
            </EmployeeRoute>
          </Route>
          <Route path="/child-information/:childId/assistance-need-decision/:id/edit">
            <EmployeeRoute
              title="assistanceNeedDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedDecisionEditPage />
            </EmployeeRoute>
          </Route>
          <Route path="/child-information/:childId/assistance-need-preschool-decisions/:decisionId">
            <EmployeeRoute
              title="assistanceNeedPreschoolDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedPreschoolDecisionReadPage />
            </EmployeeRoute>
          </Route>
          <Route path="/child-information/:childId/assistance-need-preschool-decisions/:decisionId/edit">
            <EmployeeRoute
              title="assistanceNeedPreschoolDecision"
              hideDefaultTitle={true}
            >
              <AssistanceNeedPreschoolDecisionEditPage />
            </EmployeeRoute>
          </Route>
          <Route path="/applications">
            <EmployeeRoute title="applications">
              <ApplicationsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/applications/:id">
            <EmployeeRoute title="applications">
              <ApplicationPage />
            </EmployeeRoute>
          </Route>
          <Route path="/applications/:id/placement">
            <EmployeeRoute title="placementDraft">
              <PlacementDraftPage />
            </EmployeeRoute>
          </Route>
          <Route path="/applications/:id/decisions">
            <EmployeeRoute title="decision">
              <DecisionPage />
            </EmployeeRoute>
          </Route>
          <Route path="/finance/basics">
            <EmployeeRoute title="financeBasics">
              <FinanceBasicsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/finance/fee-decisions/:id">
            <EmployeeRoute>
              <FeeDecisionDetailsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/finance/value-decisions/:id">
            <EmployeeRoute>
              <VoucherValueDecisionPage />
            </EmployeeRoute>
          </Route>
          <Route path="/finance/invoices/:id">
            <EmployeeRoute>
              <InvoicePage />
            </EmployeeRoute>
          </Route>
          <Route path="/finance/*?">
            <FinancePage />
            <Switch>
              <Route path="/finance/fee-decisions">
                <EmployeeRoute title="feeDecisions">
                  <FeeDecisionsPage />
                </EmployeeRoute>
              </Route>
              <Route path="/finance/value-decisions">
                <EmployeeRoute title="valueDecisions">
                  <VoucherValueDecisionsPage />
                </EmployeeRoute>
              </Route>
              <Route path="/finance/invoices">
                <EmployeeRoute title="invoices">
                  <InvoicesPage />
                </EmployeeRoute>
              </Route>
              <Route path="/finance/payments">
                {featureFlags.voucherUnitPayments ? (
                  <EmployeeRoute title="payments">
                    <PaymentsPage />
                  </EmployeeRoute>
                ) : (
                  <RedirectToMainPage />
                )}
              </Route>
              <Route path="/finance/income-statements">
                <EmployeeRoute title="incomeStatements">
                  <IncomeStatementsPage />
                </EmployeeRoute>
              </Route>
              <Route>
                <Redirect replace to="/finance/fee-decisions" />
              </Route>
            </Switch>
          </Route>
          <Route path="/reports">
            <EmployeeRoute title="reports">
              <Reports />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/family-conflicts">
            <EmployeeRoute title="reports">
              <ReportFamilyConflicts />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/missing-head-of-family">
            <EmployeeRoute title="reports">
              <ReportMissingHeadOfFamily />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/missing-service-need">
            <EmployeeRoute title="reports">
              <ReportMissingServiceNeed />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/non-ssn-children">
            <EmployeeRoute title="reports">
              <ReportNonSsnChildren />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/applications">
            <EmployeeRoute title="reports">
              <ReportApplications />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/decisions">
            <EmployeeRoute title="reports">
              <ReportDecisions />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/partners-in-different-address">
            <EmployeeRoute title="reports">
              <ReportPartnersInDifferentAddress />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/children-in-different-address">
            <EmployeeRoute title="reports">
              <ReportChildrenInDifferentAddress />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/child-age-language">
            <EmployeeRoute title="reports">
              <ReportChildAgeLanguage />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/child-document-decisions">
            <EmployeeRoute title="reports">
              <ReportChildDocumentDecisions />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/child-documents">
            <EmployeeRoute title="reports">
              <ReportChildDocuments />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/assistance-needs-and-actions">
            <EmployeeRoute title="reports">
              <ReportAssistanceNeedsAndActions />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/occupancies">
            <EmployeeRoute title="reports">
              <ReportOccupancies />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/invoices">
            <EmployeeRoute title="reports">
              <ReportInvoices />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/customer-fees">
            <EmployeeRoute title="reports">
              <ReportCustomerFees />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/starting-placements">
            <EmployeeRoute title="reports">
              <ReportStartingPlacements />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/ended-placements">
            <EmployeeRoute title="reports">
              <ReportEndedPlacements />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/exceeded-service-needs">
            <EmployeeRoute title="reports">
              <ReportExceededServiceNeeds />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/duplicate-people">
            <EmployeeRoute title="reports">
              <ReportDuplicatePeople />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/presences">
            <EmployeeRoute title="reports">
              <ReportPresences />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/service-needs">
            <EmployeeRoute title="reports">
              <ReportServiceNeeds />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/sextet">
            <EmployeeRoute title="reports">
              <ReportSextet />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/voucher-service-providers">
            <EmployeeRoute title="reports">
              <VoucherServiceProviders />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/voucher-service-providers/:unitId">
            <EmployeeRoute title="reports">
              <VoucherServiceProviderUnit />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/attendance-reservation">
            <EmployeeRoute title="reports">
              <AttendanceReservation />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/attendance-reservation-by-child">
            <EmployeeRoute title="reports">
              <AttendanceReservationByChild />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/varda-child-errors">
            <EmployeeRoute title="reports">
              <VardaChildErrors />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/varda-unit-errors">
            <EmployeeRoute title="reports">
              <VardaUnitErrors />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/placement-count">
            <EmployeeRoute title="reports">
              <PlacementCount />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/placement-sketching">
            <EmployeeRoute title="reports">
              <PlacementSketching />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/raw">
            <EmployeeRoute title="reports">
              <ReportRaw />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/assistance-need-decisions/:id">
            <EmployeeRoute title="reports">
              <AssistanceNeedDecisionsReportDecision />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/assistance-need-preschool-decisions/:decisionId">
            <EmployeeRoute title="reports">
              <AssistanceNeedDecisionsReportPreschoolDecision />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/assistance-need-decisions">
            <EmployeeRoute title="reports">
              <AssistanceNeedDecisionsReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/manual-duplication">
            <EmployeeRoute title="reports">
              <ManualDuplicationReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/family-daycare-meal-count">
            <EmployeeRoute title="reports">
              <FamilyDaycareMealCount />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/preschool-absence">
            <EmployeeRoute title="reports">
              <PreschoolAbsenceReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/preschool-application">
            <EmployeeRoute title="reports">
              <PreschoolApplicationReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/future-preschoolers">
            <EmployeeRoute title="reports">
              <FuturePreschoolersReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/placement-guarantee">
            <EmployeeRoute title="reports">
              <PlacementGuarantee />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/units">
            <EmployeeRoute title="reports">
              <ReportUnits />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/meals">
            <EmployeeRoute title="reports">
              <MealReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/nekkuorders">
            <EmployeeRoute title="reports">
              <NekkuOrders />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/child-attendance/:childId">
            <EmployeeRoute title="reports">
              <ChildAttendanceReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/holiday-period-attendance">
            <EmployeeRoute title="reports">
              <HolidayPeriodAttendanceReport />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/titania-errors">
            <EmployeeRoute title="reports">
              <TitaniaErrors />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/incomplete-income">
            <EmployeeRoute title="reports">
              <IncompleteIncomes />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/tampere-regional-survey">
            <EmployeeRoute title="reports">
              <TampereRegionalSurvey />
            </EmployeeRoute>
          </Route>
          <Route path="/reports/citizen-document-response">
            <EmployeeRoute title="reports">
              <CitizenDocumentResponseReport />
            </EmployeeRoute>
          </Route>
          <Route path="/messages">
            <EmployeeRoute title="messages">
              <MessagesPage />
            </EmployeeRoute>
          </Route>
          <Route path="/messages/send">
            <EmployeeRoute title="messages">
              <MessagesPage showEditor />
            </EmployeeRoute>
          </Route>
          <Route path="/personal-mobile-devices">
            <EmployeeRoute title="personalMobileDevices">
              <PersonalMobileDevicesPage />
            </EmployeeRoute>
          </Route>
          <Route path="/pin-code">
            <EmployeeRoute title="employeePinCode">
              <EmployeePinCodePage />
            </EmployeeRoute>
          </Route>
          <Route path="/preferred-first-name">
            <EmployeeRoute title="preferredFirstName">
              <EmployeePreferredFirstNamePage />
            </EmployeeRoute>
          </Route>
          <Route path="/employees">
            <EmployeeRoute title="employees">
              <EmployeesPage />
            </EmployeeRoute>
          </Route>
          <Route path="/employees/:id">
            <EmployeeRoute title="employees">
              <EmployeePage />
            </EmployeeRoute>
          </Route>
          <Route path="/welcome">
            <EmployeeRoute title="welcomePage">
              <WelcomePage />
            </EmployeeRoute>
          </Route>
          <Route path="/document-templates">
            <EmployeeRoute title="documentTemplates">
              <DocumentTemplatesPage />
            </EmployeeRoute>
          </Route>
          <Route path="/document-templates/:templateId">
            <EmployeeRoute title="documentTemplates">
              <TemplateEditorPage />
            </EmployeeRoute>
          </Route>
          <Route path="/child-documents/:documentId">
            <EmployeeRoute>
              <ChildDocumentReadView />
            </EmployeeRoute>
          </Route>
          <Route path="/child-documents/:documentId/edit">
            <EmployeeRoute>
              <ChildDocumentEditView />
            </EmployeeRoute>
          </Route>
          <Route path="/holiday-periods">
            <EmployeeRoute title="holidayAndTermPeriods">
              <HolidayAndTermPeriodsPage />
            </EmployeeRoute>
          </Route>
          <Route path="/holiday-periods/:id">
            <EmployeeRoute title="holidayAndTermPeriods">
              <HolidayPeriodEditor />
            </EmployeeRoute>
          </Route>
          <Route path="/holiday-periods/questionnaire/:id">
            <EmployeeRoute title="holidayQuestionnaire">
              <QuestionnaireEditor />
            </EmployeeRoute>
          </Route>
          <Route path="/holiday-periods/club-term/:termId">
            <EmployeeRoute title="clubTerm">
              <ClubTermPeriodEditor />
            </EmployeeRoute>
          </Route>
          <Route path="/holiday-periods/preschool-term/:termId">
            <EmployeeRoute title="preschoolTerm">
              <PreschoolTermPeriodEditor />
            </EmployeeRoute>
          </Route>
          <Route path="/placement-tool">
            <EmployeeRoute title="placementTool">
              <PlacementToolPage />
            </EmployeeRoute>
          </Route>
          <Route path="/out-of-office">
            <EmployeeRoute title="outOfOffice">
              <OutOfOfficePage />
            </EmployeeRoute>
          </Route>
          <Route path="/close-after-login">
            <EmployeeRoute title="login" requireAuth={false}>
              <CloseAfterLogin />
            </EmployeeRoute>
          </Route>
          <Route>
            <RedirectToMainPage />
          </Route>
        </Switch>
      </App>
    </Router>
  )
}
