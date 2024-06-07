// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getCurrentSystemNotificationEmployeeMobile } from '../../generated/api-clients/systemnotifications'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('topBar', {
  currentSystemNotification: () => ['currentSystemNotification']
})

export const currentSystemNotificationQuery = query({
  api: getCurrentSystemNotificationEmployeeMobile,
  queryKey: queryKeys.currentSystemNotification,
  options: {
    staleTime: 5 * 60 * 1000
  }
})
