// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator, Page } from 'playwright'
import { UUID } from 'lib-common/types'

export default class UnitPage {
  constructor(private readonly page: Page) {}

  readonly #unitInfoTab = this.page.locator('[data-qa="unit-info-tab"]')

  async openUnitInformation(): Promise<UnitInformationSection> {
    await this.#unitInfoTab.click()
    const section = new UnitInformationSection(this.page)
    await section.waitFor()
    return section
  }
}

class UnitInformationSection {
  constructor(private readonly page: Page) {}

  readonly staffAcl = new StaffAclSection(
    this.page.locator('[data-qa="daycare-acl-staff"]')
  )

  async waitFor() {
    await this.page.locator('[data-qa="unit-name"]').waitFor()
  }
}

class StaffAclSection {
  constructor(private root: Locator) {}

  readonly #table = this.root.locator('[data-qa="acl-table"]')
  readonly #tableRows = this.#table.locator('[data-qa="acl-row"]')

  readonly #combobox = this.root.locator('[data-qa="acl-combobox"]')
  readonly #addButton = this.root.locator('[data-qa="acl-add-button"]')

  async addEmployeeAcl(employeeEmail: string, employeeId: UUID) {
    await this.#combobox.click()
    await this.#combobox.locator('input').type(employeeEmail)
    await this.#combobox.locator(`[data-qa="value-${employeeId}"]`).click()
    await this.#addButton.click()
    await this.#table.waitFor()
  }

  get rows() {
    return this.#tableRows.evaluateAll((rows) =>
      rows.map((row) => ({
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
      this.#table.locator(
        `tbody tr:nth-child(${rowIndex + 1})[data-qa="acl-row"]`
      )
    )
  }
}

class StaffAclRow {
  constructor(private root: Locator) {}

  readonly #editButton = this.root.locator('[data-qa="edit"]')

  async edit(): Promise<StaffAclRowEditor> {
    await this.#editButton.click()
    return new StaffAclRowEditor(this.root)
  }
}

class StaffAclRowEditor {
  constructor(private root: Locator) {}

  readonly #groupEditor = this.root.locator('[data-qa="groups"]')
  readonly #save = this.root.locator('[data-qa="save"]')

  async toggleStaffGroups(groupIds: UUID[]) {
    await this.#groupEditor.locator('> div').click()
    for (const groupId of groupIds) {
      await this.#groupEditor
        .locator(`[data-qa="option"][data-id="${groupId}"]`)
        .click()
    }
    await this.#groupEditor.locator('> div').click()
  }

  async save(): Promise<StaffAclRow> {
    await this.#save.click()
    return new StaffAclRow(this.root)
  }
}
