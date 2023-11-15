// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Child } from '../../dev-api/types'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'

export default class MobileChildPage {
  constructor(private readonly page: Page) {}

  childName = this.page.findByDataQa('child-name')
  termBreak = this.page.findByDataQa('term-break')
  markPresentLink = this.page.findByDataQa('mark-present-link')
  markDepartedLink = this.page.findByDataQa('mark-departed-link')
  markAbsentLink = this.page.findByDataQa('mark-absent-link')
  cancelArrivalButton = this.page.findByDataQa('cancel-arrival-button')
  returnToPresentButton = this.page.findByDataQa('return-to-present-btn')

  modalOkButton = this.page.findByDataQa('modal-okBtn')

  markReservationsLink = this.page.findByDataQa('mark-reservations')
  markAbsentBeforehandLink = this.page.findByDataQa('mark-absent-beforehand')
  sensitiveInfoLink = this.page.findByDataQa('link-child-sensitive-info')

  messageEditorLink = this.page.findByDataQa('link-new-message')

  notesLink = this.page.findByDataQa('link-child-daycare-daily-note')

  notesExistsBubble = this.page.findByDataQa('daily-note-icon-bubble')

  saveNoteButton = this.page.findByDataQa('create-daily-note-btn')

  goBack = this.page.findByDataQa('back-btn')
  goBackFromSensitivePage = this.page.findByDataQa('go-back')

  sensitiveInfo = {
    name: this.page.findByDataQa('child-info-name'),
    allergies: this.page.findByDataQa('child-info-allergies'),
    diet: this.page.findByDataQa('child-info-diet'),
    medication: this.page.findByDataQa('child-info-medication'),
    contactName: (n: number) =>
      this.page.findByDataQa(`child-info-contact${n + 1}-name`),
    contactPhone: (n: number) =>
      this.page.findByDataQa(`child-info-contact${n + 1}-phone`),
    contactEmail: (n: number) =>
      this.page.findByDataQa(`child-info-contact${n + 1}-email`),
    backupPickupName: (n: number) =>
      this.page.findByDataQa(`child-info-backup-pickup${n + 1}-name`),
    backupPickupPhone: (n: number) =>
      this.page.findByDataQa(`child-info-backup-pickup${n + 1}-phone`)
  }

  attendance = {
    arrivalTimes: this.page.findAllByDataQa('arrival-time'),
    departureTimes: this.page.findAllByDataQa('departure-time')
  }

  async waitUntilLoaded() {
    await this.childName.waitUntilVisible()
  }

  async assertSensitiveInfoIsShown(name: string) {
    await this.sensitiveInfo.name.assertTextEquals(name)
  }

  async assertSensitiveInfo(
    additionalInfo: Child,
    contacts: {
      firstName: string
      lastName: string
      phone?: string
      email?: string | null
    }[],
    backupPickups: {
      name: string
      phone: string
    }[]
  ) {
    await this.sensitiveInfo.allergies.assertTextEquals(
      additionalInfo.allergies ?? 'should be defined'
    )
    await this.sensitiveInfo.diet.assertTextEquals(
      additionalInfo.diet ?? 'should be defined'
    )
    await this.sensitiveInfo.medication.assertTextEquals(
      additionalInfo.medication ?? 'should be defined'
    )

    for (let i = 0; i < contacts.length; i++) {
      const contact = contacts[i]
      await this.sensitiveInfo
        .contactName(i)
        .assertTextEquals(`${contact.firstName} ${contact.lastName}`)
      if (contact.phone) {
        await this.sensitiveInfo.contactPhone(i).assertTextEquals(contact.phone)
      }
      if (contact.email) {
        await this.sensitiveInfo.contactEmail(i).assertTextEquals(contact.email)
      }
    }

    for (let i = 0; i < backupPickups.length; i++) {
      const backupPickup = backupPickups[i]
      await this.sensitiveInfo
        .backupPickupName(i)
        .assertTextEquals(backupPickup.name)
      await this.sensitiveInfo
        .backupPickupPhone(i)
        .assertTextEquals(backupPickup.phone)
    }
  }

  async openNotes() {
    await this.notesLink.click()
    await this.saveNoteButton.waitUntilVisible()
  }

  async assertArrivalTimeInfoIsShown(arrivalTimeText: string) {
    await waitUntilEqual(
      () =>
        this.attendance.arrivalTimes
          .allTexts()
          .then((texts) =>
            texts.map((text) => text.replace(/\s/g, '')).join(',')
          ),
      arrivalTimeText
    )
  }

  async assertDepartureTimeInfoIsShown(departureTimeText: string) {
    await waitUntilEqual(
      () =>
        this.attendance.departureTimes
          .allTexts()
          .then((texts) =>
            texts.map((text) => text.replace(/\s/g, '')).join(',')
          ),
      departureTimeText
    )
  }
}
