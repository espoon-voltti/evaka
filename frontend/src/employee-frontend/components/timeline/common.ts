// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'

export interface WithRange {
  range: DateRange
}

export function hasRange(x: unknown): x is WithRange {
  return (
    typeof x === 'object' &&
    x !== null &&
    'range' in x &&
    x.range instanceof DateRange
  )
}
