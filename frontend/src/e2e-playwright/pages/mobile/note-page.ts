// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDailyNoteBody,
  ChildDailyNoteLevel,
  ChildDailyNoteReminder
} from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'
import { Page } from 'playwright'
import { waitUntilEqual, waitUntilTrue } from '../../utils'

export default class MobileNotePage {
  constructor(private readonly page: Page) {}

  #stickyNote = {
    note: this.page.locator('[data-qa="sticky-note"]'),
    newNoteBtn: this.page.locator('[data-qa="sticky-note-new"]'),
    editBtn: this.page.locator('[data-qa="sticky-note-edit"]'),
    removeBtn: this.page.locator('[data-qa="sticky-note-remove"]'),
    saveBtn: this.page.locator('[data-qa="sticky-note-save"]'),
    input: this.page.locator('[data-qa="sticky-note-input"]')
  }

  #createNoteButton = this.page.locator('[data-qa="create-daily-note-btn"]')
  #note = {
    dailyNote: this.page.locator('[data-qa="daily-note-note-input"]'),
    sleepingTimeHours: this.page.locator(
      '[data-qa="sleeping-time-hours-input"]'
    ),
    sleepingTimeMinutes: this.page.locator(
      '[data-qa="sleeping-time-minutes-input"]'
    ),
    reminderNote: this.page.locator('[data-qa="reminder-note-input"]'),
    feedingNote: (level: ChildDailyNoteLevel) =>
      this.page.locator(`[data-qa="feeding-note-${level}"]`),
    sleepingNote: (level: ChildDailyNoteLevel) =>
      this.page.locator(`[data-qa="sleeping-note-${level}"]`),
    reminders: (reminder: ChildDailyNoteReminder) =>
      this.page.locator(`[data-qa="reminders-${reminder}"]`)
  }

  async selectTab(tab: 'note' | 'group' | 'sticky') {
    await this.page.locator(`[data-qa="tab-${tab.toUpperCase()}"]`).click()
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
    await this.#stickyNote.input.selectText()
    await this.#stickyNote.input.type(text)
    await this.#stickyNote.saveBtn.click()
  }

  async removeStickyNote(nth = 0) {
    await this.#stickyNote.removeBtn.nth(nth).click()
  }

  async assertStickyNote(expected: string, nth = 0) {
    await waitUntilEqual(
      () => this.#stickyNote.note.nth(nth).locator('p').textContent(),
      expected
    )
  }
  async assertStickyNoteExpires(date: LocalDate, nth = 0) {
    await waitUntilTrue(() =>
      this.#stickyNote.note
        .nth(nth)
        .locator('[data-qa="sticky-note-expires"]')
        .textContent()
        .then((t) => !!t?.includes(date.format()))
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

  async assertNote(expected: ChildDailyNoteBody) {
    await waitUntilEqual(
      () => this.#note.dailyNote.inputValue(),
      expected.note || ''
    )
    await waitUntilEqual(
      () => this.#note.reminderNote.inputValue(),
      expected.reminderNote || ''
    )

    await waitUntilEqual(async () => {
      const hours = Number(
        (await this.#note.sleepingTimeHours.inputValue()) || 0
      )
      const minutes = Number(
        (await this.#note.sleepingTimeMinutes.inputValue()) || 0
      )
      return (hours * 60 + minutes).toString()
    }, expected.sleepingMinutes?.toString() || '0')
  }
}
