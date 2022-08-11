// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import DateRange from 'lib-common/date-range'
import {
  AbsenceCategory,
  AbsenceChild,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

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
  position: string
}

const deserializeDailyServiceTimes = (
  json: JsonOf<DailyServiceTimes[]> | null
): DailyServiceTimes[] | null =>
  json === null
    ? null
    : json.map((dst) => ({
        ...dst,
        validityPeriod: DateRange.parseJson(dst.validityPeriod)
      }))

export const deserializeChild = (json: JsonOf<AbsenceChild>): AbsenceChild => ({
  ...json,
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
  dailyServiceTimes: deserializeDailyServiceTimes(json.dailyServiceTimes)
})
