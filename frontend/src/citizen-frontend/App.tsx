// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AuthContext } from 'citizen-frontend/auth/state'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { featureFlags } from 'lib-customizations/citizen'
import { theme } from 'lib-customizations/common'
import React, { ReactNode, useContext } from 'react'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import ApplicationCreation from './applications/ApplicationCreation'
import Applications from './applications/Applications'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import { Authentication } from './auth'
import { LoginErrorModal } from './auth/LoginErrorModal'
import requireAuth from './auth/requireAuth'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Decisions from './decisions/decisions-page/Decisions'
import Header from './header/Header'
import { Localization } from './localization'
import MapView from './map/MapView'
import MessagesPage from './messages/MessagesPage'
import { MessageContextProvider } from './messages/state'
import GlobalErrorDialog from './overlay/Error'
import GlobalInfoDialog from './overlay/Info'
import { OverlayContextProvider } from './overlay/state'

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
                    <Route exact path="/" component={MapView} />
                    <Route
                      exact
                      path="/applications"
                      component={requireAuth(Applications)}
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
                    {featureFlags.messaging && (
                      <Route
                        exact
                        path="/messages"
                        component={requireAuth(MessagesPage, false)}
                      />
                    )}
                    <Route path="/" component={RedirectToMap} />
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

function RedirectToMap() {
  window.location.href =
    window.location.host === 'localhost:9094' ? 'http://localhost:9094' : '/'
  return null
}
