// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Combobox,
  DatePickerDeprecated,
  Element,
  Page,
  Radio,
  Select,
  TextInput
} from '../../../utils/page'

import ApplicationEditView from './application-edit-view'

export default class CreateApplicationModal extends Element {
  constructor(private page: Page, root: Element) {
    super(root)
  }

  #submit = this.find('[data-qa="modal-okBtn"]')

  async submit() {
    await this.#submit.click()
    return new ApplicationEditView(this.page)
  }

  #guardianRadio = new Radio(this.find('[data-qa="select-guardian-radio"]'))
  #guardianSelect = new Select(this.find('[data-qa="select-guardian"]'))

  async selectGuardian(name: string) {
    await this.#guardianRadio.check()
    await this.#guardianSelect.selectOption({ label: name })
  }

  #vtjPersonRadio = new Radio(this.find('[data-qa="vtj-person-radio"]'))
  #vtjPersonSelect = new Combobox(
    this.find('[data-qa="select-search-from-vtj-guardian"]')
  )

  async selectVtjPersonAsGuardian(ssn: string) {
    await this.#vtjPersonRadio.check()
    await this.#vtjPersonSelect.fillAndSelectFirst(ssn)
  }

  #newNoSsnRadio = new Radio(this.find('[data-qa="radio-new-no-ssn"]'))
  #firstNameInput = new TextInput(this.find('[data-qa="input-first-name"]'))
  #lastNameInput = new TextInput(this.find('[data-qa="input-last-name"]'))
  #dob = new DatePickerDeprecated(this.find('[data-qa="datepicker-dob"]'))
  #streetAddress = new TextInput(this.find('[data-qa="input-street-address"]'))
  #postalCode = new TextInput(this.find('[data-qa="input-postal-code"]'))
  #postOffice = new TextInput(this.find('[data-qa="input-post-office"]'))
  #phone = new TextInput(this.find('[data-qa="input-phone"]'))
  #email = new TextInput(this.find('[data-qa="input-email"]'))

  async selectCreateNewPersonAsGuardian(
    firstName: string,
    lastName: string,
    dob: string,
    streetAddress: string,
    postalCode: string,
    postOffice: string,
    phone: string,
    email: string
  ) {
    await this.#newNoSsnRadio.check()
    await this.#firstNameInput.fill(firstName)
    await this.#lastNameInput.fill(lastName)
    await this.#dob.fill(dob)
    await this.#streetAddress.fill(streetAddress)
    await this.#postalCode.fill(postalCode)
    await this.#postOffice.fill(postOffice)
    await this.#phone.fill(phone)
    await this.#email.fill(email)
  }
}
