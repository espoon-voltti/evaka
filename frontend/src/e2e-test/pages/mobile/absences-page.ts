// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AbsenceType } from 'lib-common/generated/api-types/absence'
import LocalDate from 'lib-common/local-date'

import { Page, TextInput } from '../../utils/page'

export default class MobileAbsencesPage {
  constructor(private readonly page: Page) {}

  #markAbsentBtn = this.page.find('[data-qa="mark-absent-btn"]')
  #firstDeleteAbsencePeriodBtn = this.page
    .findAll('[data-qa="delete-absence-period"]')
    .first()
  #confirmDeleteBtn = this.page.find('[data-qa="modal-okBtn"]')

  async markAbsent() {
    return this.#markAbsentBtn.click()
  }

  async getAbsencesCount() {
    return this.page.findAll('[data-qa="absence-row"]').count()
  }

  absenceChip(absenceType: AbsenceType) {
    return this.page.find(`[data-qa="mark-absent-${absenceType}"]`)
  }

  async markNewAbsencePeriod(
    from: LocalDate,
    to: LocalDate,
    absenceType: AbsenceType
  ) {
    await new TextInput(this.page.find('[data-qa="start-date-input"]')).fill(
      from.formatIso()
    )
    await new TextInput(this.page.find('[data-qa="end-date-input"]')).fill(
      to.formatIso()
    )
    await this.absenceChip(absenceType).click()
    await this.markAbsent()
  }

  async deleteFirstAbsencePeriod() {
    await this.#firstDeleteAbsencePeriodBtn.click()
    await this.#confirmDeleteBtn.click()
  }
}
