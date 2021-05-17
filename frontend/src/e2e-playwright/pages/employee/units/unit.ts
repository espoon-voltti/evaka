// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { RawElement } from 'e2e-playwright/utils/element'
import { UUID } from 'e2e-test-common/dev-api/types'

export default class UnitPage {
  constructor(private readonly page: Page) {}

  readonly #unitInfoTab = new RawElement(this.page, '[data-qa="unit-info-tab"]')

  async openUnitInformation(): Promise<UnitInformationSection> {
    await this.#unitInfoTab.click()
    const section = new UnitInformationSection(this.page)
    await section.unitName.waitUntilVisible()
    return section
  }
}

class UnitInformationSection {
  constructor(private readonly page: Page) {}

  readonly staffAcl = new StaffAclSection(
    this.page,
    '[data-qa="daycare-acl-staff"]'
  )
  readonly unitName = new RawElement(this.page, '[data-qa="unit-name"]')
}

class StaffAclSection extends RawElement {
  readonly #table = this.find('[data-qa="acl-table"]')
  readonly #addInput = this.find('.acl-select')
  readonly #addButton = this.find('[data-qa="acl-add-button"]')

  async waitUntilLoaded() {
    await this.#table.waitUntilVisible()
  }
  async addEmployeeAcl(employeeId: UUID) {
    await this.waitUntilLoaded()
    // typing text would be better, but it's buggy with react-select
    await this.#addInput.click()
    await this.find(`[data-qa="value-${employeeId}"]`).click()
    await this.#addButton.click()
    await this.waitUntilLoaded()
  }
  get rows() {
    return this.page.$$eval(
      `${this.#table.selector} [data-qa="acl-row"]`,
      (rows) =>
        rows
          // .filter((row): row is HTMLElement => row instanceof HTMLElement)
          .map((row) => ({
            name: row.querySelector('[data-qa="name"]')?.textContent ?? '',
            email: row.querySelector('[data-qa="email"]')?.textContent ?? '',
            groups: Array.from(
              row.querySelectorAll('[data-qa="groups"] > div')
            ).map((element) => element.textContent ?? '')
          }))
    )
  }
  async getRow(name: string): Promise<StaffAclRow> {
    const rowIndex = (await this.rows).findIndex((row) => row.name === name)
    if (rowIndex < 0) {
      throw new Error(`Row with name ${name} not found`)
    }
    return new StaffAclRow(
      this.page,
      `${this.#table.selector} tbody tr:nth-child(${
        rowIndex + 1
      })[data-qa="acl-row"]`
    )
  }
}

class StaffAclRow extends RawElement {
  readonly #editButton = this.find('[data-qa="edit"]')

  async edit(): Promise<StaffAclRowEditor> {
    await this.#editButton.click()
    return new StaffAclRowEditor(this.page, this.selector)
  }
}

class StaffAclRowEditor extends RawElement {
  readonly #groupEditor = this.find('[data-qa="groups"]')
  readonly #save = this.find('[data-qa="save"]')

  async toggleStaffGroups(groupIds: UUID[]) {
    await this.#groupEditor.find('> div').click()
    for (const groupId of groupIds) {
      await this.#groupEditor
        .find(`[data-qa="option"][data-id="${groupId}"]`)
        .click()
    }
    await this.#groupEditor.find('> div').click()
  }

  async save(): Promise<StaffAclRow> {
    await this.#save.click()
    return new StaffAclRow(this.page, this.selector)
  }
}
