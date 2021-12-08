// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Combobox, Element, Page } from 'e2e-playwright/utils/page'
import { UUID } from 'lib-common/types'

export default class UnitPage {
  constructor(private readonly page: Page) {}

  readonly #unitInfoTab = this.page.find('[data-qa="unit-info-tab"]')
  readonly #groupsTab = this.page.find('[data-qa="groups-tab"]')

  async openUnitInformation(): Promise<UnitInformationSection> {
    await this.#unitInfoTab.click()
    const section = new UnitInformationSection(this.page)
    await section.waitUntilVisible()
    return section
  }

  async openGroups(): Promise<GroupsSection> {
    await this.#groupsTab.click()
    const section = new GroupsSection(this.page)
    await section.waitUntilVisible()
    return section
  }
}

class UnitInformationSection {
  constructor(private readonly page: Page) {}

  readonly staffAcl = new StaffAclSection(
    this.page.find('[data-qa="daycare-acl-staff"]')
  )

  async waitUntilVisible() {
    await this.page.find('[data-qa="unit-name"]').waitUntilVisible()
  }
}

class StaffAclSection {
  constructor(private root: Element) {}

  readonly #table = this.root.find('[data-qa="acl-table"]')
  readonly #tableRows = this.#table.findAll('[data-qa="acl-row"]')

  readonly #combobox = new Combobox(this.root.find('[data-qa="acl-combobox"]'))
  readonly #addButton = this.root.find('[data-qa="acl-add-button"]')

  async addEmployeeAcl(employeeEmail: string, employeeId: UUID) {
    await this.#combobox.click()
    await this.#combobox.fillAndSelectItem(employeeEmail, employeeId)
    await this.#addButton.click()
    await this.#table.waitUntilVisible()
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
      this.#table.find(`tbody tr:nth-child(${rowIndex + 1})[data-qa="acl-row"]`)
    )
  }
}

class StaffAclRow {
  constructor(private root: Element) {}

  readonly #editButton = this.root.find('[data-qa="edit"]')

  async edit(): Promise<StaffAclRowEditor> {
    await this.#editButton.click()
    return new StaffAclRowEditor(this.root)
  }
}

class StaffAclRowEditor {
  constructor(private root: Element) {}

  readonly #groupEditor = this.root.find('[data-qa="groups"]')
  readonly #save = this.root.find('[data-qa="save"]')

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
    return new StaffAclRow(this.root)
  }
}

class GroupsSection {
  constructor(private readonly page: Page) {}

  readonly #groupCollapsible = (groupId: string, expectIsClosed = true) =>
    this.page
      .find(
        `[data-qa="daycare-group-collapsible-${groupId}"][data-status="${
          expectIsClosed ? 'closed' : 'open'
        }"]`
      )
      .find('[data-qa="group-name"]')

  async assertGroupCollapsibleIsClosed(groupId: string) {
    await this.#groupCollapsible(groupId, true).waitUntilVisible()
  }

  async assertGroupCollapsibleIsOpen(groupId: string) {
    await this.#groupCollapsible(groupId, false).waitUntilVisible()
  }

  async openGroupCollapsible(groupId: string) {
    await this.assertGroupCollapsibleIsClosed(groupId)
    await this.#groupCollapsible(groupId).click()
    await this.assertGroupCollapsibleIsOpen(groupId)
  }

  async closeGroupCollapsible(groupId: string) {
    await this.assertGroupCollapsibleIsOpen(groupId)
    await this.#groupCollapsible(groupId, false).click()
    await this.assertGroupCollapsibleIsClosed(groupId)
  }

  async waitUntilVisible() {
    await this.page
      .findAll('[data-qa="table-of-missing-groups"]')
      .nth(0)
      .waitUntilVisible()
  }
}
