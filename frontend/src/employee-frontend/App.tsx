// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import React, { useContext } from 'react'
import { StyleSheetManager, ThemeProvider } from 'styled-components'
import { Redirect, useLocation } from 'wouter'

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

export function Navigate(props: { replace?: boolean; to: string }) {
  return <Redirect to={props.to} replace={props.replace} />
}

export function App({ children }: { children?: React.ReactNode }) {
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
                  <Content>{children}</Content>
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

function Content({ children }: { children?: React.ReactNode }) {
  const { apiVersion, loaded } = useContext(UserContext)

  const {
    loggedIn,
    unauthorizedApiCallDetected,
    dismissUnauthorizedApiCallDetection,
    refreshAuthStatus
  } = useContext(UserContext)
  const { sessionExpirationDetected, dismissSessionExpiredDetection } =
    useKeepSessionAlive(sessionKeepalive, loggedIn)

  const [path] = useLocation()

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

      {children}

      {!path.startsWith('/messages') && <Footer />}
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
    return <Navigate replace to="~/employee/login" />
  }

  if (
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'SPECIAL_EDUCATION_TEACHER')
  ) {
    return <Navigate replace to="~/employee/applications" />
  } else if (hasRole(roles, 'UNIT_SUPERVISOR') || hasRole(roles, 'STAFF')) {
    return <Navigate replace to="~/employee/units" />
  } else if (hasRole(roles, 'DIRECTOR') || hasRole(roles, 'REPORT_VIEWER')) {
    return <Navigate replace to="~/employee/reports" />
  } else if (hasRole(roles, 'MESSAGING')) {
    return <Navigate replace to="~/employee/messages" />
  } else if (roles.length === 0) {
    return <Navigate replace to="~/employee/welcome" />
  } else {
    return <Navigate replace to="~/employee/search" />
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
