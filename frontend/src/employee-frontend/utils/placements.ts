// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PlacementType } from 'lib-common/generated/api-types/placement'

const partDayPlacementTypes: readonly PlacementType[] = [
  'DAYCARE_PART_TIME',
  'DAYCARE_PART_TIME_FIVE_YEAR_OLDS',
  'TEMPORARY_DAYCARE_PART_DAY',
  'PRESCHOOL',
  'PREPARATORY'
] as const

export function isPartDayPlacement(type: PlacementType): boolean {
  return partDayPlacementTypes.includes(type)
}
