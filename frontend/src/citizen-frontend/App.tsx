// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import React, { ReactNode, useCallback, useContext } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import styled, { ThemeProvider } from 'styled-components'

import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'

import AccessibilityStatement from './AccessibilityStatement'
import LoginPage from './LoginPage'
import RequireAuth from './RequireAuth'
import ScrollToTop from './ScrollToTop'
import ApplicationCreation from './applications/ApplicationCreation'
import Applications from './applications/Applications'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import { UnwrapResult } from './async-rendering'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import CalendarPage from './calendar/CalendarPage'
import ChildPage from './children/ChildPage'
import VasuPage from './children/sections/vasu-and-leops/vasu/VasuPage'
import AssistanceDecisionPage from './decisions/assistance-decision-page/AssistanceDecisionPage'
import AssistancePreschoolDecisionPage from './decisions/assistance-decision-page/AssistancePreschoolDecisionPage'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Decisions from './decisions/decisions-page/Decisions'
import ChildIncomeStatementEditor from './income-statements/ChildIncomeStatementEditor'
import ChildIncomeStatementView from './income-statements/ChildIncomeStatementView'
import IncomeStatementEditor from './income-statements/IncomeStatementEditor'
import IncomeStatementView from './income-statements/IncomeStatementView'
import IncomeStatements from './income-statements/IncomeStatements'
import { Localization, useTranslation } from './localization'
import MapPage from './map/MapPage'
import MessagesPage from './messages/MessagesPage'
import { MessageContextProvider } from './messages/state'
import Header from './navigation/Header'
import MobileNav from './navigation/MobileNav'
import GlobalDialog from './overlay/GlobalDialog'
import { OverlayContext, OverlayContextProvider } from './overlay/state'
import PersonalDetails from './personal-details/PersonalDetails'
import { queryClient, QueryClientProvider } from './query'

export default function App() {
  const i18n = useTranslation()

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename="/">
        <ThemeProvider theme={theme}>
          <Localization>
            <ErrorBoundary
              fallback={() => (
                <ErrorPage basePath="/" labels={i18n.errorPage} />
              )}
            >
              <AuthContextProvider>
                <OverlayContextProvider>
                  <NotificationsContextProvider>
                    <MessageContextProvider>
                      <Content />
                      <GlobalDialog />
                      <LoginErrorModal />
                      <div id="modal-container" />
                    </MessageContextProvider>
                  </NotificationsContextProvider>
                </OverlayContextProvider>
              </AuthContextProvider>
            </ErrorBoundary>
          </Localization>
        </ThemeProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

const FullPageContainer = styled.div`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
`

const Content = React.memo(function Content() {
  const t = useTranslation()
  const { apiVersion } = useContext(AuthContext)
  const { modalOpen } = useContext(OverlayContext)

  return (
    <FullPageContainer>
      <SkipToContent target="main">{t.skipLinks.mainContent}</SkipToContent>
      <Header ariaHidden={modalOpen} />
      <Notifications apiVersion={apiVersion} />
      <MainContainer ariaHidden={modalOpen}>
        <Routes>
          <Route
            path="/login"
            element={
              <ScrollToTop>
                <LoginPage />
              </ScrollToTop>
            }
          />
          <Route
            path="/map"
            element={
              <ScrollToTop>
                <MapPage />
              </ScrollToTop>
            }
          />
          <Route
            path="/accessibility"
            element={
              <ScrollToTop>
                <AccessibilityStatement />
              </ScrollToTop>
            }
          />
          <Route
            path="/applications"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <Applications />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/applications/new/:childId"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <ApplicationCreation />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/applications/:applicationId"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <ApplicationReadView />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/applications/:applicationId/edit"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <ApplicationEditor />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/personal-details"
            element={
              <RequireAuth strength="WEAK">
                <ScrollToTop>
                  <PersonalDetails />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/income"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <IncomeStatements />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/income/:incomeStatementId/edit"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <IncomeStatementEditor />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/income/:incomeStatementId"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <IncomeStatementView />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/child-income/:childId/:incomeStatementId/edit"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <ChildIncomeStatementEditor />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/child-income/:childId/:incomeStatementId"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <ChildIncomeStatementView />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/children/:childId"
            element={
              <RequireAuth strength="WEAK">
                <ScrollToTop>
                  <ChildPage />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/decisions"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <Decisions />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/decisions/by-application/:applicationId"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <DecisionResponseList />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/decisions/assistance/:id"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <AssistanceDecisionPage />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/decisions/assistance-preschool/:id"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <AssistancePreschoolDecisionPage />
                </ScrollToTop>
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
                <ScrollToTop>
                  <MessagesPage />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/calendar"
            element={
              <RequireAuth strength="WEAK">
                <ScrollToTop>
                  <CalendarPage />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route
            path="/vasu/:id"
            element={
              <RequireAuth>
                <ScrollToTop>
                  <VasuPage />
                </ScrollToTop>
              </RequireAuth>
            }
          />
          <Route path="*" element={<HandleRedirection />} />
        </Routes>
      </MainContainer>
      <MobileNav />
    </FullPageContainer>
  )
})

const MainContainer = React.memo(function MainContainer({
  ariaHidden,
  children
}: {
  ariaHidden: boolean
  children: ReactNode
}) {
  const { user } = useContext(AuthContext)
  const render = useCallback(() => <>{children}</>, [children])
  return (
    <ScrollableMain aria-hidden={ariaHidden}>
      <UnwrapResult result={user}>{render}</UnwrapResult>
    </ScrollableMain>
  )
})

function HandleRedirection() {
  const user = useUser()

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const hasAccessToCalendar = !!user?.accessibleFeatures.reservations
  return hasAccessToCalendar ? (
    <Navigate replace to="/calendar" />
  ) : (
    <Navigate replace to="/map" />
  )
}

const ScrollableMain = styled.div`
  flex-grow: 1;
`
