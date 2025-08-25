// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilDefined, waitUntilEqual, waitUntilTrue } from '../../../utils'
import type { ElementCollection, Page } from '../../../utils/page'
import {
  Combobox,
  DatePicker,
  Element,
  Modal,
  MultiSelect,
  TextInput
} from '../../../utils/page'

import { UnitMonthCalendarPage } from './unit-month-calendar-page'

export class UnitGroupsPage {
  childCapacityFactorColumnHeading: Element
  #groupCollapsibles: ElementCollection
  terminatedPlacementsSection: TerminatedPlacementsSection
  missingPlacementsSection: MissingPlacementsSection
  childCapacityFactorColumnData: ElementCollection

  constructor(private readonly page: Page) {
    this.childCapacityFactorColumnHeading = page.findByDataQa(
      `child-capacity-factor-heading`
    )
    this.#groupCollapsibles = page.findAll(
      `[data-qa^="daycare-group-collapsible-"]`
    )
    this.terminatedPlacementsSection = new TerminatedPlacementsSection(
      page,
      page.findByDataQa('terminated-placements-section')
    )
    this.missingPlacementsSection = new MissingPlacementsSection(
      page,
      page.findByDataQa('missing-placements-section')
    )
    this.childCapacityFactorColumnData = page.findAllByDataQa(
      `child-capacity-factor-column`
    )
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-groups-page"][data-loading="false"]')
      .waitUntilVisible()
  }

  #groupCollapsible = (groupId: string) =>
    this.page.findByDataQa(`daycare-group-collapsible-${groupId}`)

  async selectPeriod(period: '1 day' | '3 months' | '6 months' | '1 year') {
    await this.page
      .findByDataQa(`unit-filter-period-${period.replace(' ', '-')}`)
      .click()
    await this.waitUntilLoaded()
  }

  async setFilterStartDate(date: string) {
    await new DatePicker(this.page.findByDataQa('unit-filter-start-date')).fill(
      date
    )
    await this.waitUntilLoaded()
  }

  async assertChildCapacityFactor(childId: string, factor: string) {
    await this.page
      .findByDataQa(`child-capacity-factor-${childId}`)
      .assertTextEquals(factor)
  }

  async assertGroupCount(expectedCount: number) {
    await waitUntilEqual(() => this.#groupCollapsibles.count(), expectedCount)
  }

  async openGroupCollapsible(groupId: string) {
    const elem = this.#groupCollapsible(groupId)
    const state = await waitUntilDefined(() => elem.getAttribute('data-status'))
    if (state === 'closed') {
      await elem.findByDataQa('group-name').click()
    }
    return new GroupCollapsible(this.page, elem)
  }

  async assertGroupCollapsibleIsOpen(groupId: string) {
    await this.#groupCollapsible(groupId)
      .findByDataQa('group-name')
      .waitUntilVisible()
  }

  async assertGroupCollapsibleHasNekkuOrderButton(groupId: string) {
    await this.#groupCollapsible(groupId)
      .findByDataQa(`btn-nekku-order`)
      .waitUntilVisible()
  }

  async assertGroupCollapsibleNotHasNekkuOrderButton(groupId: string) {
    await this.#groupCollapsible(groupId)
      .findByDataQa(`btn-nekku-order`)
      .waitUntilHidden()
  }

  async openNekkuOrderModal(groupId: string) {
    await this.#groupCollapsible(groupId)
      .findByDataQa(`btn-nekku-order`)
      .click()

    await this.page.findByDataQa('nekku-order-modal').waitUntilVisible()
    return new NekkuOrderModal(this.page.findByDataQa('nekku-order-modal'))
  }

  async waitUntilVisible() {
    await this.page.findByDataQa('groups-title-bar').waitUntilVisible()
  }

  async assertChildOccupancyFactorColumnNotVisible() {
    await waitUntilEqual(() => this.childCapacityFactorColumnData.count(), 0)
  }
}

export class TerminatedPlacementsSection extends Element {
  #terminatedPlacementRows: ElementCollection

  constructor(page: Page, self: Element) {
    super(self)
    this.#terminatedPlacementRows = page.findAllByDataQa(
      'terminated-placement-row'
    )
  }

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

  #missingPlacementRows = this.findAllByDataQa('missing-placement-row')

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

  #childName = this.findByDataQa('child-name')
  #dateOfBirth = this.findByDataQa('child-dob')
  #placementDuration = this.findByDataQa('placement-duration')
  #groupMissingDuration = this.findByDataQa('group-missing-duration')

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

  #addToGroup = this.findByDataQa('add-to-group-btn')

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

  #groupName = this.findByDataQa('group-name')
  #groupStartDate = this.findByDataQa('group-start-date')
  #groupEndDate = this.findByDataQa('group-end-date')

  #monthCalendarButton = this.findByDataQa('open-month-calendar-button')
  #groupDailyNoteButton = this.findByDataQa('btn-create-group-note')
  createChildDocumentsButton = this.findByDataQa('btn-create-child-documents')

  #childRows = this.findByDataQa('table-of-group-placements').findAll(
    '[data-qa^="group-placement-row-"]'
  )
  #noChildren = this.findByDataQa('no-children-placeholder')

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
    return new GroupCollapsibleChildRow(this.page, this, childId)
  }

  async assertChildCount(expectedCount: number) {
    if (expectedCount === 0) {
      await this.#noChildren.waitUntilVisible()
    } else {
      await waitUntilEqual(() => this.#childRows.count(), expectedCount)
    }
  }

  async openMonthCalendar() {
    await this.#monthCalendarButton.click()
    return new UnitMonthCalendarPage(this.page)
  }

  async openGroupDailyNoteModal() {
    await this.#groupDailyNoteButton.click()
    return new GroupDailyNoteModal(this.findByDataQa('modal'))
  }

  async openCreateChildDocumentsModal() {
    await this.createChildDocumentsButton.click()
    return new CreateChildDocumentsModal(this.findByDataQa('modal'))
  }

  #updateButton = this.findByDataQa('btn-update-group')

  async edit(fields: { name: string; startDate: string; endDate: string }) {
    await this.#updateButton.click()

    const modal = new Modal(this.findByDataQa('group-update-modal'))
    await new TextInput(modal.findByDataQa('name-input')).fill(fields.name)
    await new DatePicker(modal.findByDataQa('start-date-input')).fill(
      fields.startDate
    )
    await new DatePicker(modal.findByDataQa('end-date-input')).fill(
      fields.endDate
    )
    await modal.submit()
  }
}

export class GroupDailyNoteModal extends Modal {
  #input = new TextInput(this.findByDataQa('sticky-note-input'))
  #save = this.findByDataQa('sticky-note-save')
  #delete = this.findByDataQa('sticky-note-remove')

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

export class CreateChildDocumentsModal extends Modal {
  templateSelect: Combobox
  childrenSelect: MultiSelect

  constructor(self: Element) {
    super(self)
    this.templateSelect = new Combobox(
      self.findByDataQa('create-child-documents-modal-select-template')
    )
    this.childrenSelect = new MultiSelect(
      self.findByDataQa('create-child-documents-modal-select-children')
    )
  }
}

export class GroupCollapsibleChildRow extends Element {
  #dailyNoteIcon: Element
  #dailyNoteTooltip: Element

  constructor(
    private page: Page,
    self: Element,
    childId: string
  ) {
    super(self)
    this.#dailyNoteIcon = this.findByDataQa(
      `daycare-daily-note-icon-${childId}`
    )

    this.#dailyNoteTooltip = this.page.findByDataQa(
      `daycare-daily-note-hover-${childId}`
    )
  }

  #childName = this.findByDataQa('child-name')
  #placementDuration = this.findByDataQa('placement-duration')

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

  async assertDailyNoteContainsText(expectedText: string) {
    await this.#dailyNoteIcon.hover()
    await waitUntilTrue(async () =>
      ((await this.#dailyNoteTooltip.text) ?? '').includes(expectedText)
    )
  }

  async openDailyNoteModal() {
    await this.#dailyNoteIcon.click()
    return new ChildDailyNoteModal(this.findByDataQa('modal'))
  }

  #removeButton = this.findByDataQa('remove-btn')

  async remove() {
    await this.#removeButton.click()
  }
}

export class ChildDailyNoteModal extends Modal {
  noteInput = new TextInput(this.findByDataQa('note-input'))
  sleepingHoursInput = new TextInput(this.findByDataQa('sleeping-hours-input'))
  sleepingMinutesInput = new TextInput(
    this.findByDataQa('sleeping-minutes-input')
  )
  reminderNoteInput = new TextInput(this.findByDataQa('reminder-note-input'))
  submitButton = this.findByDataQa('btn-submit')

  async openTab(tab: 'child' | 'sticky' | 'group') {
    await this.findByDataQa(`tab-${tab}`).click()
  }

  // Group
  #groupNote = this.findByDataQa('sticky-note-note')
  #groupNoteInput = this.findByDataQa('sticky-note')

  async assertGroupNote(expectedText: string) {
    await this.#groupNote.assertTextEquals(expectedText)
  }

  async assertNoGroupNote() {
    await this.#groupNoteInput.waitUntilVisible()
  }
}

export class NekkuOrderModal extends Modal {
  datePicker = new DatePicker(this.findByDataQa('input-order-date'))
  okButton = this.findByDataQa('modal-okBtn')
}
