// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { waitUntilEqual } from '../../utils'
import {
  ChildDailyNoteBody,
  ChildDailyNoteLevel,
  ChildDailyNoteReminder,
  GroupNoteBody
} from 'lib-common/generated/api-types/messaging'

export default class MobileNotePage {
  constructor(private readonly page: Page) {}

  #createNoteButton = this.page.locator('[data-qa="create-daily-note-btn"]')
  #groupTab = this.page.locator('[data-qa="tab-group-note"]')

  #note = {
    dailyNote: this.page.locator('[data-qa="daily-note-note-input"]'),
    groupNote: this.page.locator('[data-qa="daily-note-group-note-input"]'),
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

  async selectGroupTab() {
    await this.#groupTab.click()
  }

  async fillGroupNote(groupNote: GroupNoteBody) {
    if (groupNote.note) await this.#note.groupNote.type(groupNote.note)
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

  async saveNote() {
    await this.#createNoteButton.click()
  }
  async assertGroupNote(expected: GroupNoteBody) {
    await waitUntilEqual(
      () => this.#note.groupNote.inputValue(),
      expected.note || ''
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
