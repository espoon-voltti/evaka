// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

import type { Page, ElementCollection, Element } from '../../utils/page'
import { Checkbox, TextInput } from '../../utils/page'

export default class PersonSearchPage {
  searchInput: TextInput
  searchResults: ElementCollection
  #createPersonButton: Element
  #addSsnButton: Element
  #noSsnText: Element
  #disableSsnAddingCheckbox: Checkbox
  #ssnInput: TextInput
  #modalConfirm: Element
  #personLink: Element
  #createPersonModal: {
    modal: Element
    firstNameInput: TextInput
    lastNameInput: TextInput
    dateOfBirthInput: TextInput
    streetAddressInput: TextInput
    postalCodeInput: TextInput
    postOfficeInput: TextInput
  }
  #personData: {
    firstName: Element
    lastName: Element
    dateOfBirth: Element
    address: Element
    ssn: Element
  }
  constructor(page: Page) {
    this.searchInput = new TextInput(page.findByDataQa('search-input'))
    this.searchResults = page.findAllByDataQa('person-row')
    this.#createPersonButton = page.findByDataQa('create-person-button')
    this.#addSsnButton = page.findByDataQa('add-ssn-button')
    this.#noSsnText = page.findByDataQa('no-ssn')
    this.#disableSsnAddingCheckbox = new Checkbox(
      page.findByDataQa('disable-ssn-adding')
    )
    this.#ssnInput = new TextInput(page.findByDataQa('ssn-input'))
    this.#modalConfirm = page.findByDataQa('modal-okBtn')
    this.#personLink = page.find('[data-qa="person-row"] a')
    this.#createPersonModal = {
      modal: page.findByDataQa('modal'),
      firstNameInput: new TextInput(page.findByDataQa('first-name-input')),
      lastNameInput: new TextInput(page.findByDataQa('last-name-input')),
      dateOfBirthInput: new TextInput(page.findByDataQa('date-of-birth-input')),
      streetAddressInput: new TextInput(
        page.findByDataQa('street-address-input')
      ),
      postalCodeInput: new TextInput(page.findByDataQa('postal-code-input')),
      postOfficeInput: new TextInput(page.findByDataQa('post-office-input'))
    }
    this.#personData = {
      firstName: page.findByDataQa('person-first-names'),
      lastName: page.findByDataQa('person-last-name'),
      dateOfBirth: page.findByDataQa('person-birthday'),
      address: page.findByDataQa('person-address'),
      ssn: page.findByDataQa('person-ssn')
    }
  }

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
