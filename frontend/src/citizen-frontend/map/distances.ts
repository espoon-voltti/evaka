// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PublicUnit } from 'lib-common/generated/api-types/daycare'
import type { Coordinate } from 'lib-common/generated/api-types/shared'

export type UnitWithStraightDistance = PublicUnit & {
  straightDistance: number | null
}

export type UnitWithDistance = UnitWithStraightDistance & {
  drivingDistance: number | null
}

// source https://www.movable-type.co.uk/scripts/latlong.html
export const calcStraightDistance = (
  { lon: lon1, lat: lat1 }: Coordinate,
  { lon: lon2, lat: lat2 }: Coordinate
): number => {
  const R = 6371e3
  const phi1 = (lat1 * Math.PI) / 180
  const phi2 = (lat2 * Math.PI) / 180
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) *
      Math.cos(phi2) *
      Math.sin(deltaLambda / 2) *
      Math.sin(deltaLambda / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

  return R * c
}

export const formatDistance = (distance: number | null): string => {
  if (typeof distance !== 'number') return '?'

  return Math.round(distance) >= 1000
    ? `${Math.round(distance / 100) / 10} km`
    : `${Math.round(distance)} m`
}
