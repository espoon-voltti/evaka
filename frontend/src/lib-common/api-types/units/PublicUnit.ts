// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import type { PublicUnit } from 'lib-common/generated/api-types/daycare'
import type { JsonOf } from 'lib-common/json'

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
