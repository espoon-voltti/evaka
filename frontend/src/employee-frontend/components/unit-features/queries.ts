// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getUnitFeatures,
  updateUnitFeatures
} from '../../generated/api-clients/daycare'

const q = new Queries()

export const unitFeaturesQuery = q.query(getUnitFeatures)

export const updateUnitFeaturesMutation = q.mutation(updateUnitFeatures, [
  unitFeaturesQuery
])
