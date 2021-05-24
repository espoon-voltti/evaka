// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { uniqueId } from 'lodash'

import { Absence } from 'lib-common/api-types/child/Absences'
import LocalDate from 'lib-common/local-date'
import { groupAbsencesByDateRange } from './helpers'

describe('absences date range', () => {
  describe('grouping works with', () => {
    it('a single date', () => {
      const absence: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(1),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absences = [absence]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(-1)
    })

    it('two dates', () => {
      const absence: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(1),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absence2: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(2),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absences = [absence, absence2]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(-2)
    })

    it('two ranges', () => {
      const absence: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(1),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absence2: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(2),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }

      const absence3: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(7),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absence4: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(8),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absences = [absence, absence2, absence3, absence4]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(-2)
      expect(groupAbsencesByDateRange(absences)[1].durationInDays()).toBe(-2)
    })

    it('a range and a single date', () => {
      const absence: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(1),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absence2: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(2),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }

      const absence3: Absence = {
        id: uniqueId(),
        childId: uniqueId(),
        date: LocalDate.today().addDays(7),
        absenceType: 'SICKLEAVE',
        careType: 'PRESCHOOL'
      }
      const absences = [absence, absence2, absence3]
      expect(groupAbsencesByDateRange(absences)[0].durationInDays()).toBe(-2)
      expect(groupAbsencesByDateRange(absences)[1].durationInDays()).toBe(-1)
    })

    it('with no dates', () => {
      const absences: Absence[] = []
      expect(groupAbsencesByDateRange(absences).length).toBe(0)
    })
  })
})
