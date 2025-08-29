// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isPropValid from '@emotion/is-prop-valid'
import { ErrorBoundary } from '@sentry/react'
import type { ReactNode } from 'react'
import React, { useCallback, useContext } from 'react'
import styled, { StyleSheetManager, ThemeProvider } from 'styled-components'
import { Redirect } from 'wouter'

import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import { EnvironmentLabel } from 'lib-components/atoms/EnvironmentLabel'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import { desktopMin } from 'lib-components/breakpoints'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import SessionExpiredModal from 'lib-components/molecules/modals/SessionExpiredModal'
import { useKeepSessionAlive } from 'lib-components/useKeepSessionAlive'
import { featureFlags } from 'lib-customizations/citizen'
import { theme } from 'lib-customizations/common'

import { UnwrapResult } from './async-rendering'
import { AuthContext, AuthContextProvider, useUser } from './auth/state'
import { sessionKeepalive } from './auth/utils'
import { Localization, useTranslation } from './localization'
import { MessageContextProvider } from './messages/state'
import Header from './navigation/Header'
import MobileNav from './navigation/MobileNav'
import { mobileBottomNavHeight } from './navigation/const'
import GlobalDialog from './overlay/GlobalDialog'
import { OverlayContext, OverlayContextProvider } from './overlay/state'
import { queryClient, QueryClientProvider } from './query'

export function App({ children }: { children: React.ReactNode }) {
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
                      <Content>{children}</Content>
                      <GlobalDialog />
                      <LoginErrorModal />
                      <div id="modal-container" />
                      <div id="datepicker-container" />
                      <div id="tooltip-container" />
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

const Content = React.memo(function Content({
  children
}: {
  children: React.ReactNode
}) {
  const t = useTranslation()
  const { apiVersion } = useContext(AuthContext)
  const { modalOpen } = useContext(OverlayContext)

  const { user } = useContext(AuthContext)
  const { sessionExpirationDetected, dismissSessionExpiredDetection } =
    useKeepSessionAlive(
      sessionKeepalive,
      user.map((usr) => !!usr).getOrElse(false)
    )
  return (
    <FullPageContainer>
      <SkipToContent target="main">{t.skipLinks.mainContent}</SkipToContent>
      <Header ariaHidden={modalOpen} />
      <Notifications apiVersion={apiVersion} sticky offsetTop />
      <MainContainer ariaHidden={modalOpen}>{children}</MainContainer>
      <MobileNav />
      {sessionExpirationDetected && (
        <SessionExpiredModal onClose={() => dismissSessionExpiredDetection()} />
      )}
      {!!featureFlags.environmentLabel && (
        <EnvironmentLabel>{featureFlags.environmentLabel}</EnvironmentLabel>
      )}
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

export function HandleRedirection() {
  const user = useUser()

  if (!user) {
    return <Redirect replace to="/login" />
  }

  const fallbackUrl = user.authLevel !== 'STRONG' ? '/map' : '/applications'
  const hasAccessToCalendar = !!user?.accessibleFeatures.reservations
  return hasAccessToCalendar ? (
    <Redirect replace to="/calendar" />
  ) : (
    <Redirect replace to={fallbackUrl} />
  )
}

const ScrollableMain = styled.div`
  flex-grow: 1;

  padding-bottom: ${mobileBottomNavHeight}px;
  @media (min-width: ${desktopMin}) {
    padding-bottom: 0;
  }
`
