// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type LocalDate from 'lib-common/local-date'

export type PreschoolTermError =
  | 'preschoolTermNotOpen'
  | 'preschoolExtendedTermNotOpen'

export function getPreschoolTermError(
  type: PlacementType,
  startDate: LocalDate,
  endDate: LocalDate,
  preschoolTerms: PreschoolTerm[]
): PreschoolTermError | null {
  if (startDate.isAfter(endDate)) return null
  const range = new FiniteDateRange(startDate, endDate)
  switch (type) {
    case 'PRESCHOOL':
    case 'PREPARATORY':
      return preschoolTerms.some(
        (term) =>
          term.finnishPreschool.contains(range) ||
          term.swedishPreschool.contains(range)
      )
        ? null
        : 'preschoolTermNotOpen'
    case 'PRESCHOOL_DAYCARE':
    case 'PRESCHOOL_DAYCARE_ONLY':
    case 'PREPARATORY_DAYCARE':
      return preschoolTerms.some((term) => term.extendedTerm.contains(range))
        ? null
        : 'preschoolExtendedTermNotOpen'
    case 'PREPARATORY_DAYCARE_ONLY':
    case 'PRESCHOOL_CLUB':
    case 'CLUB':
    case 'DAYCARE':
    case 'DAYCARE_PART_TIME':
    case 'DAYCARE_FIVE_YEAR_OLDS':
    case 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS':
    case 'TEMPORARY_DAYCARE':
    case 'TEMPORARY_DAYCARE_PART_DAY':
    case 'SCHOOL_SHIFT_CARE':
      return null
  }
}
