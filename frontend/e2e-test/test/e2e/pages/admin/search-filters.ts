// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { format } from 'date-fns'
import { ApplicationStatus } from '@evaka/lib-common/src/api-types/application/enums'
import { Checkbox } from '../../utils/helpers'

const selectDateRangePickerDates = async (
  startDate: Date,
  endDate: Date
): Promise<void> => {
  await t.typeText(
    Selector('.datepicker-start .date-picker'),
    format(startDate, 'dd.MM.yyyy')
  )
  await t.typeText(
    Selector('.datepicker-end .date-picker'),
    format(endDate, 'dd.MM.yyyy')
  )
}

export default class SearchFilter {
  readonly areasFilter = Selector('.areas-filter')
  readonly radioBtnArea = Selector(
    this.areasFilter.find('[data-qa="radio-button"]').nth(5)
  )
  readonly areas = this.areasFilter.find('.radio-wrapper')

  readonly dateFilterDatePicker = Selector('.date-filter .date-picker')
  readonly statusRadioAll: Selector = Selector(
    '[data-qa="application-status-filter-ALL"]'
  )
  readonly statusCheckBoxSent = new Checkbox(
    Selector(
      '[data-qa="filter-status-all-VERIFIED,WAITING_PLACEMENT,PLACEMENT_PROPOSED,PLACEMENT_QUEUED"]'
    )
  )
  readonly statusCheckBoxWaitingDecision = new Checkbox(
    Selector('[data-qa="filter-status-all-WAITING_DECISION"]')
  )
  readonly statusCheckBoxConfirm = new Checkbox(
    Selector(
      '[data-qa="filter-status-all-CLEARED_FOR_CONFIRMATION,WAITING_CONFIRMATION"]'
    )
  )
  readonly statusCheckBoxAccepted = new Checkbox(
    Selector('[data-qa="filter-status-all-ACTIVE"]')
  )
  readonly statusCheckBoxRejected = new Checkbox(
    Selector('[data-qa="filter-status-all-REJECTED"]')
  )
  readonly statusCheckBoxCancelled = new Checkbox(
    Selector('[data-qa="filter-status-all-CANCELLED,TERMINATED"]')
  )

  async selectDateRangeFilterStartAndEndDate(startDate: Date, endDate: Date) {
    await selectDateRangePickerDates(startDate, endDate)
  }

  static newSearch() {
    return new SearchFilter()
  }

  async chooseArea() {
    await t.click(this.radioBtnArea)
  }

  async chooseAreaByIndex(i: number) {
    await t.click(this.areas.find('.label').nth(i))
  }

  async chooseAreaByName(areaName: string) {
    await t.click(Selector(`[data-qa="filter-option-${areaName}"]`))
  }

  async filterByTransferOnly() {
    await t.click(Selector(`[data-qa="filter-transfer-only"]`))
  }

  async filterByTransferExclude() {
    await t.click(Selector(`[data-qa="filter-transfer-exclude"]`))
  }

  async filterByApplicationStatus(status: ApplicationStatus) {
    await t.click(this.statusRadioAll)
    await t.click(this.statusRadioAll)
    await new Checkbox(
      Selector(`[data-qa="application-status-filter-all-${status}"]`)
    ).click()
  }
}
