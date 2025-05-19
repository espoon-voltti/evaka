// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Absence } from 'lib-common/generated/api-types/absence'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import HelsinkiDateTime from '../helsinki-date-time'

import { groupAbsencesByDateRange } from './absences'

const base = {
  childId: randomId<ChildId>(),
  modifiedByStaff: true,
  modifiedAt: HelsinkiDateTime.now()
}

describe('absences date range', () => {
  describe('grouping works with', () => {
    it('a single date', () => {
      const absence: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(1),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absences = [absence]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(1)
    })

    it('two dates', () => {
      const absence: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(1),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absence2: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(2),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absences = [absence, absence2]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(2)
    })

    it('two ranges', () => {
      const absence: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(1),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absence2: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(2),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absence3: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(7),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absence4: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(8),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absences = [absence3, absence2, absence, absence4]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(2)
      expect(groupAbsencesByDateRange(absences)[1].durationInDays()).toBe(2)
    })

    it('a range and a single date', () => {
      const absence: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(1),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absence2: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(2),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }

      const absence3: Absence = {
        ...base,
        date: LocalDate.todayInSystemTz().addDays(7),
        absenceType: 'SICKLEAVE',
        category: 'NONBILLABLE',
        belongsToQuestionnaire: false
      }
      const absences = [absence, absence2, absence3]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(2)
      expect(groupAbsencesByDateRange(absences)[1].durationInDays()).toBe(1)
    })

    it('with no dates', () => {
      const absences: Absence[] = []
      expect(groupAbsencesByDateRange(absences).length).toBe(0)
    })
  })
})
