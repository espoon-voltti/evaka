// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './client'
import { Failure, Result, Success } from '@evaka/lib-common/api'
import { Unit } from '../types/invoicing'
import { CareArea } from '../types/unit'
import { PlacementType } from '../types/child'
import { JsonOf } from '@evaka/lib-common/json'
import LocalDate from '@evaka/lib-common/local-date'
import { PublicUnit } from '@evaka/lib-common/api-types/units/PublicUnit'

export async function getUnits(
  areas: string[],
  type: 'ALL' | 'CLUB' | 'DAYCARE' | 'PRESCHOOL'
): Promise<Result<Unit[]>> {
  return client
    .get<JsonOf<Unit[]>>(
      `/filters/units?type=${type}${
        areas.length > 0 ? `&area=${areas.join(',')}` : ''
      }`
    )
    .then((res) =>
      Success.of(
        res.data.sort((unitA, unitB) =>
          unitA.name.localeCompare(unitB.name, 'fi', {
            ignorePunctuation: true
          })
        )
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getAreas(): Promise<Result<CareArea[]>> {
  return client
    .get<JsonOf<CareArea[]>>('/areas')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export function getApplicationUnits(
  type: PlacementType,
  date: LocalDate
): Promise<Result<PublicUnit[]>> {
  return client
    .get<JsonOf<PublicUnit[]>>('units', {
      params: { type: asUnitType(type), date: date.formatIso() }
    })
    .then((res) => res.data)
    .then((units) => Success.of(units))
    .catch((e) => Failure.fromError(e))
}

const asUnitType = (
  placementType: PlacementType
): 'CLUB' | 'DAYCARE' | 'PRESCHOOL' | 'PREPARATORY' => {
  switch (placementType) {
    case 'CLUB':
      return 'CLUB'
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
    case 'TEMPORARY_DAYCARE':
    case 'TEMPORARY_DAYCARE_PART_DAY':
      return 'DAYCARE'
    case 'PRESCHOOL':
    case 'PRESCHOOL_DAYCARE':
      return 'PRESCHOOL'
    case 'PREPARATORY':
    case 'PREPARATORY_DAYCARE':
      return 'PREPARATORY'
  }
}
