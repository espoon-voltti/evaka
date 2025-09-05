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

interface CitizenRoute {
  path: string
  component: React.FunctionComponent
  auth?: 'STRONG' | 'WEAK' | null // STRONG auth is required by default
  disabled?: boolean
}

const routes: CitizenRoute[] = [
  { path: '/login/form', component: LoginFormPage, auth: null },
  { path: '/login', component: LoginPage, auth: null },
  { path: '/map', component: MapPage, auth: null },
  { path: '/accessibility', component: AccessibilityStatement, auth: null },
  { path: '/applications', component: Applications },
  { path: '/applications/new', component: Applications },
  { path: '/applications/new/:childId', component: ApplicationCreation },
  { path: '/applications/:applicationId', component: ApplicationReadView },
  { path: '/applications/:applicationId/edit', component: ApplicationEditor },
  { path: '/personal-details', component: PersonalDetails, auth: 'WEAK' },
  { path: '/income', component: IncomeStatements },
  { path: '/income/:incomeStatementId/edit', component: IncomeStatementEditor },
  { path: '/income/:incomeStatementId', component: IncomeStatementView },
  {
    path: '/child-income/:childId/:incomeStatementId/edit',
    component: ChildIncomeStatementEditor
  },
  {
    path: '/child-income/:childId/:incomeStatementId',
    component: ChildIncomeStatementView
  },
  { path: '/children/:childId', component: ChildPage, auth: 'WEAK' },
  { path: '/child-documents/:id', component: ChildDocumentPage },
  { path: '/decisions', component: Decisions },
  {
    path: '/decisions/by-application/:applicationId',
    component: DecisionResponseList
  },
  { path: '/decisions/assistance/:id', component: AssistanceDecisionPage },
  {
    path: '/decisions/assistance-preschool/:id',
    component: AssistancePreschoolDecisionPage
  },
  { path: '/messages/:threadId', component: MessagesPage, auth: 'WEAK' },
  { path: '/messages', component: MessagesPage, auth: 'WEAK' },
  { path: '/calendar', component: CalendarPage, auth: 'WEAK' },
  {
    path: '/children/:childId/service-application',
    component: NewServiceApplicationPage,
    disabled: !featureFlags.serviceApplications
  },
  {
    path: '/children/:childId/absence-application',
    component: NewAbsenceApplicationPage,
    disabled: !featureFlags.absenceApplications
  }
]

function renderRoute({
  path,
  component: Component,
  auth,
  disabled
}: CitizenRoute) {
  if (disabled) return null
  const inner = (
    <ScrollToTop>
      <Component />
    </ScrollToTop>
  )
  const outer =
    auth !== null ? <RequireAuth strength={auth}>{inner}</RequireAuth> : inner
  return (
    <Route key={path} path={path}>
      {outer}
    </Route>
  )
}

export default function Root() {
  return (
    <Router>
      <App>
        <Switch>
          {routes.map(renderRoute)}
          <Route>
            <HandleRedirection />
          </Route>
        </Switch>
      </App>
    </Router>
  )
}
