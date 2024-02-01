// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { Absence } from 'lib-common/generated/api-types/absence'
import { groupDatesToRanges } from 'lib-common/utils/local-date'

export function groupAbsencesByDateRange(
  absences: Absence[]
): FiniteDateRange[] {
  return groupDatesToRanges(absences.map((absence) => absence.date))
}
