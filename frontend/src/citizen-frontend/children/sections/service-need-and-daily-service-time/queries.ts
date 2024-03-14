// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { getChildAttendanceSummary } from '../../../generated/api-clients/children'
import { createQueryKeys } from '../../../query'

const queryKeys = createQueryKeys('serviceNeedAndDailyServiceTime', {
  attendanceSummary: (childId: UUID, date: LocalDate) => [
    'attendanceSummary',
    childId,
    date
  ]
})

export const attendanceSummaryQuery = query({
  api: getChildAttendanceSummary,
  queryKey: ({ childId, yearMonth }) =>
    queryKeys.attendanceSummary(childId, yearMonth)
})
