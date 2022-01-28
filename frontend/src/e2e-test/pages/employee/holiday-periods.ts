// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DatePicker, Page, TextInput } from '../../utils/page'

export class HolidayPeriodsPage {
  constructor(private readonly page: Page) {}

  get visiblePeriods(): Promise<string[]> {
    return this.page.findAllByDataQa('holiday-period').allInnerTexts()
  }

  async assertRowContainsText(nth: number, texts: string[]) {
    const row = this.page.findAll(`[data-qa="holiday-period-row"]`).nth(nth)
    for (const text of texts) {
      await row.findText(text).waitUntilVisible()
    }
  }

  async clickAddButton() {
    return this.page.findByDataQa('add-button').click()
  }

  #inputs = {
    start: new DatePicker(this.page.findByDataQa('input-start')),
    end: new DatePicker(this.page.findByDataQa('input-end')),
    reservationDeadline: new DatePicker(
      this.page.findByDataQa('input-reservation-deadline')
    ),
    showReservationBannerFrom: new DatePicker(
      this.page.findByDataQa('input-show-banner-from')
    ),
    description: new TextInput(this.page.findByDataQa('input-description-fi')),
    descriptionSv: new TextInput(
      this.page.findByDataQa('input-description-sv')
    ),
    descriptionEn: new TextInput(
      this.page.findByDataQa('input-description-en')
    ),
    descriptionLink: new TextInput(
      this.page.findByDataQa('input-description-link-fi')
    ),
    descriptionLinkSv: new TextInput(
      this.page.findByDataQa('input-description-link-sv')
    ),
    descriptionLinkEn: new TextInput(
      this.page.findByDataQa('input-description-link-en')
    )
  }

  async fillForm(params: {
    description?: string
    descriptionSv?: string
    descriptionEn?: string
    descriptionLink?: string
    descriptionLinkSv?: string
    descriptionLinkEn?: string
    start?: string
    end?: string
    reservationDeadline?: string
    showReservationBannerFrom?: string
  }) {
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined) {
        await this.#inputs[key as keyof typeof params].fill(val)
      }
    }
  }

  async submit() {
    return this.page.findByDataQa('save-holiday-period-btn').click()
  }

  async editHolidayPeriod(nth: number) {
    return this.page.findAllByDataQa('btn-edit').nth(nth).click()
  }

  async deleteHolidayPeriod(nth: number) {
    await this.page.findAllByDataQa('btn-delete').nth(nth).click()
    return this.page.findByDataQa('modal-okBtn').click()
  }
}
