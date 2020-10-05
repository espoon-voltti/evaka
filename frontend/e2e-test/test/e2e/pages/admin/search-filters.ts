// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { GROUNDS, APPLICATION_TYPE, Status } from '../../const'
import { format } from 'date-fns'

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

  readonly filterByProcessingDateCheckbox = Selector(
    '.date-filter .checkbox'
  ).nth(0)
  readonly filterByCareStartDateCheckbox = Selector(
    '.date-filter .checkbox'
  ).nth(1)
  readonly filterByApplicationDateCheckbox = Selector(
    '.date-filter .checkbox'
  ).nth(2)

  readonly dateFilterDatePicker = Selector('.date-filter .date-picker')
  readonly statusRadioAll: Selector = Selector(
    '[data-qa="application-status-filter-ALL"]'
  )
  readonly statusCheckBoxSent: Selector = Selector(
    '[data-qa="filter-status-all-VERIFIED,WAITING_PLACEMENT,PLACEMENT_PROPOSED,PLACEMENT_QUEUED"]'
  )
  readonly statusCheckBoxWaitingDecision: Selector = Selector(
    '[data-qa="filter-status-all-WAITING_DECISION"]'
  )
  readonly statusCheckBoxConfirm: Selector = Selector(
    '[data-qa="filter-status-all-CLEARED_FOR_CONFIRMATION,WAITING_CONFIRMATION"]'
  )
  readonly statusCheckBoxAccepted: Selector = Selector(
    '[data-qa="filter-status-all-ACTIVE"]'
  )
  readonly statusCheckBoxRejected: Selector = Selector(
    '[data-qa="filter-status-all-REJECTED"]'
  )
  readonly statusCheckBoxCancelled: Selector = Selector(
    '[data-qa="filter-status-all-CANCELLED,TERMINATED"]'
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

  async chooseApplicationType(applicationType: APPLICATION_TYPE) {
    await t.click(Selector(`[data-qa="filter-option-${applicationType}"]`))
  }

  async chooseAreaByIndex(i: number) {
    await t.click(this.areas.find('.label').nth(i))
  }

  async chooseAreaByName(areaName: string) {
    await t.click(Selector(`[data-qa="filter-option-${areaName}"]`))
  }

  async filterByGround(ground: GROUNDS) {
    await t.click(Selector(`[data-qa="${ground}"]`))
  }

  async filterByApplicationType(applicationType: APPLICATION_TYPE) {
    await t.click(
      Selector(`[data-qa="filter-option-${applicationType.toUpperCase()}"]`)
    )
  }

  async filterByTransferOnly() {
    await t.click(Selector(`[data-qa="filter-transfer-only"]`))
  }

  async filterByTransferExclude() {
    await t.click(Selector(`[data-qa="filter-transfer-exclude"]`))
  }

  async filterByProcessingDate(processingDate: Date) {
    const checked = await this.filterByProcessingDateCheckbox.hasClass('active')
    if (!checked) await t.click(this.filterByProcessingDateCheckbox)
    await this.selectDateRangeFilterStartAndEndDate(
      processingDate,
      processingDate
    )
  }

  async filterByCareStartDate(
    careStartStartDate: Date,
    careStartEndDate: Date
  ) {
    const checked = await this.filterByCareStartDateCheckbox.hasClass('active')
    if (!checked) await t.click(this.filterByCareStartDateCheckbox)
    await this.selectDateRangeFilterStartAndEndDate(
      careStartStartDate,
      careStartEndDate
    )
  }

  async filterByApplicationArrivalDate(arrivalDate: Date) {
    const checked = await this.filterByApplicationDateCheckbox.hasClass(
      'active'
    )
    if (!checked) await t.click(this.filterByApplicationDateCheckbox)
    await this.selectDateRangeFilterStartAndEndDate(arrivalDate, arrivalDate)
  }

  async filterByApplicationStatus(status: Status) {
    await t.click(this.statusRadioAll)
    await t.click(this.statusRadioAll)
    await t.click(
      Selector(`[data-qa="application-status-filter-all-${status}"]`, {
        timeout: 50
      })
    )
  }

  async checkOrUncheckStatus(
    checkbox: Selector,
    statuses: Status[],
    status: Status
  ) {
    if (statuses.includes(status) && !(await checkbox.checked)) {
      await t.click(checkbox)
    } else if (!statuses.includes(status) && (await checkbox.checked)) {
      await t.click(checkbox)
    }
  }
}
