// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './client'
import { Failure, Result, Success } from '.'
import { Unit } from '../types/invoicing'
import { CareArea } from '~types/unit'
import { PreferredUnit } from '~types/application'
import { PlacementType } from '~types/placementdraft'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

export async function getUnits(areas: string[]): Promise<Result<Unit[]>> {
  return client
    .get<JsonOf<Unit[]>>(
      `/filters/units?type=daycare${
        areas.length > 0 ? `&area=${areas.join(',')}` : ''
      }`
    )
    .then((res) => Success(res.data))
    .catch((e) => Failure(e))
}

export async function getAreas(): Promise<Result<CareArea[]>> {
  return client
    .get<JsonOf<CareArea[]>>('/areas')
    .then((res) => Success(res.data))
    .catch((e) => Failure(e))
}

type DetailedUnit = {
  id: string
  name: string
}

export function getApplicationUnits(
  type: PlacementType,
  date: LocalDate
): Promise<Result<PreferredUnit[]>> {
  return client
    .get<JsonOf<DetailedUnit[]>>('public/units', {
      params: { type: asUnitType(type), date: date.formatIso() }
    })
    .then((res) => res.data)
    .then((units) => Success(units))
    .catch((e) => Failure(e))
}

const asUnitType = (
  placementType: PlacementType
): 'CLUB' | 'DAYCARE' | 'PRESCHOOL' | 'PREPARATORY' => {
  switch (placementType) {
    case 'CLUB':
      return 'CLUB'
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
      return 'DAYCARE'
    case 'PRESCHOOL':
    case 'PRESCHOOL_DAYCARE':
      return 'PRESCHOOL'
    case 'PREPARATORY':
    case 'PREPARATORY_DAYCARE':
      return 'PREPARATORY'
  }
}
