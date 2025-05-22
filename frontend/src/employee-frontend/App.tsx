// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { useContext } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { StyleSheetManager, ThemeProvider } from 'styled-components'

import { Notifications } from 'lib-components/Notifications'
import { EnvironmentLabel } from 'lib-components/atoms/EnvironmentLabel'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import SessionExpiredModal from 'lib-components/molecules/modals/SessionExpiredModal'
import { useKeepSessionAlive } from 'lib-components/useKeepSessionAlive'
import { theme } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'

import { Footer } from './components/Footer'
import Header from './components/Header'
import LoginPage from './components/LoginPage'
import MobilePairingModal from './components/MobilePairingModal'
import ErrorMessage from './components/common/ErrorMessage'
import { sessionKeepalive } from './components/common/sessionKeepalive'
import { queryClient, QueryClientProvider } from './query'
import StateProvider from './state/StateProvider'
import { I18nContextProvider, useTranslation } from './state/i18n'
import { UIContext } from './state/ui'
import { UserContext, UserContextProvider } from './state/user'
import { hasRole } from './utils/roles'

export function App() {
  const { i18n } = useTranslation()

  return (
    <QueryClientProvider client={queryClient}>
      <I18nContextProvider>
        <StyleSheetManager shouldForwardProp={shouldForwardProp}>
          <ThemeProvider theme={theme}>
            <ErrorBoundary
              fallback={() => (
                <ErrorPage basePath="/employee" labels={i18n.errorPage} />
              )}
            >
              <UserContextProvider>
                <StateProvider>
                  <Content />
                  <div id="datepicker-container" />
                </StateProvider>
              </UserContextProvider>
            </ErrorBoundary>
          </ThemeProvider>
        </StyleSheetManager>
      </I18nContextProvider>
    </QueryClientProvider>
  )
}

const Content = React.memo(function Content() {
  const { apiVersion, loaded } = useContext(UserContext)

  const {
    loggedIn,
    unauthorizedApiCallDetected,
    dismissUnauthorizedApiCallDetection,
    refreshAuthStatus
  } = useContext(UserContext)
  const { sessionExpirationDetected, dismissSessionExpiredDetection } =
    useKeepSessionAlive(sessionKeepalive, loggedIn)

  const location = useLocation()

  const handleLoginClick = () => {
    window.open('/employee/close-after-login', '_blank')
    const authChecker = () => {
      refreshAuthStatus()
      document.removeEventListener('focusin', authChecker)
    }
    document.addEventListener('focusin', authChecker)
  }

  if (!loaded) return null

  return (
    <>
      <Header />
      <Notifications apiVersion={apiVersion} />

      {/* the matched route element will be inserted at <Outlet /> */}
      <Outlet />

      {!location.pathname.startsWith('/messages') && <Footer />}
      {!!featureFlags.environmentLabel && (
        <EnvironmentLabel>{featureFlags.environmentLabel}</EnvironmentLabel>
      )}
      <ErrorMessage />
      <LoginErrorModal />
      <PairingModal />
      {(unauthorizedApiCallDetected || sessionExpirationDetected) && (
        <SessionExpiredModal
          onLoginClick={handleLoginClick}
          onClose={() => {
            dismissUnauthorizedApiCallDetection()
            dismissSessionExpiredDetection()
          }}
        />
      )}
    </>
  )
})

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

export function CloseAfterLogin() {
  const { loggedIn } = useContext(UserContext)
  if (loggedIn) {
    window.close()
  }
  return loggedIn ? <RedirectToMainPage /> : <LoginPage />
}

export function RedirectToMainPage() {
  const { loggedIn, roles } = useContext(UserContext)

  if (!loggedIn) {
    return <Navigate replace to="/login" />
  }

  if (
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'SPECIAL_EDUCATION_TEACHER')
  ) {
    return <Navigate replace to="/applications" />
  } else if (hasRole(roles, 'UNIT_SUPERVISOR') || hasRole(roles, 'STAFF')) {
    return <Navigate replace to="/units" />
  } else if (hasRole(roles, 'DIRECTOR') || hasRole(roles, 'REPORT_VIEWER')) {
    return <Navigate replace to="/reports" />
  } else if (hasRole(roles, 'MESSAGING')) {
    return <Navigate replace to="/messages" />
  } else if (roles.length === 0) {
    return <Navigate replace to="/welcome" />
  } else {
    return <Navigate replace to="/search" />
  }
}

const PairingModal = React.memo(function GlobalModals() {
  const { uiMode, pairingState, closePairingModal } = useContext(UIContext)

  if (uiMode !== 'pair-mobile-device' || !pairingState) {
    return null
  }

  return (
    <MobilePairingModal closeModal={closePairingModal} {...pairingState.id} />
  )
})
