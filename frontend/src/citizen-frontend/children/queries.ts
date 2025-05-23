// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import { getChildren } from '../generated/api-clients/children'
import {
  getPlacements,
  postPlacementTermination
} from '../generated/api-clients/placement'
import {
  createServiceApplication,
  deleteServiceApplication,
  getChildServiceApplications,
  getChildServiceNeedOptions
} from '../generated/api-clients/serviceneed'

const q = new Queries()

export const childrenQuery = q.query(getChildren)

export const childServiceApplicationsQuery = q.query(
  getChildServiceApplications
)

export const childServiceNeedOptionsQuery = q.query(getChildServiceNeedOptions)

export const createServiceApplicationsMutation = q.mutation(
  createServiceApplication,
  [({ body: { childId } }) => childServiceApplicationsQuery({ childId })]
)

export const deleteServiceApplicationsMutation = q.parametricMutation<{
  childId: ChildId
}>()(deleteServiceApplication, [
  ({ childId }) => childServiceApplicationsQuery({ childId })
])

export const getPlacementsQuery = q.query(getPlacements)

export const terminatePlacementMutation = q.mutation(postPlacementTermination, [
  getPlacementsQuery
])
