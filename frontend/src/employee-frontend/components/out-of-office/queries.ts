// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import {
  deleteOutOfOfficePeriod,
  getOutOfOfficePeriods,
  upsertOutOfOfficePeriod
} from '../../generated/api-clients/outofoffice'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('outOfOffice', {
  periods: () => ['outOfOfficePeriods']
})

export const outOfOfficePeriodsQuery = query({
  api: getOutOfOfficePeriods,
  queryKey: queryKeys.periods
})

export const deleteOutOfOfficePeriodMutation = mutation({
  api: deleteOutOfOfficePeriod,
  invalidateQueryKeys: () => [queryKeys.periods()]
})

export const upsertOutOfOfficePeriodMutation = mutation({
  api: (arg: Arg0<typeof upsertOutOfOfficePeriod>) =>
    upsertOutOfOfficePeriod(arg),
  invalidateQueryKeys: () => [queryKeys.periods()]
})
