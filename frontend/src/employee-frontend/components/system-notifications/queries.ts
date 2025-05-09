// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  deleteSystemNotification,
  getAllSystemNotifications,
  putSystemNotificationCitizens,
  putSystemNotificationEmployees
} from '../../generated/api-clients/systemnotifications'

const q = new Queries()

export const allSystemNotificationsQuery = q.query(getAllSystemNotifications)

export const putSystemNotificationCitizensMutation = q.mutation(
  putSystemNotificationCitizens,
  [allSystemNotificationsQuery]
)

export const putSystemNotificationEmployeesMutation = q.mutation(
  putSystemNotificationEmployees,
  [allSystemNotificationsQuery]
)

export const deleteSystemNotificationMutation = q.mutation(
  deleteSystemNotification,
  [allSystemNotificationsQuery]
)
