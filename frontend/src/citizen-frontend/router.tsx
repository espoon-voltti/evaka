// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { createBrowserRouter } from 'react-router'

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

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        path: '/login/form',
        element: (
          <ScrollToTop>
            <LoginFormPage />
          </ScrollToTop>
        )
      },
      {
        path: '/login',
        element: (
          <ScrollToTop>
            <LoginPage />
          </ScrollToTop>
        )
      },
      {
        path: '/map',
        element: (
          <ScrollToTop>
            <MapPage />
          </ScrollToTop>
        )
      },
      {
        path: '/accessibility',
        element: (
          <ScrollToTop>
            <AccessibilityStatement />
          </ScrollToTop>
        )
      },
      {
        path: '/applications',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <Applications />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/applications/new/:childId',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <ApplicationCreation />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/applications/:applicationId',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <ApplicationReadView />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/applications/:applicationId/edit',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <ApplicationEditor />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/personal-details',
        element: (
          <RequireAuth strength="WEAK">
            <ScrollToTop>
              <PersonalDetails />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/income',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <IncomeStatements />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/income/:incomeStatementId/edit',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <IncomeStatementEditor />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/income/:incomeStatementId',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <IncomeStatementView />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/child-income/:childId/:incomeStatementId/edit',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <ChildIncomeStatementEditor />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/child-income/:childId/:incomeStatementId',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <ChildIncomeStatementView />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/children/:childId',
        element: (
          <RequireAuth strength="WEAK">
            <ScrollToTop>
              <ChildPage />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      ...(featureFlags.serviceApplications
        ? [
            {
              path: '/children/:childId/service-application',
              element: (
                <RequireAuth strength="STRONG">
                  <ScrollToTop>
                    <NewServiceApplicationPage />
                  </ScrollToTop>
                </RequireAuth>
              )
            }
          ]
        : []),
      ...(featureFlags.absenceApplications
        ? [
            {
              path: '/children/:childId/absence-application',
              element: (
                <ScrollToTop>
                  <NewAbsenceApplicationPage />
                </ScrollToTop>
              )
            }
          ]
        : []),
      {
        path: '/child-documents/:id',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <ChildDocumentPage />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/decisions',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <Decisions />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/decisions/by-application/:applicationId',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <DecisionResponseList />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/decisions/assistance/:id',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <AssistanceDecisionPage />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/decisions/assistance-preschool/:id',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <AssistancePreschoolDecisionPage />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/messages/:threadId',
        element: (
          <RequireAuth strength="WEAK">
            <MessagesPage />
          </RequireAuth>
        )
      },
      {
        path: '/messages',
        element: (
          <RequireAuth strength="WEAK">
            <ScrollToTop>
              <MessagesPage />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/calendar',
        element: (
          <RequireAuth strength="WEAK">
            <ScrollToTop>
              <CalendarPage />
            </ScrollToTop>
          </RequireAuth>
        )
      },
      {
        path: '/*',
        element: <HandleRedirection />
      },
      {
        index: true,
        element: <HandleRedirection />
      }
    ]
  }
])
