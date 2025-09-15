// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UUID } from 'lib-common/types'

import type { Page, Element } from '../../utils/page'
import { Combobox, DatePicker } from '../../utils/page'

export class PlacementDraftPage {
  #restrictedDetailsWarning: Element
  startDate: DatePicker
  endDate: DatePicker
  preschoolDaycareStartDate: DatePicker | null = null
  preschoolDaycareEndDate: DatePicker | null = null
  #addOtherUnitCombobox: Combobox
  constructor(private page: Page) {
    this.#restrictedDetailsWarning = page.findByDataQa(
      'restricted-details-warning'
    )
    this.startDate = new DatePicker(page.findByDataQa('start-date'))
    this.endDate = new DatePicker(page.findByDataQa('end-date'))
    this.preschoolDaycareStartDate = new DatePicker(
      page.findByDataQa('preschool-daycare-start-date')
    )
    this.preschoolDaycareEndDate = new DatePicker(
      page.findByDataQa('preschool-daycare-end-date')
    )

    this.#addOtherUnitCombobox = new Combobox(
      page.findByDataQa('add-other-unit')
    )
  }

  #unitCard = (unitId: UUID) =>
    this.page
      .find('[data-qa="placement-list"]')
      .find(`[data-qa="placement-item-${unitId}"]`)

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="placement-draft-page"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa^="placement-item-"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertRestrictedDetailsWarning() {
    await this.#restrictedDetailsWarning.waitUntilVisible()
  }

  async assertOccupancies(unitId: UUID, occupancies: OccupancyValues) {
    const current = this.#unitCard(unitId).find(
      '[data-qa="current-occupancies"]'
    )
    const speculated = this.#unitCard(unitId).find(
      '[data-qa="speculated-occupancies"]'
    )
    await current
      .find('[data-qa="3months"]')
      .assertTextEquals(occupancies.max3Months)
    await current
      .find('[data-qa="6months"]')
      .assertTextEquals(occupancies.max6Months)
    await speculated
      .find('[data-qa="3months"]')
      .assertTextEquals(occupancies.max3MonthsSpeculated)
    await speculated
      .find('[data-qa="6months"]')
      .assertTextEquals(occupancies.max6MonthsSpeculated)
  }

  async addOtherUnit(unitName: string) {
    await this.#addOtherUnitCombobox.fillAndSelectFirst(unitName)
  }

  async placeToUnit(unitId: UUID) {
    await this.#unitCard(unitId)
      .find('[data-qa="select-placement-unit"]')
      .click()
  }

  async submit() {
    await this.page.findByDataQa('send-placement-button').click()
  }
}

export interface OccupancyValues {
  max3Months: string
  max6Months: string
  max3MonthsSpeculated: string
  max6MonthsSpeculated: string
}
