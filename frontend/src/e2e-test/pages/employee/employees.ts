// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Checkbox, Page, TextInput } from '../../utils/page'

export class EmployeesPage {
  nameInput: TextInput
  hideDeactivated: Checkbox

  constructor(private readonly page: Page) {
    this.nameInput = new TextInput(page.findByDataQa('employee-name-filter'))
    this.hideDeactivated = new Checkbox(
      page.findByDataQa('hide-deactivated-checkbox')
    )
  }

  get visibleUsers(): Promise<string[]> {
    return this.page.findAllByDataQa('employee-name').allTexts()
  }

  async activateEmployee(nth: number) {
    await this.page.findAllByDataQa('activate-button').nth(nth).click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async deactivateEmployee(nth: number) {
    await this.page.findAllByDataQa('deactivate-button').nth(nth).click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async clickDeactivatedEmployees() {
    await this.hideDeactivated.waitUntilVisible()
    await this.hideDeactivated.click()
  }
}
