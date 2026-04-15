// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Redirect } from 'expo-router'

import { useAuth } from '../src/auth/state'

export default function Index() {
  const { state } = useAuth()
  if (state.status === 'loading') return null
  return state.status === 'signed-in' ? (
    <Redirect href="/(authed)" />
  ) : (
    <Redirect href="/login" />
  )
}
