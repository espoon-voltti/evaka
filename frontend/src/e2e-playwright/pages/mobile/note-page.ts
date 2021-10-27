// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDailyNoteBody,
  ChildDailyNoteLevel,
  ChildDailyNoteReminder
} from 'lib-common/generated/api-types/note'
import { Page } from 'playwright'
import { waitUntilEqual } from '../../utils'

export default class MobileNotePage {
  constructor(private readonly page: Page) {}

  #createNoteButton = this.page.locator('[data-qa="create-daily-note-btn"]')

  #note = {
    dailyNote: this.page.locator('[data-qa="daily-note-note-input"]'),
    groupNoteInput: this.page.locator('[data-qa="sticky-note-input"]'),
    stickyNote: this.page.locator('[data-qa="sticky-note"]'),
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

  async fillStickyNote(note: string) {
    await this.#note.groupNoteInput.type(note)
  }

  async saveStickyNote() {
    await this.page.locator(`[data-qa="sticky-note-save"]`).click()
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
  async assertGroupNote(expected: string, nth = 0) {
    await waitUntilEqual(
      () => this.#note.stickyNote.nth(nth).locator('p').textContent(),
      expected
    )
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
