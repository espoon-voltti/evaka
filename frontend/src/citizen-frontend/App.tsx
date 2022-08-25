// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { OverlayProvider } from '@react-aria/overlays'
import { ErrorBoundary } from '@sentry/react'
import React, { ReactNode, useCallback, useContext, useRef } from 'react'
import { Route, BrowserRouter, Navigate, Routes } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import styled from 'styled-components'

import { scrollElementToPos } from 'lib-common/utils/scrolling'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'

import AccessibilityStatement from './AccessibilityStatement'
import CitizenReloadNotification from './CitizenReloadNotification'
import LoginPage from './LoginPage'
import RequireAuth from './RequireAuth'
import ApplicationCreation from './applications/ApplicationCreation'
import Applications from './applications/Applications'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import { ApplicationsContextProvider } from './applications/state'
import { UnwrapResult } from './async-rendering'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import CalendarPage from './calendar/CalendarPage'
import ChildPage from './children/ChildPage'
import AssistanceNeedDecisionPage from './children/sections/assistance-need-decision/AssistanceNeedDecisionPage'
import VasuPage from './children/sections/vasu-and-leops/vasu/VasuPage'
import { ChildrenContextProvider } from './children/state'
import DecisionResponseList from './decisions/decision-response-page/DecisionResponseList'
import Decisions from './decisions/decisions-page/Decisions'
import { HolidayPeriodsContextProvider } from './holiday-periods/state'
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
                  <HolidayPeriodsContextProvider>
                    <ChildrenContextProvider>
                      <ApplicationsContextProvider>
                        <OverlayProvider>
                          <Content />
                          <GlobalDialog />
                          <LoginErrorModal
                            translations={i18n.login.failedModal}
                          />
                          <div id="modal-container" />
                        </OverlayProvider>
                      </ApplicationsContextProvider>
                    </ChildrenContextProvider>
                  </HolidayPeriodsContextProvider>
                </MessageContextProvider>
              </OverlayContextProvider>
            </AuthContextProvider>
          </ErrorBoundary>
        </Localization>
      </ThemeProvider>
    </BrowserRouter>
  )
}

const FullPageContainer = styled.div`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
`

const Content = React.memo(function Content() {
  const t = useTranslation()

  const { modalOpen } = useContext(OverlayContext)

  const mainRef = useRef<HTMLDivElement>(null)
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
    <FullPageContainer>
      <SkipToContent target="main">{t.skipLinks.mainContent}</SkipToContent>
      <Header ariaHidden={modalOpen} />
      <CitizenReloadNotification />
      <MainContainer ariaHidden={modalOpen} ref={mainRef}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/map"
            element={<MapPage scrollToTop={scrollMainToTop} />}
          />
          <Route
            path="/accessibility"
            element={<AccessibilityStatement scrollToTop={scrollMainToTop} />}
          />
          <Route
            path="/applications"
            element={
              <RequireAuth>
                <Applications />
              </RequireAuth>
            }
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
            path="/applications/:applicationId/edit"
            element={
              <RequireAuth>
                <ApplicationEditor />
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
            path="/children/:childId/assistance-need-decision/:id"
            element={
              <RequireAuth>
                <AssistanceNeedDecisionPage />
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
            path="/decisions"
            element={
              <RequireAuth>
                <Decisions />
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
            path="/vasu/:id"
            element={
              <RequireAuth>
                <VasuPage />
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

// eslint-disable-next-line react/display-name
const MainContainer = React.memo(
  React.forwardRef(function MainContainer(
    {
      ariaHidden,
      children
    }: {
      ariaHidden: boolean
      children: ReactNode
    },
    ref: React.ForwardedRef<HTMLDivElement>
  ) {
    const { user } = useContext(AuthContext)
    const render = useCallback(() => <>{children}</>, [children])
    return (
      <ScrollableMain aria-hidden={ariaHidden} ref={ref}>
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
    <Navigate replace to="/map" />
  )
}

const ScrollableMain = styled.div`
  flex-grow: 1;
`
