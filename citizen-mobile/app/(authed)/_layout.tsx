// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Redirect, Stack } from 'expo-router'
import { useEffect } from 'react'

import { useAuth } from '../../src/auth/state'
import { registerForPushNotifications } from '../../src/push/register'

export default function AuthedLayout() {
  const { state } = useAuth()
  const token = state.status === 'signed-in' ? state.token : null

  useEffect(() => {
    if (token) void registerForPushNotifications(token)
  }, [token])

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
