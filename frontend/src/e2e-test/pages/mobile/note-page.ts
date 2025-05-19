// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ChildDailyNoteBody,
  ChildDailyNoteLevel,
  ChildDailyNoteReminder
} from 'lib-common/generated/api-types/note'
import type LocalDate from 'lib-common/local-date'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import type { Page, Element, ElementCollection } from '../../utils/page'
import { Modal, TextInput } from '../../utils/page'

export default class MobileNotePage {
  #createNoteButton: Element
  #deleteNoteButton: Element
  #stickyNote: {
    note: ElementCollection
    newNoteBtn: Element
    editBtn: ElementCollection
    removeBtn: ElementCollection
    saveBtn: Element
    input: TextInput
  }
  #note: {
    dailyNote: TextInput
    sleepingTimeHours: TextInput
    sleepingTimeMinutes: TextInput
    reminderNote: TextInput
    feedingNote: (level: ChildDailyNoteLevel) => Element
    sleepingNote: (level: ChildDailyNoteLevel) => Element
    reminders: (reminder: ChildDailyNoteReminder) => Element
  }
  constructor(private readonly page: Page) {
    this.#createNoteButton = page.findByDataQa('create-daily-note-btn')
    this.#deleteNoteButton = page.findByDataQa('open-delete-dialog-btn')
    this.#stickyNote = {
      note: page.findAll('[data-qa="sticky-note"]'),
      newNoteBtn: page.findByDataQa('sticky-note-new'),
      editBtn: page.findAll('[data-qa="sticky-note-edit"]'),
      removeBtn: page.findAll('[data-qa="sticky-note-remove"]'),
      saveBtn: page.findByDataQa('sticky-note-save'),
      input: new TextInput(page.findByDataQa('sticky-note-input'))
    }
    this.#note = {
      dailyNote: new TextInput(page.findByDataQa('daily-note-note-input')),
      sleepingTimeHours: new TextInput(
        page.findByDataQa('sleeping-time-hours-input')
      ),
      sleepingTimeMinutes: new TextInput(
        page.findByDataQa('sleeping-time-minutes-input')
      ),
      reminderNote: new TextInput(page.findByDataQa('reminder-note-input')),
      feedingNote: (level: ChildDailyNoteLevel) =>
        page.findByDataQa(`feeding-note-${level}`),
      sleepingNote: (level: ChildDailyNoteLevel) =>
        page.findByDataQa(`sleeping-note-${level}`),
      reminders: (reminder: ChildDailyNoteReminder) =>
        page.findByDataQa(`reminders-${reminder}`)
    }
  }

  async selectTab(tab: 'note' | 'group' | 'sticky') {
    await this.page.findByDataQa(`tab-${tab.toUpperCase()}`).click()
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
    await new Modal(this.page.findByDataQa('modal')).submit()
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
