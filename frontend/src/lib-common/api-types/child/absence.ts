// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AbsenceCareType,
  AbsenceWithModifierInfo,
  Child
} from '../../generated/api-types/daycare'
import { JsonOf } from '../../json'
import LocalDate from '../../local-date'

export interface AbsenceChild {
  child: Child
  placements: Record<JsonOf<LocalDate>, AbsenceCareType[]>
  absences: Record<JsonOf<LocalDate>, AbsenceWithModifierInfo[]>
  backupCares: Record<JsonOf<LocalDate>, boolean>
}
