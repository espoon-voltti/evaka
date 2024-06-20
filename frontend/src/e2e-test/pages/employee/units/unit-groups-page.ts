// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilDefined, waitUntilEqual, waitUntilTrue } from '../../../utils'
import {
  DatePicker,
  DatePickerDeprecated,
  Element,
  Modal,
  Page,
  TextInput
} from '../../../utils/page'

import { UnitMonthCalendarPage } from './unit-month-calendar-page'

export class UnitGroupsPage {
  childCapacityFactorColumnHeading: Element
  constructor(private readonly page: Page) {
    this.childCapacityFactorColumnHeading = page.findByDataQa(
      `child-capacity-factor-heading`
    )
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-groups-page"][data-loading="false"]')
      .waitUntilVisible()
  }

  #groupCollapsibles = this.page.findAll(
    `[data-qa^="daycare-group-collapsible-"]`
  )
  #groupCollapsible = (groupId: string) =>
    this.page.findByDataQa(`daycare-group-collapsible-${groupId}`)

  async selectPeriod(period: '1 day' | '3 months' | '6 months' | '1 year') {
    await this.page
      .find(`[data-qa="unit-filter-period-${period.replace(' ', '-')}"]`)
      .click()
    await this.waitUntilLoaded()
  }

  async setFilterStartDate(date: string) {
    await new DatePicker(this.page.findByDataQa('unit-filter-start-date')).fill(
      date
    )
    await this.waitUntilLoaded()
  }

  terminatedPlacementsSection = new TerminatedPlacementsSection(
    this.page,
    this.page.findByDataQa('terminated-placements-section')
  )

  missingPlacementsSection = new MissingPlacementsSection(
    this.page,
    this.page.findByDataQa('missing-placements-section')
  )

  async assertChildCapacityFactor(childId: string, factor: string) {
    await this.page
      .find(`[data-qa="child-capacity-factor-${childId}"]`)
      .assertTextEquals(factor)
  }

  readonly childCapacityFactorColumnData = this.page.findAll(
    `[data-qa="child-capacity-factor-column"]`
  )

  async assertGroupCount(expectedCount: number) {
    await waitUntilEqual(() => this.#groupCollapsibles.count(), expectedCount)
  }

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
    await this.page.findByDataQa('groups-title-bar').waitUntilVisible()
  }

  async assertChildOccupancyFactorColumnNotVisible() {
    await waitUntilEqual(() => this.childCapacityFactorColumnData.count(), 0)
  }
}

export class TerminatedPlacementsSection extends Element {
  constructor(
    private page: Page,
    self: Element
  ) {
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
  constructor(
    private page: Page,
    self: Element
  ) {
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
  constructor(
    private page: Page,
    self: Element
  ) {
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
      await this.#childName.assertTextEquals(fields.childName)
    }
    if (fields.dateOfBirth !== undefined) {
      await this.#dateOfBirth.assertTextEquals(fields.dateOfBirth)
    }
    if (fields.placementDuration !== undefined) {
      await this.#placementDuration.assertTextEquals(fields.placementDuration)
    }
    if (fields.groupMissingDuration !== undefined) {
      await this.#groupMissingDuration.assertTextEquals(
        fields.groupMissingDuration
      )
    }
  }

  #addToGroup = this.find('[data-qa="add-to-group-btn"]')

  async addToGroup() {
    await this.#addToGroup.click()
    return new CreateGroupPlacementModal(
      this.page.findByDataQa('group-placement-modal')
    )
  }
}

export class CreateGroupPlacementModal extends Modal {}

export class GroupCollapsible extends Element {
  constructor(
    private page: Page,
    self: Element
  ) {
    super(self)
  }

  #groupName = this.find('[data-qa="group-name"]')
  #groupStartDate = this.find('[data-qa="group-start-date"]')
  #groupEndDate = this.find('[data-qa="group-end-date"]')

  #monthCalendarButton = this.find('[data-qa="open-month-calendar-button"]')
  #groupDailyNoteButton = this.find('[data-qa="btn-create-group-note"]')

  #childRows = this.find('[data-qa="table-of-group-placements"]').findAll(
    '[data-qa^="group-placement-row-"]'
  )
  #noChildren = this.find('[data-qa="no-children-placeholder"]')

  async assertGroupName(expectedName: string) {
    await this.#groupName.assertTextEquals(expectedName)
  }

  async assertGroupStartDate(expectedStartDate: string) {
    await this.#groupStartDate.assertTextEquals(expectedStartDate)
  }

  async assertGroupEndDate(expectedEndDate: string) {
    await this.#groupEndDate.assertTextEquals(expectedEndDate)
  }

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

  async openMonthCalendar() {
    await this.#monthCalendarButton.click()
    return new UnitMonthCalendarPage(this.page)
  }

  async openGroupDailyNoteModal() {
    await this.#groupDailyNoteButton.click()
    return new GroupDailyNoteModal(this.find('[data-qa="modal"]'))
  }

  #updateButton = this.find('[data-qa="btn-update-group"]')

  async edit(fields: { name: string; startDate: string; endDate: string }) {
    await this.#updateButton.click()

    const modal = new Modal(this.find('[data-qa="group-update-modal"]'))
    await new TextInput(modal.find('[data-qa="name-input"]')).fill(fields.name)
    await new DatePickerDeprecated(
      modal.find('[data-qa="start-date-input"]')
    ).fill(fields.startDate)
    await new DatePickerDeprecated(
      modal.find('[data-qa="end-date-input"]')
    ).fill(fields.endDate)
    await modal.submit()
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
  constructor(
    self: Element,
    private childId: string
  ) {
    super(self)
  }

  #childName = this.find('[data-qa="child-name"]')
  #placementDuration = this.find('[data-qa="placement-duration"]')

  async assertFields(fields: {
    childName?: string
    placementDuration?: string
  }) {
    if (fields.childName !== undefined) {
      await this.#childName.assertTextEquals(fields.childName)
    }
    if (fields.placementDuration !== undefined) {
      await this.#placementDuration.assertTextEquals(fields.placementDuration)
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
      ((await this.#dailyNoteTooltip.text) ?? '').includes(expectedText)
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
  noteInput = new TextInput(this.find('[data-qa="note-input"]'))
  sleepingHoursInput = new TextInput(
    this.find('[data-qa="sleeping-hours-input"]')
  )
  sleepingMinutesInput = new TextInput(
    this.find('[data-qa="sleeping-minutes-input"]')
  )
  reminderNoteInput = new TextInput(
    this.find('[data-qa="reminder-note-input"]')
  )
  submitButton = this.find('[data-qa="btn-submit"]')

  async openTab(tab: 'child' | 'sticky' | 'group') {
    await this.find(`[data-qa="tab-${tab}"]`).click()
  }

  // Group
  #groupNote = this.find('[data-qa="sticky-note-note"]')
  #groupNoteInput = this.find('[data-qa="sticky-note"]')

  async assertGroupNote(expectedText: string) {
    await this.#groupNote.assertTextEquals(expectedText)
  }

  async assertNoGroupNote() {
    await this.#groupNoteInput.waitUntilVisible()
  }
}
