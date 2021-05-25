// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

import LocalDate from 'lib-common/local-date'
import { AbsenceType } from 'lib-common/api-types/child/Absences'

import { RawElement } from 'e2e-playwright/utils/element'

export default class MobileAbsencesPage {
  constructor(private readonly page: Page) {}

  #markAbsentBtn = new RawElement(this.page, '[data-qa="mark-absent-btn"]')
  #deleteAbsencePeriodBtn = new RawElement(
    this.page,
    '[data-qa="delete-absence-period"]'
  )
  #confirmDeleteBtn = new RawElement(this.page, '[data-qa="modal-okBtn"]')

  async markAbsent() {
    return this.#markAbsentBtn.click()
  }

  async getAbsencesCount() {
    return this.page.$$eval('[data-qa="absence-row"]', (rows) => rows.length)
  }
  absenceChip(absenceType: AbsenceType) {
    return new RawElement(this.page, `[data-qa="mark-absent-${absenceType}"]`)
  }

  async markNewAbsencePeriod(
    from: LocalDate,
    to: LocalDate,
    absenceType: AbsenceType
  ) {
    await this.page.fill('[data-qa="start-date-input"]', from.formatIso())
    await this.page.fill('[data-qa="end-date-input"]', to.formatIso())
    await this.absenceChip(absenceType).click()
    await this.markAbsent()
  }

  async deleteFirstAbsencePeriod() {
    await this.#deleteAbsencePeriodBtn.click()
    await this.#confirmDeleteBtn.click()
  }
}
