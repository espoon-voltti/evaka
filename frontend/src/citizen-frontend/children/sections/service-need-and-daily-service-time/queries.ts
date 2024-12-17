// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildId } from 'lib-common/generated/api-types/shared'
import { query } from 'lib-common/query'
import YearMonth from 'lib-common/year-month'

import { getChildAttendanceSummary } from '../../../generated/api-clients/children'
import { createQueryKeys } from '../../../query'

const queryKeys = createQueryKeys('serviceNeedAndDailyServiceTime', {
  attendanceSummary: (childId: ChildId, month: YearMonth) => [
    'attendanceSummary',
    childId,
    month
  ]
})

export const attendanceSummaryQuery = query({
  api: getChildAttendanceSummary,
  queryKey: ({ childId, yearMonth }) =>
    queryKeys.attendanceSummary(childId, yearMonth)
})
