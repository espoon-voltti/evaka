// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PlacementType } from '~types/placementdraft'

const partDayPlacementTypes: readonly PlacementType[] = [
  'DAYCARE_PART_TIME',
  'TEMPORARY_DAYCARE_PART_DAY',
  'PRESCHOOL',
  'PREPARATORY'
] as const

export function isPartDayPlacement(type: PlacementType): boolean {
  return partDayPlacementTypes.includes(type)
}
