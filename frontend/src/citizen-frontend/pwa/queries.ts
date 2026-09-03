// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  addPushSubscription,
  checkPushSubscription,
  deletePushDevice,
  getPushSettings,
  sendTestPushNotification
} from '../generated/api-clients/webpush'

const q = new Queries()

export const pushSettingsQuery = q.query(getPushSettings)

/**
 * Asks the backend whether the browser's subscription is the logged-in
 * citizen's. Run as a query so that every part of the app that needs the answer
 * shares one request per browser subscription.
 */
export const pushSubscriptionCheckQuery = q.query(checkPushSubscription)

export const addPushSubscriptionMutation = q.mutation(addPushSubscription, [
  pushSettingsQuery,
  pushSubscriptionCheckQuery.prefix
])

export const deletePushDeviceMutation = q.mutation(deletePushDevice, [
  pushSettingsQuery,
  pushSubscriptionCheckQuery.prefix
])

export const sendTestPushNotificationMutation = q.mutation(
  sendTestPushNotification,
  [pushSettingsQuery]
)
