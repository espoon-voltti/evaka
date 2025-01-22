// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevEmployee } from 'e2e-test/generated/api-types'

import { Checkbox, Element, Page, TextInput } from '../../utils/page'

export class EmployeesPage {
  nameInput: TextInput
  hideDeactivated: Checkbox
  createNewSsnEmployee: Element
  createSsnEmployeeWizard: CreateSsnEmployeeWizard

  constructor(private readonly page: Page) {
    this.nameInput = new TextInput(page.findByDataQa('employee-name-filter'))
    this.hideDeactivated = new Checkbox(
      page.findByDataQa('hide-deactivated-checkbox')
    )
    this.createNewSsnEmployee = page.findByDataQa('create-new-ssn-employee')
    this.createSsnEmployeeWizard = new CreateSsnEmployeeWizard(
      page.findByDataQa('create-ssn-employee-wizard')
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

  async openEmployeePage(employee: DevEmployee) {
    const name = `${employee.lastName} ${employee.firstName}`
    await this.page.findTextExact(name).click()
    return new EmployeePage(this.page)
  }
}

export class EmployeePage {
  content: Element

  constructor(private readonly page: Page) {
    this.content = this.page.find('#app')
  }
}

export class CreateSsnEmployeeWizard extends Element {
  ssn: TextInput
  firstName: TextInput
  lastName: TextInput
  email: TextInput
  ok: Element

  constructor(readonly root: Element) {
    super(root)
    this.ssn = new TextInput(root.findByDataQa('new-employee-ssn'))
    this.firstName = new TextInput(root.findByDataQa('new-employee-first-name'))
    this.lastName = new TextInput(root.findByDataQa('new-employee-last-name'))
    this.email = new TextInput(root.findByDataQa('new-employee-email'))
    this.ok = root.findByDataQa('modal-okBtn')
  }
}
