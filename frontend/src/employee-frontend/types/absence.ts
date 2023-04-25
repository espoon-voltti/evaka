// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  AbsenceCategory,
  AbsenceChild,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultAbsenceCategories: AbsenceCategory[] = []

export const absenceCategories: AbsenceCategory[] = ['NONBILLABLE', 'BILLABLE']

export interface Cell {
  id: UUID
  parts: CellPart[]
}

type AbsenceTypeWithBackupCare = AbsenceType | 'TEMPORARY_RELOCATION'

export interface CellPart {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType?: AbsenceTypeWithBackupCare
  category: AbsenceCategory
  position: 'left' | 'right'
}

export const deserializeChild = (json: JsonOf<AbsenceChild>): AbsenceChild => ({
  ...json,
  actualServiceNeeds: json.actualServiceNeeds.map((cdi) => ({
    ...cdi,
    validDuring: FiniteDateRange.parseJson(cdi.validDuring)
  })),
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  },
  absences: Object.fromEntries(
    Object.entries(json.absences).map(([date, absences]) => [
      date,
      absences.map((absence) => ({
        ...absence,
        date: LocalDate.parseIso(absence.date),
        modifiedAt: HelsinkiDateTime.parseIso(absence.modifiedAt)
      }))
    ])
  ),
  missingHolidayReservations: json.missingHolidayReservations.map((d) =>
    LocalDate.parseIso(d)
  )
})
