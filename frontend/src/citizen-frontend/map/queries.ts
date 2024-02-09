// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { Coordinate } from 'lib-common/generated/api-types/shared'
import { query } from 'lib-common/query'

import { getAllApplicableUnits } from '../generated/api-clients/daycare'
import { createQueryKeys } from '../query'

import { MapAddress } from './MapView'
import {
  fetchDistance,
  autocompleteAddress,
  fetchUnitsWithDistances
} from './api'

const queryKeys = createQueryKeys('map', {
  units: (type: ApplicationType) => ['units', type],
  unitsWithDistances: (
    selectedAddress: MapAddress | null,
    filteredUnits: PublicUnit[]
  ) => [
    'unitsWithDistances',
    selectedAddress?.coordinates,
    filteredUnits.map((u) => u.id)
  ],
  addressOptions: (input: string) => ['addressOptions', input],
  distance: (start: Coordinate | null, end: Coordinate | null) => [
    'distance',
    start,
    end
  ]
})

export const unitsQuery = query({
  api: getAllApplicableUnits,
  queryKey: ({ applicationType }) => queryKeys.units(applicationType)
})

export const unitsWithDistancesQuery = query({
  api: async (
    selectedAddress: MapAddress | null,
    filteredUnits: PublicUnit[]
  ) =>
    selectedAddress && filteredUnits.length > 0
      ? fetchUnitsWithDistances(selectedAddress, filteredUnits)
      : [],
  queryKey: queryKeys.unitsWithDistances
})

export const addressOptionsQuery = query({
  api: async (input: string) =>
    input.length > 0 ? autocompleteAddress(input) : [],
  queryKey: queryKeys.addressOptions
})

export const distanceQuery = query({
  api: async (start: Coordinate | null, end: Coordinate | null) =>
    start && end ? fetchDistance(start, end) : null,
  queryKey: queryKeys.distance
})
