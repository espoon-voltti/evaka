// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { ReactNode, useCallback, useContext } from 'react'
import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom'
import styled, { StyleSheetManager, ThemeProvider } from 'styled-components'

import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import { theme } from 'lib-customizations/common'

import AccessibilityStatement from './AccessibilityStatement'
import RequireAuth from './RequireAuth'
import ScrollToTop from './ScrollToTop'
import ApplicationCreation from './applications/ApplicationCreation'
import Applications from './applications/Applications'
import ApplicationEditor from './applications/editor/ApplicationEditor'
import ApplicationReadView from './applications/read-view/ApplicationReadView'
import { UnwrapResult } from './async-rendering'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import CalendarPage from './calendar/CalendarPage'
import ChildDocumentPage from './child-documents/ChildDocumentPage'
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
import LoginPage from './login/LoginPage'
import MapPage from './map/MapPage'
import MessagesPage from './messages/MessagesPage'
import { MessageContextProvider } from './messages/state'
import Header from './navigation/Header'
import MobileNav from './navigation/MobileNav'
import GlobalDialog from './overlay/GlobalDialog'
import { OverlayContext, OverlayContextProvider } from './overlay/state'
import PersonalDetails from './personal-details/PersonalDetails'
import { queryClient, QueryClientProvider } from './query'

function App() {
  const i18n = useTranslation()

  return (
    <QueryClientProvider client={queryClient}>
      <StyleSheetManager shouldForwardProp={shouldForwardProp}>
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
      </StyleSheetManager>
    </QueryClientProvider>
  )
}

// This implements the default behavior from styled-components v5
// TODO: Prefix all custom props with $, then remove this
function shouldForwardProp(propName: string, target: unknown) {
  if (typeof target === 'string') {
    // For HTML elements, forward the prop if it is a valid HTML attribute
    return isPropValid(propName)
  }
  // For other elements, forward all props
  return true
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
        <Outlet />
      </MainContainer>
      <MobileNav />
    </FullPageContainer>
  )
})

export default createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
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
        path: '/vasu/:id',
        element: (
          <RequireAuth>
            <ScrollToTop>
              <VasuPage />
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
