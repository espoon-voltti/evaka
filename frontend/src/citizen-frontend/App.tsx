// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'
import React, { ReactNode, useCallback, useContext } from 'react'
import { BrowserRouter, Redirect, Route, Switch } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import ApplicationCreation from './applications/ApplicationCreation'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import ApplyingRouter from './applying/ApplyingRouter'
import { UnwrapResult } from './async-rendering'
import requireAuth from './auth/requireAuth'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import CalendarPage from './calendar/CalendarPage'
import ChildPage from './children/ChildPage'
import ChildrenPage from './children/ChildrenPage'
import CitizenReloadNotification from './CitizenReloadNotification'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Header from './header/Header'
import IncomeStatementEditor from './income-statements/IncomeStatementEditor'
import IncomeStatements from './income-statements/IncomeStatements'
import IncomeStatementView from './income-statements/IncomeStatementView'
import { Localization, useTranslation } from './localization'
import MessagesPage from './messages/MessagesPage'
import { MessageContextProvider } from './messages/state'
import GlobalErrorDialog from './overlay/Error'
import GlobalInfoDialog from './overlay/Info'
import { OverlayContextProvider } from './overlay/state'
import PedagogicalDocuments from './pedagogical-documents/PedagogicalDocuments'
import { PedagogicalDocumentsContextProvider } from './pedagogical-documents/state'
import PersonalDetails from './personal-details/PersonalDetails'

export default function App() {
  const i18n = useTranslation()

  return (
    <BrowserRouter basename="/">
      <ThemeProvider theme={theme}>
        <Localization>
          <ErrorBoundary
            fallback={() => <ErrorPage basePath="/" labels={i18n.errorPage} />}
          >
            <AuthContextProvider>
              <OverlayContextProvider>
                <MessageContextProvider>
                  <PedagogicalDocumentsContextProvider>
                    <Header />
                    <Main>
                      <Switch>
                        <Route path="/applying" component={ApplyingRouter} />
                        <Route
                          exact
                          path="/applications/new/:childId"
                          component={requireAuth(ApplicationCreation)}
                        />
                        <Route
                          exact
                          path="/applications/:applicationId"
                          component={requireAuth(ApplicationReadView)}
                        />
                        <Route
                          exact
                          path="/personal-details"
                          component={requireAuth(PersonalDetails, false)}
                        />
                        <Route
                          exact
                          path="/income"
                          component={requireAuth(IncomeStatements)}
                        />
                        <Route
                          exact
                          path="/income/:incomeStatementId/edit"
                          component={requireAuth(IncomeStatementEditor)}
                        />
                        <Route
                          exact
                          path="/income/:incomeStatementId"
                          component={requireAuth(IncomeStatementView)}
                        />
                        <Route
                          exact
                          path="/applications/:applicationId/edit"
                          component={requireAuth(ApplicationEditor)}
                        />
                        <Route
                          exact
                          path="/children/:childId"
                          component={requireAuth(ChildPage)}
                        />
                        <Route
                          exact
                          path="/children"
                          component={requireAuth(ChildrenPage)}
                        />
                        <Route
                          exact
                          path="/decisions/by-application/:applicationId"
                          component={requireAuth(DecisionResponseList)}
                        />
                        <Route
                          exact
                          path="/messages"
                          component={requireAuth(MessagesPage, false)}
                        />
                        <Route
                          exact
                          path="/calendar"
                          component={requireAuth(CalendarPage, false)}
                        />
                        <Route
                          exact
                          path="/pedagogical-documents"
                          component={requireAuth(PedagogicalDocuments)}
                        />
                        <Route path="/">
                          <HandleRedirection />
                        </Route>
                      </Switch>
                    </Main>
                    <GlobalInfoDialog />
                    <GlobalErrorDialog />
                    <CitizenReloadNotification />
                    <LoginErrorModal translations={i18n.login.failedModal} />
                  </PedagogicalDocumentsContextProvider>
                </MessageContextProvider>
              </OverlayContextProvider>
            </AuthContextProvider>
          </ErrorBoundary>
        </Localization>
      </ThemeProvider>
    </BrowserRouter>
  )
}

function HandleRedirection() {
  const user = useUser()
  const hasAccessToCalendar = !!user?.accessibleFeatures.reservations
  return hasAccessToCalendar ? (
    <Redirect to="/calendar" />
  ) : (
    <Redirect to="/applying/map" />
  )
}

function Main({ children }: { children: ReactNode }) {
  const { user } = useContext(AuthContext)
  const render = useCallback(() => <>{children}</>, [children])
  return (
    <main>
      <UnwrapResult result={user}>{render}</UnwrapResult>
    </main>
  )
}
