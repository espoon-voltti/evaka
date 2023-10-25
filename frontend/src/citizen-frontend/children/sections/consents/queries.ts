// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../../../query'

import {
  getChildConsentNotifications,
  getChildConsents,
  insertChildConsents
} from './api'

const queryKeys = createQueryKeys('childConsents', {
  consents: () => ['consents'],
  notifications: () => ['notifications']
})

export const childConsentsQuery = query({
  api: getChildConsents,
  queryKey: queryKeys.consents
})

export const childConsentNotificationsQuery = query({
  api: getChildConsentNotifications,
  queryKey: queryKeys.notifications
})

export const insertChildConsentsMutation = mutation({
  api: insertChildConsents,
  invalidateQueryKeys: () => [
    childConsentsQuery().queryKey,
    childConsentNotificationsQuery().queryKey
  ]
})
