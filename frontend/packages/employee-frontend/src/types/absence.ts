// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DayOfWeek, UUID } from '~types'
import LocalDate from '@evaka/lib-common/src/local-date'
import { JsonOf } from '@evaka/lib-common/src/json'
import { PlacementType } from './placementdraft'
import { Translations } from '~state/i18n'

export type TableMode = 'MONTH'

export type AbsenceType =
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'UNKNOWN_ABSENCE'
  | 'PLANNED_ABSENCE'
  | 'TEMPORARY_RELOCATION'
  | 'TEMPORARY_VISITOR'
  | 'PARENTLEAVE'
  | 'FORCE_MAJEURE'
  | 'PRESENCE'

export const AbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE',
  'PRESENCE'
]

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultCareTypeCategory: CareTypeCategory[] = []

export type CareType =
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'DAYCARE_5YO_FREE'
  | 'DAYCARE'
  | 'CLUB'

export type CareTypeCategory = 'BILLABLE' | 'NONBILLABLE'

export const CareTypeCategories: CareTypeCategory[] = [
  'NONBILLABLE',
  'BILLABLE'
]

export const billableCareTypes: CareType[] = ['PRESCHOOL_DAYCARE', 'DAYCARE']

export function formatCareType(
  careType: CareType,
  placementType: PlacementType,
  i18n: Translations
) {
  const isPreparatory =
    placementType === 'PREPARATORY' || placementType === 'PREPARATORY_DAYCARE'

  if (isPreparatory && careType === 'PRESCHOOL')
    return i18n.common.types.PREPARATORY_EDUCATION

  return i18n.absences.careTypes[careType]
}

export interface Cell {
  id: UUID
  parts: CellPart[]
}

export interface CellPart {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType?: AbsenceType
  careType: CareType
  position: string
}

export interface AbsencePayload {
  absenceType: AbsenceType
  childId: UUID
  date: LocalDate
  careType: CareType
}

// Response

export interface Absence {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType: AbsenceType
  careType: CareType
  modifiedAt?: Date
  modifiedBy?: string
}

export interface Child {
  id: UUID
  firstName: string
  lastName: string
  dob: LocalDate
  placements: { [key: string]: CareType[] }
  absences: { [key: string]: Absence[] }
  backupCares: { [key: string]: AbsenceBackupCare | null }
}

export const deserializeChild = (child: JsonOf<Child>): Child => ({
  ...child,
  dob: LocalDate.parseIso(child.dob),
  absences: Object.entries(child.absences).reduce(
    (absenceMap, [key, absences]) => ({
      ...absenceMap,
      [key]: absences.map(deserializeAbsence)
    }),
    {}
  ),
  backupCares: Object.entries(child.backupCares).reduce(
    (backupCareMap, [key, backup]) => ({
      ...backupCareMap,
      [key]: backup
        ? {
            ...backup,
            date: LocalDate.parseIso(backup.date)
          }
        : null
    }),
    {}
  )
})

export const deserializeAbsence = (absence: JsonOf<Absence>): Absence => ({
  ...absence,
  date: LocalDate.parseIso(absence.date),
  modifiedAt: absence.modifiedAt ? new Date(absence.modifiedAt) : undefined
})

export interface Group {
  groupId: UUID
  daycareName: string
  groupName: string
  children: Child[]
  operationDays: DayOfWeek[]
}

export interface AbsenceBackupCare {
  childId: UUID
  date: LocalDate
}

export interface StaffAttendance {
  groupId: UUID
  date: LocalDate
  count: number | undefined
}

export interface StaffAttendanceGroup {
  groupId: UUID
  groupName: string
  startDate: LocalDate
  endDate: LocalDate | null
  attendances: { [key: string]: StaffAttendance }
}
