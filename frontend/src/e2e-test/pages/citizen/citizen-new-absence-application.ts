// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Element, Page } from '../../utils/page'
import { Checkbox, DatePicker, TextInput } from '../../utils/page'

export class CitizenNewAbsenceApplicationPage {
  startDate: DatePicker
  endDate: DatePicker
  description: TextInput
  confirmation: Checkbox
  dateRangeWarning: Element
  createButton: Element

  constructor(page: Page) {
    this.startDate = new DatePicker(page.findByDataQa('start-date'))
    this.endDate = new DatePicker(page.findByDataQa('end-date'))
    this.description = new TextInput(page.findByDataQa('description'))
    this.confirmation = new Checkbox(page.findByDataQa('confirmation'))
    this.dateRangeWarning = page.find(
      `span:has-text("Lapsella ei ole esiopetusta valitulla aikavälillä")`
    )
    this.createButton = page.findByDataQa('create-button')
  }
}
