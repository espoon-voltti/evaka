// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import { JsonOf } from '../../json'
import { OutOfOfficeId } from './shared'

/**
* Generated from fi.espoo.evaka.outofoffice.OutOfOfficePeriod
*/
export interface OutOfOfficePeriod {
  id: OutOfOfficeId
  period: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.outofoffice.OutOfOfficePeriodUpsert
*/
export interface OutOfOfficePeriodUpsert {
  id: OutOfOfficeId | null
  period: FiniteDateRange
}


export function deserializeJsonOutOfOfficePeriod(json: JsonOf<OutOfOfficePeriod>): OutOfOfficePeriod {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonOutOfOfficePeriodUpsert(json: JsonOf<OutOfOfficePeriodUpsert>): OutOfOfficePeriodUpsert {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}
