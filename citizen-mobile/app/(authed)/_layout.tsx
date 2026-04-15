// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Redirect, Stack } from 'expo-router'

import { useAuth } from '../../src/auth/state'

export default function AuthedLayout() {
  const { state } = useAuth()
  if (state.status === 'loading') return null
  if (state.status !== 'signed-in') return <Redirect href="/login" />
  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="index" options={{ title: 'eVaka' }} />
      <Stack.Screen name="thread/[id]" options={{ title: '' }} />
      <Stack.Screen name="settings" />
    </Stack>
  )
}
