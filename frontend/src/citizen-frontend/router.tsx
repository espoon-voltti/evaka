// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Route, Router, Switch } from 'wouter'

import { featureFlags } from 'lib-customizations/citizen'

import AccessibilityStatement from './AccessibilityStatement'
import { App, HandleRedirection } from './App'
import RequireAuth from './RequireAuth'
import ScrollToTop from './ScrollToTop'
import ApplicationCreation from './applications/ApplicationCreation'
import Applications from './applications/Applications'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import CalendarPage from './calendar/CalendarPage'
import ChildDocumentPage from './child-documents/ChildDocumentPage'
import ChildPage from './children/ChildPage'
import { NewAbsenceApplicationPage } from './children/sections/absence-applications/NewAbsenceApplicationPage'
import NewServiceApplicationPage from './children/sections/service-need-and-daily-service-time/NewServiceApplicationPage'
import AssistanceDecisionPage from './decisions/assistance-decision-page/AssistanceDecisionPage'
import AssistancePreschoolDecisionPage from './decisions/assistance-decision-page/AssistancePreschoolDecisionPage'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Decisions from './decisions/decisions-page/Decisions'
import ChildIncomeStatementEditor from './income-statements/ChildIncomeStatementEditor'
import ChildIncomeStatementView from './income-statements/ChildIncomeStatementView'
import IncomeStatementEditor from './income-statements/IncomeStatementEditor'
import IncomeStatementView from './income-statements/IncomeStatementView'
import IncomeStatements from './income-statements/IncomeStatements'
import LoginPage from './login/LoginPage'
import LoginFormPage from './login/WeakLoginFormPage'
import MapPage from './map/MapPage'
import MessagesPage from './messages/MessagesPage'
import PersonalDetails from './personal-details/PersonalDetails'

export default function Root() {
  return (
    <Router>
      <App>
        <Switch>
          <Route path="/login/form">
            <ScrollToTop>
              <LoginFormPage />
            </ScrollToTop>
          </Route>
          <Route path="/login">
            <ScrollToTop>
              <LoginPage />
            </ScrollToTop>
          </Route>
          <Route path="/map">
            <ScrollToTop>
              <MapPage />
            </ScrollToTop>
          </Route>
          <Route path="/accessibility">
            <ScrollToTop>
              <AccessibilityStatement />
            </ScrollToTop>
          </Route>
          <Route path="/applications">
            <RequireAuth>
              <ScrollToTop>
                <Applications />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/applications/new/:childId">
            <RequireAuth>
              <ScrollToTop>
                <ApplicationCreation />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/applications/:applicationId">
            <RequireAuth>
              <ScrollToTop>
                <ApplicationReadView />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/applications/:applicationId/edit">
            <RequireAuth>
              <ScrollToTop>
                <ApplicationEditor />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/personal-details">
            <RequireAuth strength="WEAK">
              <ScrollToTop>
                <PersonalDetails />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/income">
            <RequireAuth>
              <ScrollToTop>
                <IncomeStatements />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/income/:incomeStatementId/edit">
            <RequireAuth>
              <ScrollToTop>
                <IncomeStatementEditor />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/income/:incomeStatementId">
            <RequireAuth>
              <ScrollToTop>
                <IncomeStatementView />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/child-income/:childId/:incomeStatementId/edit">
            <RequireAuth>
              <ScrollToTop>
                <ChildIncomeStatementEditor />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/child-income/:childId/:incomeStatementId">
            <RequireAuth>
              <ScrollToTop>
                <ChildIncomeStatementView />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/children/:childId">
            <RequireAuth strength="WEAK">
              <ScrollToTop>
                <ChildPage />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          {featureFlags.serviceApplications ? (
            <Route path="/children/:childId/service-application">
              <RequireAuth strength="STRONG">
                <ScrollToTop>
                  <NewServiceApplicationPage />
                </ScrollToTop>
              </RequireAuth>
            </Route>
          ) : null}
          {featureFlags.absenceApplications ? (
            <Route path="/children/:childId/absence-application">
              <ScrollToTop>
                <NewAbsenceApplicationPage />
              </ScrollToTop>
            </Route>
          ) : null}
          <Route path="/child-documents/:id">
            <RequireAuth>
              <ScrollToTop>
                <ChildDocumentPage />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/decisions">
            <RequireAuth>
              <ScrollToTop>
                <Decisions />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/decisions/by-application/:applicationId">
            <RequireAuth>
              <ScrollToTop>
                <DecisionResponseList />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/decisions/assistance/:id">
            <RequireAuth>
              <ScrollToTop>
                <AssistanceDecisionPage />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/decisions/assistance-preschool/:id">
            <RequireAuth>
              <ScrollToTop>
                <AssistancePreschoolDecisionPage />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/messages/:threadId">
            <RequireAuth strength="WEAK">
              <MessagesPage />
            </RequireAuth>
          </Route>
          <Route path="/messages">
            <RequireAuth strength="WEAK">
              <ScrollToTop>
                <MessagesPage />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route path="/calendar">
            <RequireAuth strength="WEAK">
              <ScrollToTop>
                <CalendarPage />
              </ScrollToTop>
            </RequireAuth>
          </Route>
          <Route>
            <HandleRedirection />
          </Route>
        </Switch>
      </App>
    </Router>
  )
}
