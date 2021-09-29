// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AuthContext } from './auth/state'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { theme } from 'lib-customizations/common'
import React, { ReactNode, useContext } from 'react'
import { BrowserRouter, Redirect, Route, Switch } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import ApplicationCreation from './applications/ApplicationCreation'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import { Authentication } from './auth'
import { LoginErrorModal } from './auth/LoginErrorModal'
import requireAuth from './auth/requireAuth'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Decisions from './decisions/decisions-page/Decisions'
import Header from './header/Header'
import { Localization } from './localization'
import MessagesPage from './messages/MessagesPage'
import CalendarPage from './calendar/CalendarPage'
import { MessageContextProvider } from './messages/state'
import GlobalErrorDialog from './overlay/Error'
import GlobalInfoDialog from './overlay/Info'
import { OverlayContextProvider } from './overlay/state'
import IncomeStatements from './income-statements/IncomeStatements'
import IncomeStatementEditor from './income-statements/IncomeStatementEditor'
import IncomeStatementView from './income-statements/IncomeStatementView'
import { featureFlags } from 'lib-customizations/citizen'
import Applying from './applying/Applying'
import PedagogicalDocuments from './pedagogical-documents/PedagogicalDocuments'

export default function App() {
  return (
    <BrowserRouter basename="/">
      <ThemeProvider theme={theme}>
        <Authentication>
          <Localization>
            <OverlayContextProvider>
              <MessageContextProvider>
                <Header />
                <Main>
                  <Switch>
                    <Route exact path="/applying/map" component={Applying} />
                    <Route
                      exact
                      path="/applying/applications"
                      component={requireAuth(Applying)}
                    />
                    <Route
                      exact
                      path="/applying/decisions"
                      component={requireAuth(Applying)}
                    />
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
                    {featureFlags.experimental?.incomeStatements && (
                      <Route
                        exact
                        path="/income"
                        component={requireAuth(IncomeStatements)}
                      />
                    )}
                    {featureFlags.experimental?.incomeStatements && (
                      <Route
                        exact
                        path="/income/:incomeStatementId/edit"
                        component={requireAuth(IncomeStatementEditor)}
                      />
                    )}
                    {featureFlags.experimental?.incomeStatements && (
                      <Route
                        exact
                        path="/income/:incomeStatementId"
                        component={requireAuth(IncomeStatementView)}
                      />
                    )}
                    <Route
                      exact
                      path="/applications/:applicationId/edit"
                      component={requireAuth(ApplicationEditor)}
                    />
                    <Route
                      exact
                      path="/decisions"
                      component={requireAuth(Decisions)}
                    />
                    <Route
                      exact
                      path="/decisions/by-application/:applicationId"
                      component={requireAuth(DecisionResponseList)}
                    />
                    <Route
                      exact
                      path="/pedagogical-documents"
                      component={requireAuth(PedagogicalDocuments)}
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
                    <Route path="/">
                      <Redirect to="/applying/map" />
                    </Route>
                  </Switch>
                </Main>
                <GlobalInfoDialog />
                <GlobalErrorDialog />
                <LoginErrorModal />
              </MessageContextProvider>
            </OverlayContextProvider>
          </Localization>
        </Authentication>
      </ThemeProvider>
    </BrowserRouter>
  )
}

function Main({ children }: { children: ReactNode }) {
  const { loading: authStatusLoading } = useContext(AuthContext)
  return <main>{authStatusLoading ? <SpinnerSegment /> : children}</main>
}
