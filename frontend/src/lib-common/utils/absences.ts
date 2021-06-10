// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Absence } from 'lib-common/api-types/child/Absences'
import FiniteDateRange from 'lib-common/finite-date-range'
import { groupDatesToRanges } from 'lib-common/utils/local-date'

export function groupAbsencesByDateRange(
  absences: Absence[]
): FiniteDateRange[] {
  return groupDatesToRanges(absences.map((absence) => absence.date))
}
