// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import type { ReactNode } from 'react'
import React, { lazy, Suspense, useCallback, useContext, useRef } from 'react'
import { Route, Navigate, Routes } from 'react-router-dom'
import { ThemeProvider } from 'styled-components'
import styled from 'styled-components'

import { scrollElementToPos } from 'lib-common/utils/scrolling'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'

import CitizenReloadNotification from './CitizenReloadNotification'
import RequireAuth from './RequireAuth'
import { UnwrapResult } from './async-rendering'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import { PedagogicalDocumentsContextProvider } from './child-documents/state'
import Header from './header/Header'
import { HolidayPeriodsContextProvider } from './holiday-periods/state'
import { Localization, useTranslation } from './localization'
import { MessageContextProvider } from './messages/state'
import GlobalDialog from './overlay/GlobalDialog'
import { OverlayContext, OverlayContextProvider } from './overlay/state'

const LoginPage = lazy(
  () => import(/* webpackChunkName: "LoginPage" */ './LoginPage')
)
const MapPage = lazy(
  () => import(/* webpackChunkName: "MapPage" */ './map/MapPage')
)
const ApplicationCreation = lazy(
  () =>
    import(
      /* webpackChunkName: "ApplicationCreation" */ './applications/ApplicationCreation'
    )
)
const AccessibilityStatement = lazy(
  () =>
    import(
      /* webpackChunkName: "AccessibilityStatement" */ './AccessibilityStatement'
    )
)
const AssistanceNeedDecisionPage = lazy(
  () =>
    import(
      /* webpackChunkName: "AssistanceNeedDecisionPage" */ './children/AssistanceNeedDecisionPage'
    )
)
const ApplicationEditor = lazy(
  () =>
    import(
      /* webpackChunkName: "ApplicationEditor" */ './applications/editor/ApplicationEditor'
    )
)
const ApplicationReadView = lazy(
  () =>
    import(
      /* webpackChunkName: "ApplicationReadView" */ './applications/read-view/ApplicationReadView'
    )
)
const ApplyingRouter = lazy(
  () =>
    import(/* webpackChunkName: "ApplyingRouter" */ './applying/ApplyingRouter')
)
const CalendarPage = lazy(
  () => import(/* webpackChunkName: "CalendarPage" */ './calendar/CalendarPage')
)
const ChildPage = lazy(
  () => import(/* webpackChunkName: "ChildPage" */ './children/ChildPage')
)
const ChildrenPage = lazy(
  () => import(/* webpackChunkName: "ChildrenPage" */ './children/ChildrenPage')
)
const DecisionResponseList = lazy(
  () =>
    import(
      /* webpackChunkName: "DecisionResponseList" */ './decisions/decision-response-page/DecisionResponseList'
    )
)
const ChildIncomeStatementEditor = lazy(
  () =>
    import(
      /* webpackChunkName: "ChildIncomeStatementEditor" */ './income-statements/ChildIncomeStatementEditor'
    )
)
const ChildIncomeStatementView = lazy(
  () =>
    import(
      /* webpackChunkName: "ChildIncomeStatementView" */ './income-statements/ChildIncomeStatementView'
    )
)
const IncomeStatementEditor = lazy(
  () =>
    import(
      /* webpackChunkName: "IncomeStatementEditor" */ './income-statements/IncomeStatementEditor'
    )
)
const IncomeStatementView = lazy(
  () =>
    import(
      /* webpackChunkName: "IncomeStatementView" */ './income-statements/IncomeStatementView'
    )
)
const IncomeStatements = lazy(
  () =>
    import(
      /* webpackChunkName: "IncomeStatements" */ './income-statements/IncomeStatements'
    )
)
const MessagesPage = lazy(
  () => import(/* webpackChunkName: "MessagesPage" */ './messages/MessagesPage')
)
const ChildDocuments = lazy(
  () =>
    import(
      /* webpackChunkName: "ChildDocuments" */ './child-documents/ChildDocuments'
    )
)
const PersonalDetails = lazy(
  () =>
    import(
      /* webpackChunkName: "PersonalDetails" */ './personal-details/PersonalDetails'
    )
)
const VasuPage = lazy(
  () =>
    import(/* webpackChunkName: "VasuPage" */ './child-documents/vasu/VasuPage')
)

export default function App() {
  const i18n = useTranslation()

  return (
    <ThemeProvider theme={theme}>
      <ErrorBoundary
        fallback={() => <ErrorPage basePath="/" labels={i18n.errorPage} />}
      >
        <Localization>
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
        </Localization>
      </ErrorBoundary>
    </ThemeProvider>
  )
}

const FullPageContainer = styled.div`
  display: flex;
  flex-direction: column;
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
        <Suspense fallback={<SpinnerSegment />}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/map"
              element={<MapPage scrollToTop={scrollMainToTop} />}
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
        </Suspense>
      </MainContainer>
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
    <Navigate replace to="/applying/map" />
  )
}

const ScrollableMain = styled.div`
  flex-grow: 1;
`
