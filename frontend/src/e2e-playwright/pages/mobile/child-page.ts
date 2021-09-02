// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import {
  Combobox,
  RawElement,
  RawTextInput
} from 'e2e-playwright/utils/element'
import { Child } from 'e2e-test-common/dev-api/types'
import { Page } from 'playwright'

export default class MobileChildPage {
  constructor(private readonly page: Page) {}

  #markAbsentBeforehandLink = new RawElement(
    this.page,
    '[data-qa="mark-absent-beforehand"]'
  )
  #sensitiveInfoLink = new RawElement(
    this.page,
    '[data-qa="link-child-sensitive-info"]'
  )
  #goBack = new RawElement(this.page, '[data-qa="go-back"]')
  #staffCombobox = new Combobox(this.page, '[data-qa="select-staff"]')
  #pinInput = new RawTextInput(this.page, '[data-qa="set-pin"]')
  #pinInfo = new RawElement(this.page, '[data-qa="set-pin-info"]')
  #submitPin = new RawElement(this.page, '[data-qa="submit-pin"]')
  #sensitiveInfo = {
    name: new RawElement(this.page, '[data-qa="child-info-name"]'),
    allergies: new RawElement(this.page, '[data-qa="child-info-allergies"]'),
    diet: new RawElement(this.page, '[data-qa="child-info-diet"]'),
    medication: new RawElement(this.page, '[data-qa="child-info-medication"]'),
    contactName: (n: number) =>
      new RawElement(this.page, `[data-qa="child-info-contact${n + 1}-name"]`),
    contactPhone: (n: number) =>
      new RawElement(this.page, `[data-qa="child-info-contact${n + 1}-phone"]`),
    contactEmail: (n: number) =>
      new RawElement(this.page, `[data-qa="child-info-contact${n + 1}-email"]`),
    backupPickupName: (n: number) =>
      new RawElement(
        this.page,
        `[data-qa="child-info-backup-pickup${n + 1}-name"]`
      ),
    backupPickupPhone: (n: number) =>
      new RawElement(
        this.page,
        `[data-qa="child-info-backup-pickup${n + 1}-phone"]`
      )
  }

  async markFutureAbsences() {
    return this.#markAbsentBeforehandLink.click()
  }

  async goBack() {
    await this.#goBack.click()
  }

  async openSensitiveInfoWithPinCode(
    employeeName: string,
    employeePin: string
  ) {
    await this.#sensitiveInfoLink.click()
    await this.#staffCombobox.fill(employeeName)
    await this.#staffCombobox.findItem(employeeName).click()
    await this.#pinInput.click()
    await this.#pinInput.type(employeePin)
    await this.#submitPin.click()
  }

  async assertWrongPinError() {
    await waitUntilEqual(() => this.#pinInfo.innerText, 'Väärä PIN-koodi')
  }

  async assertSensitiveInfoIsShown(name: string) {
    await waitUntilEqual(() => this.#sensitiveInfo.name.innerText, name)
  }

  async assertSensitiveInfo(
    additionalInfo: Child,
    contacts: Array<{
      firstName: string
      lastName: string
      phone?: string
      email?: string
    }>,
    backupPickups: Array<{
      name: string
      phone: string
    }>
  ) {
    await waitUntilEqual(
      () => this.#sensitiveInfo.allergies.innerText,
      additionalInfo.allergies
    )
    await waitUntilEqual(
      () => this.#sensitiveInfo.diet.innerText,
      additionalInfo.diet
    )
    await waitUntilEqual(
      () => this.#sensitiveInfo.medication.innerText,
      additionalInfo.medication
    )

    for (let i = 0; i < contacts.length; i++) {
      const contact = contacts[i]
      await waitUntilEqual(
        () => this.#sensitiveInfo.contactName(i).innerText,
        `${contact.firstName} ${contact.lastName}`
      )
      if (contact.phone) {
        await waitUntilEqual(
          () => this.#sensitiveInfo.contactPhone(i).innerText,
          contact.phone
        )
      }
      if (contact.email) {
        await waitUntilEqual(
          () => this.#sensitiveInfo.contactEmail(i).innerText,
          contact.email
        )
      }
    }

    for (let i = 0; i < backupPickups.length; i++) {
      const backupPickup = backupPickups[i]
      await waitUntilEqual(
        () => this.#sensitiveInfo.backupPickupName(i).innerText,
        backupPickup.name
      )
      await waitUntilEqual(
        () => this.#sensitiveInfo.backupPickupPhone(i).innerText,
        backupPickup.phone
      )
    }
  }
}
