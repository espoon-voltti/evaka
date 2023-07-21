// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createDaycare, getDaycare, updateDaycare } from '../../api/unit'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('unit', {
  unit: (unitId: UUID) => ['unit', unitId]
})

export const unitQuery = query({
  api: getDaycare,
  queryKey: queryKeys.unit
})

export const createUnitMutation = mutation({
  api: createDaycare
})

export const updateUnitMutation = mutation({
  api: updateDaycare,
  invalidateQueryKeys: ({ id }) => [queryKeys.unit(id)]
})
