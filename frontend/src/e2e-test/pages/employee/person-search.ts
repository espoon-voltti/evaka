// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { Checkbox, Page, TextInput } from '../../utils/page'

export default class PersonSearchPage {
  constructor(private readonly page: Page) {}

  searchInput = new TextInput(this.page.findByDataQa('search-input'))
  searchResults = this.page.findAllByDataQa('person-row')
  #personLink = this.page.find('[data-qa="person-row"] a')
  #createPersonButton = this.page.findByDataQa('create-person-button')
  #createPersonModal = {
    modal: this.page.findByDataQa('modal'),
    firstNameInput: new TextInput(this.page.findByDataQa('first-name-input')),
    lastNameInput: new TextInput(this.page.findByDataQa('last-name-input')),
    dateOfBirthInput: new TextInput(
      this.page.findByDataQa('date-of-birth-input')
    ),
    streetAddressInput: new TextInput(
      this.page.findByDataQa('street-address-input')
    ),
    postalCodeInput: new TextInput(this.page.findByDataQa('postal-code-input')),
    postOfficeInput: new TextInput(this.page.findByDataQa('post-office-input'))
  }
  #personData = {
    firstName: this.page.findByDataQa('person-first-names'),
    lastName: this.page.findByDataQa('person-last-name'),
    dateOfBirth: this.page.findByDataQa('person-birthday'),
    address: this.page.findByDataQa('person-address'),
    ssn: this.page.findByDataQa('person-ssn')
  }
  #addSsnButton = this.page.findByDataQa('add-ssn-button')
  #noSsnText = this.page.findByDataQa('no-ssn')
  #disableSsnAddingCheckbox = new Checkbox(
    this.page.findByDataQa('disable-ssn-adding')
  )
  #ssnInput = new TextInput(this.page.findByDataQa('ssn-input'))
  #modalConfirm = this.page.findByDataQa('modal-okBtn')

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
    await this.#createPersonModal.modal.waitUntilVisible()
    await this.#modalConfirm.click()
    await this.#createPersonModal.modal.waitUntilHidden()
  }

  async findPerson(searchString: string) {
    await this.searchInput.fill(searchString)
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
    await this.#personData.firstName.assertTextEquals(personData.firstName)
    await this.#personData.lastName.assertTextEquals(personData.lastName)
    await this.#personData.dateOfBirth.assertTextEquals(
      personData.dateOfBirth.format()
    )
    await this.#personData.address.assertTextEquals(
      `${personData.streetAddress}, ${personData.postalCode} ${personData.postOffice}`
    )
    if (personData.ssn === undefined) {
      await this.#personData.ssn
        .findByDataQa('add-ssn-button')
        .waitUntilVisible()
      await this.#personData.ssn
        .findByDataQa('add-ssn-button')
        .assertTextEquals('Aseta hetu')
    } else {
      await this.#personData.ssn.assertTextEquals(personData.ssn)
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
