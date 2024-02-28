// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'

export type AbsenceUpdate =
  | {
      type: 'absence'
      absenceType: AbsenceType
      absenceCategories: AbsenceCategory[]
    }
  | { type: 'noAbsence'; absenceCategories: AbsenceCategory[] }
  | { type: 'missingHolidayReservation' }
