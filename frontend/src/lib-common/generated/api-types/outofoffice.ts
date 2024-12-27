// SPDX-FileCopyrightText: 2017-2024 City of Espoo
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


export function deserializeJsonOutOfOfficePeriod(json: JsonOf<OutOfOfficePeriod>): OutOfOfficePeriod {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}
