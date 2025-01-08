// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getAllApplicableUnits } from '../generated/api-clients/daycare'

import {
  fetchDistance,
  autocompleteAddress,
  fetchUnitsWithDistances
} from './api'

const q = new Queries()

export const unitsQuery = q.query(getAllApplicableUnits)

export const unitsWithDistancesQuery = q.query(fetchUnitsWithDistances)

export const addressOptionsQuery = q.query(autocompleteAddress)

export const distanceQuery = q.query(fetchDistance)
