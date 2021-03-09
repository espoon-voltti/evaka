// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { ApplicationStatus } from '@evaka/lib-common/api-types/application/enums'
import { Checkbox } from '../../utils/helpers'

export default class SearchFilter {
  readonly statusRadioAll: Selector = Selector(
    '[data-qa="application-status-filter-ALL"]'
  )

  static newSearch() {
    return new SearchFilter()
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
