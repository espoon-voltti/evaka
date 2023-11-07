// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { deserializePublicUnit } from 'lib-common/api-types/units/PublicUnit'
import {
  DaycareCareArea,
  PublicUnit,
  UnitStub,
  UnitTypeFilter
} from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

import { client } from './client'

export interface Unit {
  id: string
  name: string
}

export async function getUnitsRaw(
  areas: string[],
  type: UnitTypeFilter,
  from?: LocalDate
): Promise<UnitStub[]> {
  return client
    .get<JsonOf<UnitStub[]>>('/filters/units', {
      params: {
        type,
        ...(areas.length > 0 ? { area: areas.join(',') } : {}),
        from: from?.toJSON()
      }
    })
    .then((res) =>
      res.data.sort((unitA, unitB) =>
        unitA.name.localeCompare(unitB.name, 'fi', {
          ignorePunctuation: true
        })
      )
    )
}

export async function getUnits(
  areas: string[],
  type: UnitTypeFilter,
  from?: LocalDate
): Promise<Result<UnitStub[]>> {
  return getUnitsRaw(areas, type, from)
    .then((res) => Success.of(res))
    .catch((e) => Failure.fromError(e))
}

export async function getAreas(): Promise<DaycareCareArea[]> {
  return client.get<JsonOf<DaycareCareArea[]>>('/areas').then((res) => res.data)
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
    .then((units) => Success.of(units.map(deserializePublicUnit)))
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
    case 'DAYCARE_FIVE_YEAR_OLDS':
    case 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS':
    case 'TEMPORARY_DAYCARE':
    case 'TEMPORARY_DAYCARE_PART_DAY':
    case 'SCHOOL_SHIFT_CARE':
      return 'DAYCARE'
    case 'PRESCHOOL':
    case 'PRESCHOOL_DAYCARE':
    case 'PRESCHOOL_CLUB':
      return 'PRESCHOOL'
    case 'PREPARATORY':
    case 'PREPARATORY_DAYCARE':
      return 'PREPARATORY'
  }
}
