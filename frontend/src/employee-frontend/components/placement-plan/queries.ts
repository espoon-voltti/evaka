// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createPlacementPlan,
  getPlacementPlanDraft
} from '../../generated/api-clients/application'
import { getApplicationUnits } from '../../generated/api-clients/daycare'

const q = new Queries()

export const placementPlanDraftQuery = q.query(getPlacementPlanDraft)
export const applicationUnitsQuery = q.query(getApplicationUnits)
export const createPlacementPlanMutation = q.mutation(createPlacementPlan)
