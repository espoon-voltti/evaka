// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { ReactNode, useCallback, useContext, useRef } from 'react'
import { Route, BrowserRouter, Navigate, Routes } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import styled from 'styled-components'

import { scrollElementToPos } from 'lib-common/utils/scrolling'
import { desktopMin } from 'lib-components/breakpoints'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'

import AccessibilityStatement from './AccessibilityStatement'
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
import ChildDocuments from './child-documents/ChildDocuments'
import { PedagogicalDocumentsContextProvider } from './child-documents/state'
import VasuPage from './child-documents/vasu/VasuPage'
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

  const mainRef = useRef<HTMLElement>(null)
  const scrollMainToTop = useCallback(
    () =>
      scrollElementToPos(mainRef.current, {
        top: 0,
        left: 0,
        behavior: 'auto'
      }),
    []
  )

  return (
    <>
      <Header ariaHidden={modalOpen} />
      <Main ariaHidden={modalOpen} ref={mainRef}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/map"
            element={<MapView scrollToTop={scrollMainToTop} />}
          />
          <Route
            path="/applying/*"
            element={<ApplyingRouter scrollToTop={scrollMainToTop} />}
          />
          <Route
            path="/accessibility"
            element={<AccessibilityStatement scrollToTop={scrollMainToTop} />}
          />
          <Route
            path="/applications/new/:childId"
            element={
              <RequireAuth>
                <ApplicationCreation />
              </RequireAuth>
            }
          />
          <Route
            path="/applications/:applicationId"
            element={
              <RequireAuth>
                <ApplicationReadView />
              </RequireAuth>
            }
          />
          <Route
            path="/personal-details"
            element={
              <RequireAuth strength="WEAK">
                <PersonalDetails />
              </RequireAuth>
            }
          />
          <Route
            path="/income"
            element={
              <RequireAuth>
                <IncomeStatements />
              </RequireAuth>
            }
          />
          <Route
            path="/income/:incomeStatementId/edit"
            element={
              <RequireAuth>
                <IncomeStatementEditor />
              </RequireAuth>
            }
          />
          <Route
            path="/income/:incomeStatementId"
            element={
              <RequireAuth>
                <IncomeStatementView />
              </RequireAuth>
            }
          />
          <Route
            path="/child-income/:childId/:incomeStatementId/edit"
            element={
              <RequireAuth>
                <ChildIncomeStatementEditor />
              </RequireAuth>
            }
          />
          <Route
            path="/child-income/:childId/:incomeStatementId"
            element={
              <RequireAuth>
                <ChildIncomeStatementView />
              </RequireAuth>
            }
          />
          <Route
            path="/applications/:applicationId/edit"
            element={
              <RequireAuth>
                <ApplicationEditor />
              </RequireAuth>
            }
          />
          <Route
            path="/children/:childId"
            element={
              <RequireAuth>
                <ChildPage />
              </RequireAuth>
            }
          />
          <Route
            path="/children"
            element={
              <RequireAuth>
                <ChildrenPage />
              </RequireAuth>
            }
          />
          <Route
            path="/decisions/by-application/:applicationId"
            element={
              <RequireAuth>
                <DecisionResponseList />
              </RequireAuth>
            }
          />
          <Route
            path="/messages/:threadId"
            element={
              <RequireAuth strength="WEAK">
                <MessagesPage />
              </RequireAuth>
            }
          />
          <Route
            path="/messages"
            element={
              <RequireAuth strength="WEAK">
                <MessagesPage />
              </RequireAuth>
            }
          />
          <Route
            path="/calendar"
            element={
              <RequireAuth strength="WEAK">
                <CalendarPage />
              </RequireAuth>
            }
          />
          <Route
            path="/child-documents"
            element={
              <RequireAuth>
                <ChildDocuments />
              </RequireAuth>
            }
          />
          <Route
            path="/vasu/:id"
            element={
              <RequireAuth>
                <VasuPage />
              </RequireAuth>
            }
          />
          <Route index element={<HandleRedirection />} />
        </Routes>
      </Main>
    </>
  )
})

// eslint-disable-next-line react/display-name
const Main = React.memo(
  React.forwardRef(function Main(
    {
      ariaHidden,
      children
    }: {
      ariaHidden: boolean
      children: ReactNode
    },
    ref: React.ForwardedRef<HTMLElement>
  ) {
    const { user } = useContext(AuthContext)
    const render = useCallback(() => <>{children}</>, [children])
    return (
      <ScrollableMain id="main" aria-hidden={ariaHidden} ref={ref}>
        <UnwrapResult result={user}>{render}</UnwrapResult>
      </ScrollableMain>
    )
  })
)

function HandleRedirection() {
  const user = useUser()

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const hasAccessToCalendar = !!user?.accessibleFeatures.reservations
  return hasAccessToCalendar ? (
    <Navigate replace to="/calendar" />
  ) : (
    <Navigate replace to="/applying/map" />
  )
}

const ScrollableMain = styled.main`
  height: calc(100% - ${headerHeightMobile}px);
  overflow: auto;

  @media (min-width: ${desktopMin}) {
    height: calc(100% - ${headerHeightDesktop}px);
  }
`
