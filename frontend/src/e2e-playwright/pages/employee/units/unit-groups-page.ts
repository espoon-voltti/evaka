// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  DatePickerDeprecated,
  Element,
  Modal,
  Page,
  TextInput
} from '../../../utils/page'
import { waitUntilDefined, waitUntilEqual, waitUntilTrue } from '../../../utils'
import { UnitDiaryPage } from './unit-diary-page'

export class UnitGroupsPage {
  constructor(private readonly page: Page) {}

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-groups-page"][data-loading="false"]')
      .waitUntilVisible()
  }

  #groupCollapsible = (groupId: string) =>
    this.page.find(`[data-qa="daycare-group-collapsible-${groupId}"]`)

  async selectPeriod(period: '1 day' | '3 months' | '6 months' | '1 year') {
    await this.page
      .find(`[data-qa="unit-filter-period-${period.replace(' ', '-')}"]`)
      .click()
    await this.waitUntilLoaded()
  }

  async setFilterStartDate(date: string) {
    await new DatePickerDeprecated(
      this.page.find('[data-qa="unit-filter-start-date"]')
    ).fill(date)
    await this.waitUntilLoaded()
  }

  terminatedPlacementsSection = new TerminatedPlacementsSection(
    this.page,
    this.page.find('[data-qa="terminated-placements-section"]')
  )

  missingPlacementsSection = new MissingPlacementsSection(
    this.page,
    this.page.find('[data-qa="missing-placements-section"]')
  )

  async assertChildCapacityFactor(childId: string, factor: string) {
    await waitUntilEqual(
      () =>
        this.page.find(`[data-qa="child-capacity-factor-${childId}"]`)
          .innerText,
      factor
    )
  }

  readonly childCapacityFactorColumnHeading = this.page.find(
    `[data-qa="child-capacity-factor-heading"]`
  )

  readonly childCapacityFactorColumnData = this.page.findAll(
    `[data-qa="child-capacity-factor-column"]`
  )

  async openGroupCollapsible(groupId: string) {
    const elem = this.#groupCollapsible(groupId)
    const state = await waitUntilDefined(() => elem.getAttribute('data-status'))
    if (state === 'closed') {
      await elem.find('[data-qa="group-name"]').click()
    }
    return new GroupCollapsible(this.page, elem)
  }

  async assertGroupCollapsibleIsOpen(groupId: string) {
    await this.#groupCollapsible(groupId)
      .find('[data-qa="group-name"]')
      .waitUntilVisible()
  }

  async waitUntilVisible() {
    await this.page.find('[data-qa="groups-title-bar"]').waitUntilVisible()
  }

  async assertChildOccupancyFactorColumnNotVisible() {
    await waitUntilEqual(() => this.childCapacityFactorColumnData.count(), 0)
  }
}

export class TerminatedPlacementsSection extends Element {
  constructor(private page: Page, self: Element) {
    super(self)
  }

  readonly #terminatedPlacementRows = this.page.findAll(
    '[data-qa="terminated-placement-row"]'
  )

  async assertRowCount(expectedCount: number) {
    await waitUntilEqual(
      () => this.#terminatedPlacementRows.count(),
      expectedCount
    )
  }
}

export class MissingPlacementsSection extends Element {
  constructor(private page: Page, self: Element) {
    super(self)
  }

  #missingPlacementRows = this.findAll('[data-qa="missing-placement-row"]')

  async assertRowCount(expectedCount: number) {
    await waitUntilEqual(
      () => this.#missingPlacementRows.count(),
      expectedCount
    )
  }

  async createGroupPlacementForChild(n: number) {
    const missingPlacementRow = new MissingPlacementRow(
      this.page,
      this.#missingPlacementRows.nth(n)
    )
    const modal = await missingPlacementRow.addToGroup()
    await modal.submit()
  }

  async assertRowFields(
    nth: number,
    fields: {
      childName?: string
      dateOfBirth?: string
      placementDuration?: string
      groupMissingDuration?: string
    }
  ) {
    const missingPlacementRow = new MissingPlacementRow(
      this.page,
      this.#missingPlacementRows.nth(nth)
    )
    await missingPlacementRow.assertFields(fields)
  }
}

export class MissingPlacementRow extends Element {
  constructor(private page: Page, self: Element) {
    super(self)
  }

  #childName = this.find('[data-qa="child-name"]')
  #dateOfBirth = this.find('[data-qa="child-dob"]')
  #placementDuration = this.find('[data-qa="placement-duration"]')
  #groupMissingDuration = this.find('[data-qa="group-missing-duration"]')

  async assertFields(fields: {
    childName?: string
    dateOfBirth?: string
    placementDuration?: string
    groupMissingDuration?: string
  }) {
    if (fields.childName !== undefined) {
      await waitUntilEqual(() => this.#childName.innerText, fields.childName)
    }
    if (fields.dateOfBirth !== undefined) {
      await waitUntilEqual(
        () => this.#dateOfBirth.innerText,
        fields.dateOfBirth
      )
    }
    if (fields.placementDuration !== undefined) {
      await waitUntilEqual(
        () => this.#placementDuration.innerText,
        fields.placementDuration
      )
    }
    if (fields.groupMissingDuration !== undefined) {
      await waitUntilEqual(
        () => this.#groupMissingDuration.innerText,
        fields.groupMissingDuration
      )
    }
  }

  #addToGroup = this.find('[data-qa="add-to-group-btn"]')

  async addToGroup() {
    await this.#addToGroup.click()
    return new CreateGroupPlacementModal(
      this.page.find('[data-qa="group-placement-modal"]')
    )
  }
}

export class CreateGroupPlacementModal extends Modal {}

export class GroupCollapsible extends Element {
  constructor(private page: Page, self: Element) {
    super(self)
  }

  #diaryButton = this.find('[data-qa="open-absence-diary-button"]')
  #groupDailyNoteButton = this.find('[data-qa="btn-create-group-note"]')

  #childRows = this.find('[data-qa="table-of-group-placements"]').findAll(
    '[data-qa^="group-placement-row-"]'
  )
  #noChildren = this.find('[data-qa="no-children-placeholder"]')

  childRow(childId: string) {
    return new GroupCollapsibleChildRow(this, childId)
  }

  async assertChildCount(expectedCount: number) {
    if (expectedCount === 0) {
      await this.#noChildren.waitUntilVisible()
    } else {
      await waitUntilEqual(() => this.#childRows.count(), expectedCount)
    }
  }

  async removeGroupPlacement(n: number) {
    await this.#childRows.nth(n).find('[data-qa="remove-btn"]').click()
  }

  async openDiary() {
    await this.#diaryButton.click()
    return new UnitDiaryPage(this.page)
  }

  async openGroupDailyNoteModal() {
    await this.#groupDailyNoteButton.click()
    return new GroupDailyNoteModal(this.find('[data-qa="modal"]'))
  }
}

export class GroupDailyNoteModal extends Modal {
  #input = new TextInput(this.find('[data-qa="sticky-note-input"]'))
  #save = this.find('[data-qa="sticky-note-save"]')
  #delete = this.find('[data-qa="sticky-note-remove"]')

  async fillNote(text: string) {
    await this.#input.fill(text)
  }

  async save() {
    await this.#save.click()
    await this.#save.waitUntilHidden()
  }

  async deleteNote() {
    await this.#delete.click()
    await this.#delete.waitUntilHidden()
  }
}

export class GroupCollapsibleChildRow extends Element {
  constructor(self: Element, private childId: string) {
    super(self)
  }

  #childName = this.find('[data-qa="child-name"]')
  #placementDuration = this.find('[data-qa="placement-duration"]')

  async assertFields(fields: {
    childName?: string
    placementDuration?: string
  }) {
    if (fields.childName !== undefined) {
      await waitUntilEqual(() => this.#childName.innerText, fields.childName)
    }
    if (fields.placementDuration !== undefined) {
      await waitUntilEqual(
        () => this.#placementDuration.innerText,
        fields.placementDuration
      )
    }
  }

  #dailyNoteIcon = this.find(
    `[data-qa="daycare-daily-note-icon-${this.childId}"]`
  )
  #dailyNoteTooltip = this.find(
    `[data-qa="daycare-daily-note-hover-${this.childId}"]`
  )

  async assertDailyNoteContainsText(expectedText: string) {
    await this.#dailyNoteIcon.hover()
    await waitUntilTrue(async () =>
      ((await this.#dailyNoteTooltip.textContent) ?? '').includes(expectedText)
    )
  }

  async openDailyNoteModal() {
    await this.#dailyNoteIcon.click()
    return new ChildDailyNoteModal(this.find('[data-qa="modal"]'))
  }

  #removeButton = this.find('[data-qa="remove-btn"]')

  async remove() {
    await this.#removeButton.click()
  }
}

export class ChildDailyNoteModal extends Modal {
  #noteInput = new TextInput(this.find('[data-qa="note-input"]'))
  #sleepingHoursInput = new TextInput(
    this.find('[data-qa="sleeping-hours-input"]')
  )
  #sleepingMinutesInput = new TextInput(
    this.find('[data-qa="sleeping-minutes-input"]')
  )
  #reminderNoteInput = new TextInput(
    this.find('[data-qa="reminder-note-input"]')
  )
  #submit = this.find('[data-qa="btn-submit"]')

  async openTab(tab: 'child' | 'sticky' | 'group') {
    await this.find(`[data-qa="tab-${tab}"]`).click()
  }

  // Child
  async fillNote(text: string) {
    await this.#noteInput.fill(text)
  }

  async assertNote(expectedText: string) {
    await waitUntilEqual(() => this.#noteInput.inputValue, expectedText)
  }

  async assertSleepingHours(expectedText: string) {
    await waitUntilEqual(
      () => this.#sleepingHoursInput.inputValue,
      expectedText
    )
  }

  async assertSleepingMinutes(expectedText: string) {
    await waitUntilEqual(
      () => this.#sleepingMinutesInput.inputValue,
      expectedText
    )
  }

  async assertReminderNote(expectedText: string) {
    await waitUntilEqual(() => this.#reminderNoteInput.inputValue, expectedText)
  }

  async submit() {
    await this.#submit.click()
  }

  // Group
  #groupNote = this.find('[data-qa="sticky-note-note"]')
  #groupNoteInput = this.find('[data-qa="sticky-note"]')

  async assertGroupNote(expectedText: string) {
    await waitUntilEqual(() => this.#groupNote.textContent, expectedText)
  }

  async assertNoGroupNote() {
    await this.#groupNoteInput.waitUntilVisible()
  }
}
