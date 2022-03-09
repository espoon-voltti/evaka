// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { waitUntilEqual } from '../../utils'
import { Page, Checkbox, TextInput } from '../../utils/page'

export default class PersonSearchPage {
  constructor(private readonly page: Page) {}

  #searchInput = new TextInput(this.page.find('[data-qa="search-input"]'))
  #personLink = this.page.find('[data-qa="person-row"] a')
  #createPersonButton = this.page.find('[data-qa="create-person-button"]')
  #createPersonModal = {
    firstNameInput: new TextInput(
      this.page.find('[data-qa="first-name-input"]')
    ),
    lastNameInput: new TextInput(this.page.find('[data-qa="last-name-input"]')),
    dateOfBirthInput: new TextInput(
      this.page.find('[data-qa="date-of-birth-input"]')
    ),
    streetAddressInput: new TextInput(
      this.page.find('[data-qa="street-address-input"]')
    ),
    postalCodeInput: new TextInput(
      this.page.find('[data-qa="postal-code-input"]')
    ),
    postOfficeInput: new TextInput(
      this.page.find('[data-qa="post-office-input"]')
    )
  }
  #personData = {
    firstName: this.page.find('[data-qa="person-first-names"]'),
    lastName: this.page.find('[data-qa="person-last-name"]'),
    dateOfBirth: this.page.find('[data-qa="person-birthday"]'),
    address: this.page.find('[data-qa="person-address"]'),
    ssn: this.page.find('[data-qa="person-ssn"]')
  }
  #addSsnButton = this.page.find('[data-qa="add-ssn-button"]')
  #noSsnText = this.page.find('[data-qa="no-ssn"]')
  #disableSsnAddingCheckbox = new Checkbox(
    this.page.find('[data-qa="disable-ssn-adding"]')
  )
  #ssnInput = new TextInput(this.page.find('[data-qa="ssn-input"]'))
  #modalConfirm = this.page.find('[data-qa="modal-okBtn"]')

  async createPerson(personData: {
    firstName: string
    lastName: string
    dateOfBirth: LocalDate
    streetAddress: string
    postalCode: string
    postOffice: string
  }) {
    await this.#createPersonButton.click()
    await this.#createPersonModal.firstNameInput.type(personData.firstName)
    await this.#createPersonModal.lastNameInput.type(personData.lastName)
    await this.#createPersonModal.dateOfBirthInput.type(
      personData.dateOfBirth.format()
    )
    await this.#createPersonModal.streetAddressInput.type(
      personData.streetAddress
    )
    await this.#createPersonModal.postalCodeInput.type(personData.postalCode)
    await this.#createPersonModal.postOfficeInput.type(personData.postOffice)
    await this.#modalConfirm.click()
  }

  async findPerson(searchString: string) {
    await this.#searchInput.fill(searchString)
    await this.#personLink.click()
  }

  async assertPersonData(personData: {
    firstName: string
    lastName: string
    dateOfBirth: LocalDate
    streetAddress: string
    postalCode: string
    postOffice: string
    ssn?: string
  }) {
    await waitUntilEqual(
      () => this.#personData.firstName.innerText,
      personData.firstName
    )
    await waitUntilEqual(
      () => this.#personData.lastName.innerText,
      personData.lastName
    )
    await waitUntilEqual(
      () => this.#personData.dateOfBirth.innerText,
      personData.dateOfBirth.format()
    )
    await waitUntilEqual(
      () => this.#personData.address.innerText,
      `${personData.streetAddress}, ${personData.postalCode} ${personData.postOffice}`
    )
    if (personData.ssn === undefined) {
      await this.#personData.ssn
        .findByDataQa('add-ssn-button')
        .waitUntilVisible()
      await waitUntilEqual(
        () => this.#personData.ssn.findByDataQa('add-ssn-button').innerText,
        'Aseta hetu'
      )
    } else {
      await waitUntilEqual(() => this.#personData.ssn.innerText, personData.ssn)
    }
  }

  async addSsn(ssn: string) {
    await this.#addSsnButton.click()
    await this.#ssnInput.type(ssn)
    await this.#modalConfirm.click()
  }

  async checkAddSsnButtonVisibility(visible: boolean) {
    if (visible) {
      await this.#addSsnButton.waitUntilVisible()
    } else {
      await this.#noSsnText.waitUntilVisible()
    }
  }

  async disableSsnAdding(disabled: boolean) {
    if ((await this.#disableSsnAddingCheckbox.checked) === disabled) {
      return
    }

    await this.#disableSsnAddingCheckbox.click()
  }
}
