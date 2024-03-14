// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getChildren } from '../generated/api-clients/children'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('children', {
  all: () => null
})

export const childrenQuery = query({
  api: getChildren,
  queryKey: queryKeys.all
})
