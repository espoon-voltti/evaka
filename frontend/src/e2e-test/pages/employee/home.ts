// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import config from 'e2e-test-common/config'
import DevLoginForm, { DevLoginUser } from '../dev-login-form'

export default class EmployeeHome {
  private readonly devLoginForm = new DevLoginForm()
  readonly loginBtn = Selector('[data-qa="login-btn"]')
  readonly logoutBtn = Selector('[data-qa="logout-btn"]')

  readonly url = config.employeeUrl

  readonly unitsNav = Selector('[data-qa="units-nav"]')
  readonly personSearchNav = Selector('[data-qa="search-nav"]')
  readonly addVTJPersonButton = Selector('[data-qa="add-vtj-person-button"]')
  readonly createPersonButton = Selector('[data-qa="create-person-button"]')
  readonly messagesNav = Selector('[data-qa="messages-nav"]')

  readonly createPersonModal = {
    firstNameInput: Selector('[data-qa="first-name-input"]'),
    lastNameInput: Selector('[data-qa="last-name-input"]'),
    dateOfBirthInput: Selector('.react-datepicker-wrapper').find('input'),
    streetAddressInput: Selector('[data-qa="street-address-input"]'),
    postalCodeInput: Selector('[data-qa="postal-code-input"]'),
    postOfficeInput: Selector('[data-qa="post-office-input"]'),
    phoneInput: Selector('[data-qa="phone-input"]'),
    emailInput: Selector('[data-qa="email-input"]'),
    confirmButton: Selector('[data-qa="modal-okBtn"]'),
    cancelButton: Selector('[data-qa="modal-cancelBtn"]')
  }

  readonly personSearch = {
    searchFilter: Selector('[data-qa="search-input"]'),
    searchResults: Selector('[data-qa="person-row"]'),
    firstResult: Selector('[data-qa="person-row"]').nth(0),
    async filterByName(name: string) {
      await t.typeText(this.searchFilter, name, { replace: true })
    },
    async navigateToNthPerson(n: number) {
      await t.click(this.searchResults.nth(n).find('a').nth(0))
    }
  }

  async login(user: DevLoginUser) {
    await t.click(this.loginBtn)
    await this.devLoginForm.login(user)
    await t.expect(this.logoutBtn.visible).ok()
  }

  async navigateToUnits() {
    await t.click(this.unitsNav)
  }

  async navigateToPersonSearch() {
    await t.click(this.personSearchNav)
  }

  async navigateToChildInformation(id: string) {
    await t.navigateTo(this.url + '/child-information/' + id)
  }

  async navigateToGuardianInformation(personId: string) {
    await t.navigateTo(this.url + '/profile/' + personId)
  }

  async navigateToMessages() {
    await t.click(this.messagesNav)
  }
}
