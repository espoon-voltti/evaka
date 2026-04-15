// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Constants from 'expo-constants'
import * as Device from 'expo-device'
import * as Notifications from 'expo-notifications'

import { deletePushSubscription, upsertPushSubscription } from '../api/push'

import { getOrCreateDeviceId } from './deviceId'

export async function registerForPushNotifications(
  token: string
): Promise<void> {
  if (!Device.isDevice) return

  const perm = await Notifications.getPermissionsAsync()
  let granted = perm.granted
  if (!granted && perm.canAskAgain) {
    const req = await Notifications.requestPermissionsAsync()
    granted = req.granted
  }
  if (!granted) return

  const projectId =
    (Constants.expoConfig?.extra?.eas as { projectId?: string } | undefined)
      ?.projectId ??
    (Constants.easConfig as { projectId?: string } | undefined)?.projectId

  const expoToken = await Notifications.getExpoPushTokenAsync(
    projectId ? { projectId } : {}
  )

  const deviceId = await getOrCreateDeviceId()
  await upsertPushSubscription(token, deviceId, expoToken.data)
}

export async function unregisterFromPushNotifications(
  token: string
): Promise<void> {
  const deviceId = await getOrCreateDeviceId()
  try {
    await deletePushSubscription(token, deviceId)
  } catch {
    // ignore
  }
}
