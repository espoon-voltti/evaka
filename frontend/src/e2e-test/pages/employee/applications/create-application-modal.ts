// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/generated/api-types/application'

import {
  Combobox,
  DatePicker,
  Element,
  Page,
  Radio,
  Select,
  TextInput
} from '../../../utils/page'

import ApplicationEditView from './application-edit-view'

export default class CreateApplicationModal extends Element {
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  #submit = this.findByDataQa('modal-okBtn')

  async submit() {
    await this.#submit.click()
    return new ApplicationEditView(this.page)
  }

  #guardianRadio = new Radio(this.findByDataQa('select-guardian-radio'))
  #guardianSelect = new Select(this.findByDataQa('select-guardian'))

  async selectGuardian(name: string) {
    await this.#guardianRadio.check()
    await this.#guardianSelect.selectOption({ label: name })
  }

  #vtjPersonRadio = new Radio(this.findByDataQa('vtj-person-radio'))
  #vtjPersonSelect = new Combobox(
    this.findByDataQa('select-search-from-vtj-guardian')
  )

  async selectVtjPersonAsGuardian(ssn: string) {
    await this.#vtjPersonRadio.check()
    await this.#vtjPersonSelect.fillAndSelectFirst(ssn)
  }

  #newNoSsnRadio = new Radio(this.findByDataQa('radio-new-no-ssn'))
  #firstNameInput = new TextInput(this.findByDataQa('input-first-name'))
  #lastNameInput = new TextInput(this.findByDataQa('input-last-name'))
  #dob = new DatePicker(this.findByDataQa('datepicker-dob'))
  #streetAddress = new TextInput(this.findByDataQa('input-street-address'))
  #postalCode = new TextInput(this.findByDataQa('input-postal-code'))
  #postOffice = new TextInput(this.findByDataQa('input-post-office'))
  #phone = new TextInput(this.findByDataQa('input-phone'))
  #email = new TextInput(this.findByDataQa('input-email'))

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

  #applicationTypeSelect = new Select(
    this.findByDataQa('select-application-type')
  )

  async selectApplicationType(type: ApplicationType) {
    await this.#applicationTypeSelect.selectOption(type)
  }
}
