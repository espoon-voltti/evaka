// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getCurrentSystemNotificationEmployeeMobile } from '../../generated/api-clients/systemnotifications'

const q = new Queries()

export const currentSystemNotificationQuery = q.query(
  getCurrentSystemNotificationEmployeeMobile,
  {
    staleTime: 5 * 60 * 1000
  }
)
