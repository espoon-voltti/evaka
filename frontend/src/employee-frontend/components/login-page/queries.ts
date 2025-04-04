// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getCurrentSystemNotificationEmployee } from '../../generated/api-clients/systemnotifications'

const q = new Queries()

export const currentSystemNotificationQuery = q.query(
  getCurrentSystemNotificationEmployee
)
