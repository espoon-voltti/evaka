// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { getChildren } from '../generated/api-clients/children'
import {
  createServiceApplication,
  deleteServiceApplication,
  getChildServiceApplications,
  getChildServiceNeedOptions
} from '../generated/api-clients/serviceneed'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('children', {
  all: () => null,
  serviceApplications: (childId: UUID) => [childId, 'service-applications'],
  serviceNeedOptions: (childId: UUID, date: LocalDate) => [
    childId,
    'service-need-options',
    date
  ]
})

export const childrenQuery = query({
  api: getChildren,
  queryKey: queryKeys.all
})

export const childServiceApplicationsQuery = query({
  api: getChildServiceApplications,
  queryKey: (args) => queryKeys.serviceApplications(args.childId)
})

export const childServiceNeedOptionsQuery = query({
  api: getChildServiceNeedOptions,
  queryKey: (args) => queryKeys.serviceNeedOptions(args.childId, args.date)
})

export const createServiceApplicationsMutation = mutation({
  api: createServiceApplication,
  invalidateQueryKeys: (arg) => [
    queryKeys.serviceApplications(arg.body.childId)
  ]
})

export const deleteServiceApplicationsMutation = mutation({
  api: deleteServiceApplication,
  invalidateQueryKeys: (
    arg: Arg0<typeof deleteServiceApplication> & { childId: UUID }
  ) => [queryKeys.serviceApplications(arg.childId)]
})
