// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildId } from 'lib-common/generated/api-types/shared'

import { getChildren } from '../generated/api-clients/children'
import {
  createServiceApplication,
  deleteServiceApplication,
  getChildServiceApplications,
  getChildServiceNeedOptions
} from '../generated/api-clients/serviceneed'
import { queries } from '../query'

const q = queries('children')

export const childrenQuery = q.query(getChildren)

export const childServiceApplicationsQuery = q.query(
  getChildServiceApplications
)

export const childServiceNeedOptionsQuery = q.query(getChildServiceNeedOptions)

export const createServiceApplicationsMutation = q.mutation(
  createServiceApplication,
  [({ body: { childId } }) => childServiceApplicationsQuery({ childId })]
)

export const deleteServiceApplicationsMutation =
  q.parametricMutation<ChildId>()(deleteServiceApplication, [
    (childId) => childServiceApplicationsQuery({ childId })
  ])
