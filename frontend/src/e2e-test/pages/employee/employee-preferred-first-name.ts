// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'assert'

import { waitUntilEqual } from '../../utils'
import { AsyncButton, Page, Select } from '../../utils/page'

export class EmployeePreferredFirstNamePage {
  preferredFirstNameSelect: Select
  confirmButton: AsyncButton
  constructor(private readonly page: Page) {
    this.preferredFirstNameSelect = new Select(
      page.findByDataQa('select-preferred-first-name')
    )
    this.confirmButton = new AsyncButton(page.findByDataQa('confirm-button'))
  }

  async assertSelectedPreferredFirstName(expectedPreferredFirstName: string) {
    await waitUntilEqual(
      () => this.preferredFirstNameSelect.selectedOption,
      expectedPreferredFirstName
    )
  }

  async assertPreferredFirstNameOptions(
    expectedPreferredFirstNameOptions: string[]
  ) {
    const preferredFirstNameOptions =
      await this.preferredFirstNameSelect.allOptions
    expectedPreferredFirstNameOptions.forEach((expected) =>
      assert(preferredFirstNameOptions.includes(expected))
    )
  }

  async preferredFirstName(preferredFirstName: string) {
    await this.preferredFirstNameSelect.selectOption(preferredFirstName)
  }

  async confirm() {
    await this.confirmButton.click()
    await this.confirmButton.waitUntilIdle()
  }
}
