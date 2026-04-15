// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Stack } from 'expo-router'
import { StatusBar } from 'expo-status-bar'
import { PaperProvider } from 'react-native-paper'

import { AuthProvider } from '../src/auth/state'
import { evakaTheme } from '../src/theme'

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } }
})

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <PaperProvider theme={evakaTheme}>
        <AuthProvider>
          <StatusBar style="auto" />
          <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="login" />
            <Stack.Screen name="(authed)" />
          </Stack>
        </AuthProvider>
      </PaperProvider>
    </QueryClientProvider>
  )
}
