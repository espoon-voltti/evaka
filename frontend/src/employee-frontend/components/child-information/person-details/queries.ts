// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import {
  getDiets,
  getMealTextures
} from '../../../generated/api-clients/specialdiet'
import { createQueryKeys } from '../../../query'

const queryKeys = createQueryKeys('personDetails', {
  specialDiets: () => ['specialDiets'],
  mealTextures: () => ['mealTextures']
})

export const specialDietsQuery = query({
  api: getDiets,
  queryKey: queryKeys.specialDiets
})

export const mealTexturesQuery = query({
  api: getMealTextures,
  queryKey: queryKeys.mealTextures
})
