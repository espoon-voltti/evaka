// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput, Element, ElementCollection } from '../../utils/page'

export default class ApplicationDetailsPage {
  #guardianName: Element
  #vtjGuardianName: Element
  #otherGuardianAgreementStatus: Element
  #otherGuardianSameAddress: Element
  #noOtherVtjGuardianText: Element
  #applicationStatus: Element
  #notes: ElementCollection
  constructor(private page: Page) {
    this.#guardianName = page.findByDataQa('guardian-name')
    this.#vtjGuardianName = page.findByDataQa('vtj-guardian-name')
    this.#otherGuardianAgreementStatus = page.findByDataQa('agreement-status')
    this.#otherGuardianSameAddress = page.findByDataQa(
      'other-vtj-guardian-lives-in-same-address'
    )
    this.#noOtherVtjGuardianText = page.findByDataQa('no-other-vtj-guardian')
    this.#applicationStatus = page.findByDataQa('application-status')
    this.#notes = page.findAllByDataQa('note-container')
  }

  async assertGuardianName(expectedName: string) {
    await this.#guardianName.findText(expectedName).waitUntilVisible()
  }

  async assertNoOtherVtjGuardian() {
    await this.#noOtherVtjGuardianText.waitUntilVisible()
  }

  async assertVtjGuardianName(expectedName: string) {
    await this.#vtjGuardianName.findText(expectedName).waitUntilVisible()
  }

  async assertOtherGuardianSameAddress(status: boolean) {
    await this.#otherGuardianSameAddress
      .findText(status ? 'Kyllä' : 'Ei')
      .waitUntilVisible()
  }

  async assertOtherGuardianAgreementStatus(_status: false) {
    const expectedText = 'Ei ole sovittu yhdessä'
    await this.#otherGuardianAgreementStatus
      .findText(expectedText)
      .waitUntilVisible()
  }

  async assertApplicationStatus(text: string) {
    await this.#applicationStatus.findText(text).waitUntilVisible()
  }

  async assertNote(index: number, note: string) {
    await this.#notes
      .nth(index)
      .findByDataQa('application-note-content')
      .assertTextEquals(note)
  }

  async assertNoNote(index: number) {
    await this.#notes.nth(index).waitUntilHidden()
  }

  async addNote(note: string) {
    await this.page.findByDataQa('add-note').click()
    const noteTextArea = new TextInput(this.#notes.nth(0).find('textarea'))
    await noteTextArea.fill(note)
    await this.page.findByDataQa('save-note').click()
  }

  async editNote(index: number, note: string) {
    const noteContainer = this.#notes.nth(index)
    await noteContainer.findByDataQa('edit-note').click()
    const input = new TextInput(noteContainer.find('textarea'))
    await input.fill(note)
    await noteContainer.findByDataQa('save-note').click()
  }

  async deleteNote(index: number) {
    await this.#notes.nth(index).findByDataQa('delete-note').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }
}
