// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, DatePicker, Select, TextInput, Element } from '../../utils/page'

export class CitizenNewServiceApplicationPage {
  startDate: DatePicker
  serviceNeed: Select
  additionalInfo: TextInput
  createButton: Element

  constructor(private readonly page: Page) {
    this.startDate = new DatePicker(this.page.findByDataQa('start-date'))
    this.serviceNeed = new Select(this.page.findByDataQa('service-need-option'))
    this.additionalInfo = new TextInput(
      this.page.findByDataQa('additional-info')
    )
    this.createButton = this.page.findByDataQa('create-button')
  }
}
