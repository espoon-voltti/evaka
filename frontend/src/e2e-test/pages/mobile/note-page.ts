// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDailyNoteBody,
  ChildDailyNoteLevel,
  ChildDailyNoteReminder
} from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Modal, Page, TextInput } from '../../utils/page'

export default class MobileNotePage {
  constructor(private readonly page: Page) {}

  #stickyNote = {
    note: this.page.findAll('[data-qa="sticky-note"]'),
    newNoteBtn: this.page.find('[data-qa="sticky-note-new"]'),
    editBtn: this.page.findAll('[data-qa="sticky-note-edit"]'),
    removeBtn: this.page.findAll('[data-qa="sticky-note-remove"]'),
    saveBtn: this.page.find('[data-qa="sticky-note-save"]'),
    input: new TextInput(this.page.find('[data-qa="sticky-note-input"]'))
  }

  #createNoteButton = this.page.find('[data-qa="create-daily-note-btn"]')
  #deleteNoteButton = this.page.find('[data-qa="open-delete-dialog-btn"]')
  #note = {
    dailyNote: new TextInput(
      this.page.find('[data-qa="daily-note-note-input"]')
    ),
    sleepingTimeHours: new TextInput(
      this.page.find('[data-qa="sleeping-time-hours-input"]')
    ),
    sleepingTimeMinutes: new TextInput(
      this.page.find('[data-qa="sleeping-time-minutes-input"]')
    ),
    reminderNote: new TextInput(
      this.page.find('[data-qa="reminder-note-input"]')
    ),
    feedingNote: (level: ChildDailyNoteLevel) =>
      this.page.find(`[data-qa="feeding-note-${level}"]`),
    sleepingNote: (level: ChildDailyNoteLevel) =>
      this.page.find(`[data-qa="sleeping-note-${level}"]`),
    reminders: (reminder: ChildDailyNoteReminder) =>
      this.page.find(`[data-qa="reminders-${reminder}"]`)
  }

  async selectTab(tab: 'note' | 'group' | 'sticky') {
    await this.page.find(`[data-qa="tab-${tab.toUpperCase()}"]`).click()
  }

  async initNewStickyNote() {
    await this.#stickyNote.newNoteBtn.click()
  }

  async typeAndSaveStickyNote(note: string) {
    await this.#stickyNote.input.type(note)
    await this.#stickyNote.saveBtn.click()
  }

  async editStickyNote(text: string, nth: number) {
    await this.#stickyNote.editBtn.nth(nth).click()
    await this.#stickyNote.input.fill(text)
    await this.#stickyNote.saveBtn.click()
  }

  async removeStickyNote(nth = 0) {
    await this.#stickyNote.removeBtn.nth(nth).click()
  }

  async assertStickyNote(expected: string, nth = 0) {
    await this.#stickyNote.note.nth(nth).find('p').assertTextEquals(expected)
  }

  async assertStickyNoteExpires(date: LocalDate, nth = 0) {
    await waitUntilTrue(() =>
      this.#stickyNote.note
        .nth(nth)
        .find('[data-qa="sticky-note-expires"]')
        .text.then((t) => !!t?.includes(date.format()))
    )
  }

  async fillNote(dailyNote: ChildDailyNoteBody) {
    if (dailyNote.note) await this.#note.dailyNote.type(dailyNote.note)
    if (dailyNote.feedingNote)
      await this.#note.feedingNote(dailyNote.feedingNote).click()
    if (dailyNote.sleepingNote)
      await this.#note.sleepingNote(dailyNote.sleepingNote).click()
    if (dailyNote.reminderNote)
      await this.#note.reminderNote.type(dailyNote.reminderNote)
    for (const reminder of dailyNote.reminders) {
      await this.#note.reminders(reminder).click()
    }
    if (Number(dailyNote.sleepingMinutes) > 0) {
      await this.#note.sleepingTimeHours.type(
        Math.floor(Number(dailyNote.sleepingMinutes) / 60).toString()
      )
      await this.#note.sleepingTimeMinutes.type(
        (Number(dailyNote.sleepingMinutes) % 60).toString()
      )
    }
  }

  async saveChildDailyNote() {
    await this.#createNoteButton.click()
  }

  async deleteChildDailyNote() {
    await this.#deleteNoteButton.click()
    await new Modal(this.page.find('[data-qa="modal"]')).submit()
  }

  async assertNote(expected: ChildDailyNoteBody) {
    await waitUntilEqual(
      () => this.#note.dailyNote.inputValue,
      expected.note || ''
    )
    await waitUntilEqual(
      () => this.#note.reminderNote.inputValue,
      expected.reminderNote || ''
    )

    await waitUntilEqual(async () => {
      const hours = Number((await this.#note.sleepingTimeHours.inputValue) || 0)
      const minutes = Number(
        (await this.#note.sleepingTimeMinutes.inputValue) || 0
      )
      return (hours * 60 + minutes).toString()
    }, expected.sleepingMinutes?.toString() || '0')
  }
}
