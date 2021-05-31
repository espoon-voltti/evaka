// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export interface Absence {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType: AbsenceType
  careType: AbsenceCareType
  modifiedAt?: Date
  modifiedBy?: string
}

export type AbsenceType =
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'UNKNOWN_ABSENCE'
  | 'PLANNED_ABSENCE'
  | 'TEMPORARY_RELOCATION'
  | 'TEMPORARY_VISITOR'
  | 'PARENTLEAVE'
  | 'FORCE_MAJEURE'

export type AbsenceCareType =
  | 'SCHOOL_SHIFT_CARE'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'DAYCARE_5YO_FREE'
  | 'DAYCARE'
  | 'CLUB'

export const deserializeAbsence = (absence: JsonOf<Absence>): Absence => ({
  ...absence,
  date: LocalDate.parseIso(absence.date),
  modifiedAt: absence.modifiedAt ? new Date(absence.modifiedAt) : undefined
})
