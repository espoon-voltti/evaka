// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

/**
 * Calculate age based on date of birth.
 */
export const getAge = (dateOfBirth: LocalDate): number => {
  return LocalDate.today().differenceInYears(dateOfBirth)
}
