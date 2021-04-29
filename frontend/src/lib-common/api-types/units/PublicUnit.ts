// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../../types'
import { CareType, ProviderType, UnitLanguage } from './enums'
import { Coordinate } from './Coordinate'
import DateRange from 'lib-common/date-range'
import { JsonOf } from 'lib-common/json'

export function deserializePublicUnit(unit: JsonOf<PublicUnit>): PublicUnit {
  return {
    ...unit,
    daycareApplyPeriod: unit.daycareApplyPeriod
      ? DateRange.parseJson(unit.daycareApplyPeriod)
      : null,
    preschoolApplyPeriod: unit.preschoolApplyPeriod
      ? DateRange.parseJson(unit.preschoolApplyPeriod)
      : null,
    clubApplyPeriod: unit.clubApplyPeriod
      ? DateRange.parseJson(unit.clubApplyPeriod)
      : null
  }
}

export type PublicUnit = {
  id: UUID
  name: string
  type: CareType[]
  providerType: ProviderType
  language: UnitLanguage
  streetAddress: string
  postalCode: string
  postOffice: string
  phone: string | null
  email: string | null
  url: string | null
  location: Coordinate | null
  ghostUnit: boolean | undefined
  roundTheClock: boolean
  daycareApplyPeriod: DateRange | null
  preschoolApplyPeriod: DateRange | null
  clubApplyPeriod: DateRange | null
}
