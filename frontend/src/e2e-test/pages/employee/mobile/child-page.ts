// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ApplicationPersonDetail,
  DaycareDailyNote,
  DaycareDailyNoteLevel,
  DaycareDailyNoteReminder
} from '../../../dev-api/types'
import { t, Selector } from 'testcafe'
import { AbsenceType } from '../absences/absences-page'
import MobileGroupsPage from './mobile-groups'

export default class ChildPage {
  readonly childName = Selector('[data-qa="child-name"]')
  readonly childStatus = Selector('[data-qa="child-status"]')

  readonly markPresentLink = Selector('[data-qa="mark-present-link"]')
  readonly markDepartedLink = Selector('[data-qa="mark-departed-link"]')
  readonly markAbsentLink = Selector('[data-qa="mark-absent-link"]')
  readonly dailyNoteLink = Selector('[data-qa="link-child-daycare-daily-note"]')

  readonly setTimeInput = Selector('[data-qa="set-time"]')

  readonly markPresentBtn = Selector('[data-qa="mark-present-btn"]')
  readonly markDepartedBtn = Selector('[data-qa="mark-departed-btn"]')
  readonly markDepartedWithAbsenceBtn = Selector(
    '[data-qa="mark-departed-with-absence-btn"]'
  )
  readonly markAbsentBtn = Selector('[data-qa="mark-absent-btn"]')
  readonly returnToComingBtn = Selector('[data-qa="return-to-coming-btn"]')
  readonly returnToPresentBtn = Selector('[data-qa="return-to-present-btn"]')
  readonly createDailyNoteBtn = Selector('[data-qa="create-daily-note-btn"]')
  readonly openDeleteDialogBtn = Selector('[data-qa="open-delete-dialog-btn"]')
  readonly deleteDailyNoteBtn = Selector('[data-qa="delete-daily-note-btn"]')
  readonly backBtn = Selector('[data-qa="back-btn"]')

  readonly dailyNoteNoteInput = Selector('[data-qa="daily-note-note-input"]')
  readonly dailyNoteSleepingTimeInput = Selector(
    '[data-qa="sleeping-time-input"]'
  )
  readonly dailyNoteReminderNoteInput = Selector(
    '[data-qa="reminder-note-input"]'
  )

  readonly markAbsentRadio = (absenceType: AbsenceType) =>
    Selector(`[data-qa="mark-absent-${absenceType}"]`)
  readonly dailyNoteFeedingNote = (dailyNoteLevel: DaycareDailyNoteLevel) =>
    Selector(`[data-qa="feeding-note-${dailyNoteLevel}"]`)
  readonly dailyNoteSleepingNote = (dailyNoteLevel: DaycareDailyNoteLevel) =>
    Selector(`[data-qa="sleeping-note-${dailyNoteLevel}"]`)
  readonly dailyNoteReminders = (reminder: DaycareDailyNoteReminder) =>
    Selector(`[data-qa="reminders-${reminder}"]`)

  readonly absence = Selector('[data-qa="absence"]')

  async markPresent(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage,
    time: string
  ) {
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(mobileGroupsPage.childName(childFixture.id))
    await t
      .expect(this.childName.textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(this.markPresentLink)
    await t.typeText(this.setTimeInput, time)
    await t.click(this.markPresentBtn)

    await t.click(mobileGroupsPage.presentTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
  }

  async markDeparted(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage,
    time: string
  ) {
    await t.click(mobileGroupsPage.presentTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(mobileGroupsPage.childName(childFixture.id))
    await t
      .expect(this.childName.textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(this.markDepartedLink)
    await t.typeText(this.setTimeInput, time)
    await t.click(this.markDepartedBtn)

    await t.click(mobileGroupsPage.departedTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
  }

  async markAbsent(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage
  ) {
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(mobileGroupsPage.childName(childFixture.id))
    await t
      .expect(this.childName.textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(this.markAbsentLink)
    await t.click(this.markAbsentRadio('SICKLEAVE'))
    await t.click(this.markAbsentBtn)
    await t.click(this.backBtn)

    await t.click(mobileGroupsPage.absentTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
  }

  async returnToComing(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage
  ) {
    await t.click(mobileGroupsPage.presentTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
    await t.click(mobileGroupsPage.childName(childFixture.id))

    await t.click(this.returnToComingBtn)
    await t.click(mobileGroupsPage.comingTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
  }

  async returnToPresent(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage
  ) {
    await t.click(mobileGroupsPage.departedTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
    await t.click(mobileGroupsPage.childName(childFixture.id))

    await t.click(this.returnToPresentBtn)
    await t.click(mobileGroupsPage.presentTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)
  }

  async markDepartedAbsence(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage,
    time: string
  ) {
    await t.click(mobileGroupsPage.presentTab)
    await t
      .expect(mobileGroupsPage.childName(childFixture.id).textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(mobileGroupsPage.childName(childFixture.id))
    await t
      .expect(this.childName.textContent)
      .eql(`${childFixture.firstName} ${childFixture.lastName}`)

    await t.click(this.markDepartedLink)
    await t.typeText(this.setTimeInput, time)

    await t.click(this.markAbsentRadio('SICKLEAVE'))
    await t.click(this.markDepartedWithAbsenceBtn)

    await t.expect(this.absence.exists).ok()

    await t.expect(this.childStatus.textContent).contains('Lähtenyt')
  }

  async createDailyNote(
    childFixture: ApplicationPersonDetail,
    mobileGroupsPage: MobileGroupsPage,
    dailyNote: DaycareDailyNote
  ) {
    await t.click(mobileGroupsPage.childName(childFixture.id))
    await t.click(this.dailyNoteLink)

    await t.typeText(this.dailyNoteNoteInput, dailyNote.note)
    await t.click(this.dailyNoteFeedingNote(dailyNote.feedingNote))
    await t.click(this.dailyNoteSleepingNote(dailyNote.sleepingNote))
    await t.typeText(this.dailyNoteSleepingTimeInput, dailyNote.sleepingHours)
    for (const reminder of dailyNote.reminders) {
      await t.click(this.dailyNoteReminders(reminder))
    }
    await t.typeText(this.dailyNoteReminderNoteInput, dailyNote.reminderNote)

    await t.click(this.createDailyNoteBtn)
    await t.click(this.backBtn)
  }

  async deleteDailyNote() {
    await t.click(this.openDeleteDialogBtn)
    await t.click(this.deleteDailyNoteBtn)
  }
}
