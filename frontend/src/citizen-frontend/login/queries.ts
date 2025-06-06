// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { authWeakLogin } from '../auth/api'
import { getCurrentSystemNotificationCitizen } from '../generated/api-clients/systemnotifications'

const q = new Queries()

export const systemNotificationsQuery = q.query(
  getCurrentSystemNotificationCitizen
)

export const authWeakLoginMutation = q.mutation(authWeakLogin, [])
