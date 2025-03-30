// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { simpleApplicationAction } from '../../generated/api-clients/application'
import {
  getPlacementApplications,
  getPlacementDaycares,
  setTrialPlacementUnit
} from '../../generated/api-clients/placementdesktop'

const q = new Queries()

export const placementDaycaresQuery = q.query(getPlacementDaycares)

export const placementApplicationsQuery = q.query(getPlacementApplications)

export const setTrialPlacementUnitMutation = q.mutation(setTrialPlacementUnit)

export const simpleApplicationActionMutation = q.mutation(
  simpleApplicationAction,
  [placementDaycaresQuery.prefix, placementApplicationsQuery]
)
