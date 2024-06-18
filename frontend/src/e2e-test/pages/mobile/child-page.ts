// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevChild } from '../../generated/api-types'
import { waitUntilEqual } from '../../utils'
import { Page, Element } from '../../utils/page'

export default class MobileChildPage {
  childName: Element
  reservation: Element
  termBreak: Element
  markPresentLink: Element
  markDepartedLink: Element
  markAbsentLink: Element
  cancelArrivalButton: Element
  returnToPresentButton: Element
  modalOkButton: Element
  markReservationsLink: Element
  markAbsentBeforehandLink: Element
  sensitiveInfoLink: Element
  messageEditorLink: Element
  notesLink: Element
  notesExistsBubble: Element
  saveNoteButton: Element
  goBack: Element
  goBackFromSensitivePage: Element
  constructor(private readonly page: Page) {
    this.childName = page.findByDataQa('child-name')
    this.reservation = page.findByDataQa('reservation')
    this.termBreak = page.findByDataQa('term-break')
    this.markPresentLink = page.findByDataQa('mark-present-link')
    this.markDepartedLink = page.findByDataQa('mark-departed-link')
    this.markAbsentLink = page.findByDataQa('mark-absent-link')
    this.cancelArrivalButton = page.findByDataQa('cancel-arrival-button')
    this.returnToPresentButton = page.findByDataQa('return-to-present-btn')
    this.modalOkButton = page.findByDataQa('modal-okBtn')
    this.markReservationsLink = page.findByDataQa('mark-reservations')
    this.markAbsentBeforehandLink = page.findByDataQa('mark-absent-beforehand')
    this.sensitiveInfoLink = page.findByDataQa('link-child-sensitive-info')
    this.messageEditorLink = page.findByDataQa('link-new-message')
    this.notesLink = page.findByDataQa('link-child-daycare-daily-note')
    this.notesExistsBubble = page.findByDataQa('daily-note-icon-bubble')
    this.saveNoteButton = page.findByDataQa('create-daily-note-btn')
    this.goBack = page.findByDataQa('back-btn')
    this.goBackFromSensitivePage = page.findByDataQa('go-back')
  }

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
    additionalInfo: DevChild,
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
