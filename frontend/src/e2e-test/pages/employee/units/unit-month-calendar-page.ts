// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../../utils'
import type { Page } from '../../../utils/page'
import { Checkbox, Element, Modal, Radio, TextInput } from '../../../utils/page'

import { UnitCalendarPageBase } from './unit-calendar-page-base'

export class UnitMonthCalendarPage extends UnitCalendarPageBase {
  previousWeekButton: Element
  nextWeekButton: Element
  #addAbsencesButton: Element

  constructor(page: Page) {
    super(page)
    this.previousWeekButton = page.findByDataQa('previous-week')
    this.nextWeekButton = page.findByDataQa('next-week')
    this.#addAbsencesButton = page.findByDataQa('add-absences-button')
  }

  childRow = (childId: UUID) =>
    this.page.findByDataQa(`absence-child-row-${childId}`)

  absenceCell = (childId: UUID, date: LocalDate) =>
    new AbsenceCell(
      this.childRow(childId).findByDataQa(`absence-cell-${date.formatIso()}`)
    )

  #staffAttendanceCells = this.page.findAll('[data-qa="staff-attendance-cell"]')

  async addAbsenceToChild(
    childId: UUID,
    date: LocalDate,
    type: AbsenceType | 'NO_ABSENCE',
    categories: AbsenceCategory[] = []
  ) {
    await this.absenceCell(childId, date).select()
    await this.#addAbsencesButton.click()

    const modal = new AbsenceModal(this.page.findByDataQa('absence-modal'))
    await modal.selectAbsenceType(type)
    for (const category of categories) {
      await modal.selectAbsenceCategory(category)
    }
    await modal.submit()
  }

  async childHasAbsence(
    childId: UUID,
    date: LocalDate,
    type: AbsenceType,
    category: AbsenceCategory
  ) {
    await this.absenceCell(childId, date).assertAbsenceType(type, category)
  }

  async assertTooltipContains(
    childId: UUID,
    date: LocalDate,
    expectedTexts: string[]
  ) {
    const tooltipText = await this.absenceCell(
      childId,
      date
    ).hoverAndGetTooltip()
    return expectedTexts.every((text) => tooltipText.includes(text))
  }

  async childHasNoAbsence(
    childId: UUID,
    date: LocalDate,
    category: AbsenceCategory
  ) {
    await this.absenceCell(childId, date).assertNoAbsence(category)
  }

  async assertChildTotalHours(
    childId: UUID,
    expected: {
      reservedHours: number
      reservedHoursWarning: boolean
      usedHours: number
      usedHoursWarning: boolean
    }
  ) {
    const childRow = this.childRow(childId)
    await childRow
      .findByDataQa('reserved-hours')
      .assertTextEquals(`${expected.reservedHours} h`)
    if (expected.reservedHoursWarning) {
      await childRow.findByDataQa('reserved-hours-warning').waitUntilVisible()
    } else {
      await childRow.findByDataQa('reserved-hours-warning').waitUntilHidden()
    }
    await childRow
      .findByDataQa('used-hours')
      .assertTextEquals(`${expected.usedHours} h`)
    if (expected.usedHoursWarning) {
      await childRow.findByDataQa('used-hours-warning').waitUntilVisible()
    } else {
      await childRow.findByDataQa('used-hours-warning').waitUntilHidden()
    }
  }

  async fillStaffAttendance(n: number, staffCount: string) {
    const cell = this.#staffAttendanceCells.nth(n)
    await new TextInput(cell.find('input')).fill(staffCount)

    // Wait until saved
    await waitUntilEqual(() => cell.getAttribute('data-state'), 'clean')
  }

  async assertStaffAttendance(n: number, staffCount: string) {
    const input = new TextInput(this.#staffAttendanceCells.nth(n).find('input'))
    await input.assertValueEquals(staffCount)
  }
}

export class AbsenceCell extends Element {
  constructor(private cell: Element) {
    super(cell)
  }

  missingHolidayReservation = this.findByDataQa('missing-holiday-reservation')

  async select() {
    await this.locator.click()
  }

  async assertAbsenceType(
    type: AbsenceType | 'empty',
    category: AbsenceCategory
  ) {
    const position = category === 'BILLABLE' ? 'right' : 'left'
    const positionAttr = `[data-position="${position}"]`
    const absenceTypeAttr =
      type === 'empty'
        ? ':not([data-absence-type])'
        : `[data-absence-type="${type}"]`
    await this.cell.find(positionAttr + absenceTypeAttr).waitUntilVisible()
  }

  async assertNoAbsence(category: AbsenceCategory) {
    await this.assertAbsenceType('empty', category)
  }

  async hoverAndGetTooltip(): Promise<string> {
    await this.cell.hover()
    return (await this.cell.findByDataQa('absence-cell-tooltip').text) || ''
  }
}

export class AbsenceModal extends Modal {
  #absenceTypeRadio = (type: AbsenceType | 'NO_ABSENCE') =>
    new Radio(this.find(`[data-qa="absence-type-${type}"]`))
  #categoryCheckbox = (category: AbsenceCategory) =>
    new Checkbox(this.findByDataQa(`absences-select-${category}`))

  async selectAbsenceType(type: AbsenceType | 'NO_ABSENCE') {
    await this.#absenceTypeRadio(type).check()
  }

  async selectAbsenceCategory(category: AbsenceCategory) {
    await this.#categoryCheckbox(category).check()
  }
}
