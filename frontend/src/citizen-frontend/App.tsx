// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { ReactNode, useCallback, useContext } from 'react'
import { Route, BrowserRouter, Redirect, Switch } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import styled from 'styled-components'

import { desktopMin } from 'lib-components/breakpoints'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'

import CitizenReloadNotification from './CitizenReloadNotification'
import LoginPage from './LoginPage'
import RequireAuth from './RequireAuth'
import ApplicationCreation from './applications/ApplicationCreation'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import ApplyingRouter from './applying/ApplyingRouter'
import { UnwrapResult } from './async-rendering'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import CalendarPage from './calendar/CalendarPage'
import ChildPage from './children/ChildPage'
import ChildrenPage from './children/ChildrenPage'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Header from './header/Header'
import { headerHeightDesktop, headerHeightMobile } from './header/const'
import { HolidayPeriodsContextProvider } from './holiday-periods/state'
import ChildIncomeStatementEditor from './income-statements/ChildIncomeStatementEditor'
import ChildIncomeStatementView from './income-statements/ChildIncomeStatementView'
import IncomeStatementEditor from './income-statements/IncomeStatementEditor'
import IncomeStatementView from './income-statements/IncomeStatementView'
import IncomeStatements from './income-statements/IncomeStatements'
import { Localization, useTranslation } from './localization'
import MapView from './map/MapView'
import MessagesPage from './messages/MessagesPage'
import { MessageContextProvider } from './messages/state'
import GlobalDialog from './overlay/GlobalDialog'
import { OverlayContext, OverlayContextProvider } from './overlay/state'
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
                    <HolidayPeriodsContextProvider>
                      <Content />
                      <GlobalDialog />
                      <LoginErrorModal translations={i18n.login.failedModal} />
                      <CitizenReloadNotification />
                      <div id="modal-container" />
                    </HolidayPeriodsContextProvider>
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

const Content = React.memo(function Content() {
  const { modalOpen } = useContext(OverlayContext)

  return (
    <>
      <Header ariaHidden={modalOpen} />
      <Main ariaHidden={modalOpen}>
        <Switch>
          <Route path="/login" render={() => <LoginPage />} />
          <Route path="/map" render={() => <MapView />} />
          <Route path="/applying" render={() => <ApplyingRouter />} />
          <Route
            exact
            path="/applications/new/:childId"
            render={() => (
              <RequireAuth>
                <ApplicationCreation />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/applications/:applicationId"
            render={() => (
              <RequireAuth>
                <ApplicationReadView />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/personal-details"
            render={() => (
              <RequireAuth strength="WEAK">
                <PersonalDetails />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/income"
            render={() => (
              <RequireAuth>
                <IncomeStatements />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/income/:incomeStatementId/edit"
            render={() => (
              <RequireAuth>
                <IncomeStatementEditor />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/income/:incomeStatementId"
            render={() => (
              <RequireAuth>
                <IncomeStatementView />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/child-income/:childId/:incomeStatementId/edit"
            render={() => (
              <RequireAuth>
                <ChildIncomeStatementEditor />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/child-income/:childId/:incomeStatementId"
            render={() => (
              <RequireAuth>
                <ChildIncomeStatementView />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/applications/:applicationId/edit"
            render={() => (
              <RequireAuth>
                <ApplicationEditor />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/children/:childId"
            render={() => (
              <RequireAuth>
                <ChildPage />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/children"
            render={() => (
              <RequireAuth>
                <ChildrenPage />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/decisions/by-application/:applicationId"
            render={() => (
              <RequireAuth>
                <DecisionResponseList />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/messages"
            render={() => (
              <RequireAuth strength="WEAK">
                <MessagesPage />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/calendar"
            render={() => (
              <RequireAuth strength="WEAK">
                <CalendarPage />
              </RequireAuth>
            )}
          />
          <Route
            exact
            path="/pedagogical-documents"
            render={() => (
              <RequireAuth>
                <PedagogicalDocuments />
              </RequireAuth>
            )}
          />
          <Route path="/" render={() => <HandleRedirection />} />
        </Switch>
      </Main>
    </>
  )
})

function Main({
  ariaHidden,
  children
}: {
  ariaHidden: boolean
  children: ReactNode
}) {
  const { user } = useContext(AuthContext)
  const render = useCallback(() => <>{children}</>, [children])
  return (
    <ScrollableMain id="main" aria-hidden={ariaHidden}>
      <UnwrapResult result={user}>{render}</UnwrapResult>
    </ScrollableMain>
  )
}

function HandleRedirection() {
  const user = useUser()

  if (!user) {
    return <Redirect to="/login" />
  }

  const hasAccessToCalendar = !!user?.accessibleFeatures.reservations
  return hasAccessToCalendar ? (
    <Redirect to="/calendar" />
  ) : (
    <Redirect to="/applying/map" />
  )
}

const ScrollableMain = styled.main`
  height: calc(100% - ${headerHeightMobile}px);
  overflow: auto;

  @media (min-width: ${desktopMin}) {
    height: calc(100% - ${headerHeightDesktop}px);
  }
`
