// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ErrorBoundary } from '@sentry/react'
import type { ReactNode } from 'react'
import React, { useCallback, useContext } from 'react'
import styled, { createGlobalStyle, ThemeProvider } from 'styled-components'
import { Redirect } from 'wouter'

import { useRegisterScrollContainer } from 'lib-common/utils/scrolling'
import {
  Notifications,
  NotificationsContextProvider
} from 'lib-components/Notifications'
import { EnvironmentLabel } from 'lib-components/atoms/EnvironmentLabel'
import SkipToContent from 'lib-components/atoms/buttons/SkipToContent'
import { desktopMin, zoomedMobileMax } from 'lib-components/breakpoints'
import ErrorPage from 'lib-components/molecules/ErrorPage'
import { LoginErrorModal } from 'lib-components/molecules/modals/LoginErrorModal'
import SessionExpiredModal from 'lib-components/molecules/modals/SessionExpiredModal'
import { useKeepSessionAlive } from 'lib-components/useKeepSessionAlive'
import { featureFlags } from 'lib-customizations/citizen'
import { theme } from 'lib-customizations/common'

import { useChildrenStartingNotification } from './ChildStartingNotificationHook'
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
import { InstallSuggestion } from './pwa/InstallSuggestion'
import { useStandaloneAttribute } from './pwa/installed'
import { queryClient, QueryClientProvider } from './query'

const GlobalStyle = createGlobalStyle`
  @media screen and (max-width: ${zoomedMobileMax}) {
    html {
      overflow-x: auto;
    }
  }
`

export function App({ children }: { children: React.ReactNode }) {
  const i18n = useTranslation()

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <GlobalStyle />
        <ErrorBoundary
          fallback={() => <ErrorPage basePath="/" labels={i18n.errorPage} />}
        >
          <AuthContextProvider>
            <Localization>
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
            </Localization>
          </AuthContextProvider>
        </ErrorBoundary>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

// The app has the following DOM structure:
//
// <AppShell>
//   <Header />
//   (...other fixed elements, e.g. notifications...)
//   <ScrollArea (flex-grow)>
//     (...content...)
//   </ScrollArea>
//   <MobileNav />
// </AppShell>
//
// Normally the document scrolls, header and other fixed elements stick to the
// top and the mobile navi is fixed to the bottom.
//
// In PWA (standalone), AppShell is a flex column that fills the screen, and
// ScrollArea is the only scrollable element. This avoids anchoring topbar or
// navi with `position: fixed` or `position sticky`, because those cause subtle
// layout bugs in iOS PWA.
//
const AppShell = styled.div`
  display: flex;
  flex-direction: column;
  min-height: 100vh;

  html[data-standalone] & {
    height: 100%;
    min-height: auto;
    position: relative;
    overflow: hidden;

    @media print {
      display: block;
      height: auto;
      overflow: visible;
    }
  }
`

const ScrollArea = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1 0 auto;

  html[data-standalone] & {
    flex: 1 1 auto;
    min-height: 0;
    overflow-x: hidden;
    overflow-y: scroll;
    overscroll-behavior: contain;

    @media screen and (max-width: ${zoomedMobileMax}) {
      overflow-x: auto;
    }

    @media print {
      overflow: visible;
    }
  }
`

const FullPageContainer = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1 0 auto;
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
  useChildrenStartingNotification()
  useStandaloneAttribute()
  const { shellRef, scrollAreaRef } = useRegisterScrollContainer()
  return (
    <AppShell ref={shellRef}>
      <SkipToContent target="main">{t.skipLinks.mainContent}</SkipToContent>
      <Header ariaHidden={modalOpen} />
      <InstallSuggestion />
      <Notifications apiVersion={apiVersion} sticky offsetTop />
      <ScrollArea ref={scrollAreaRef} data-qa="scroll-area">
        <FullPageContainer>
          <MainContainer ariaHidden={modalOpen}>{children}</MainContainer>
        </FullPageContainer>
      </ScrollArea>
      <MobileNav />
      {sessionExpirationDetected && (
        <SessionExpiredModal onClose={() => dismissSessionExpiredDetection()} />
      )}
      {!!featureFlags.environmentLabel && (
        <EnvironmentLabel>{featureFlags.environmentLabel}</EnvironmentLabel>
      )}
    </AppShell>
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

  html[data-standalone] & {
    padding-bottom: 0;
  }
`
