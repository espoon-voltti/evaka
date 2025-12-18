// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

import type { Page, ElementCollection } from '../../../utils/page'
import { Combobox, DatePicker } from '../../../utils/page'
import { AsyncButton } from '../../../utils/page'
import { Element } from '../../../utils/page'

export default class PlacementDesktopView {
  applicationCards: ElementCollection
  daycareCards: ElementCollection
  occupancyPeriodStartPicker: DatePicker
  occupancyPeriodEnd: Element

  constructor(private page: Page) {
    this.applicationCards = this.page.findAllByDataQa('application-card')
    this.daycareCards = this.page.findAllByDataQa('daycare-card')
    this.occupancyPeriodStartPicker = new DatePicker(
      this.page.findByDataQa('occupancy-period-start-picker')
    )
    this.occupancyPeriodEnd = this.page.findByDataQa('occupancy-period-end')
  }

  applicationCard(n: number) {
    const element = this.applicationCards.nth(n)
    return new ApplicationCard(element)
  }

  daycareCard(n: number) {
    const element = this.daycareCards.nth(n)
    return new DaycareCard(element)
  }
}

export class ApplicationCard extends Element {
  childName: Element
  dueDate: Element
  preferredStartDate: Element
  unitPreferences: ElementCollection
  addOtherUnitButton: Element
  draftPlacementCombobox: Combobox
  toPlacementPlanButton: Element
  constructor(private root: Element) {
    super(root)
    this.childName = this.root.findByDataQa('child-name')
    this.dueDate = this.root.findByDataQa('due-date')
    this.preferredStartDate = this.root.findByDataQa('preferred-start-date')
    this.unitPreferences = this.root.findAllByDataQa('unit-preference')
    this.toPlacementPlanButton = this.root.findByDataQa(
      'to-placement-plan-button'
    )
    this.addOtherUnitButton = this.root.findByDataQa('add-other-unit-button')
    this.draftPlacementCombobox = new Combobox(
      this.root.findByDataQa('draft-placement-combobox')
    )
  }

  unitPreference(n: number) {
    const element = this.unitPreferences.nth(n)
    return new UnitPreference(element)
  }
}

export class UnitPreference extends Element {
  title: Element
  createPlacementDraftButton: AsyncButton
  cancelPlacementDraftButton: AsyncButton
  showUnitButton: Element
  placementDate: Element
  editPlacementDateButton: Element
  placementDatePicker: DatePicker
  savePlacementDateButton: AsyncButton
  constructor(private root: Element) {
    super(root)
    this.title = this.root.findByDataQa('unit-preference-title')
    this.createPlacementDraftButton = new AsyncButton(
      this.root.findByDataQa('create-placement-draft-button')
    )
    this.cancelPlacementDraftButton = new AsyncButton(
      this.root.findByDataQa('cancel-placement-draft-button')
    )
    this.showUnitButton = this.root.findByDataQa('show-unit-button')
    this.placementDate = this.root.findByDataQa('placement-date')
    this.editPlacementDateButton = this.root.findByDataQa(
      'edit-placement-date-button'
    )
    this.placementDatePicker = new DatePicker(
      this.root.findByDataQa('placement-date-picker')
    )
    this.savePlacementDateButton = new AsyncButton(
      this.root.findByDataQa('save-placement-date-button')
    )
  }

  async assertPlacementDate(date: LocalDate) {
    await this.placementDate.assertTextEquals(`${date.format()} –`)
  }
}

export class DaycareCard extends Element {
  name: Element
  hideUnitButton: Element
  occupancyConfirmed: Element
  occupancyPlanned: Element
  occupancyDraft: Element
  draftPlacementRows: ElementCollection
  constructor(private root: Element) {
    super(root)
    this.name = this.root.findByDataQa('unit-name')
    this.hideUnitButton = this.root.findByDataQa('hide-unit-button')
    this.occupancyConfirmed = this.root.findByDataQa('occupancy-confirmed')
    this.occupancyPlanned = this.root.findByDataQa('occupancy-planned')
    this.occupancyDraft = this.root.findByDataQa('occupancy-draft')
    this.draftPlacementRows = this.root.findAllByDataQa('draft-placement-row')
  }

  drawPlacementRow(n: number) {
    const element = this.draftPlacementRows.nth(n)
    return new DraftPlacementRow(element)
  }

  async assertOccupancies(confirmed: number, planned: number, draft: number) {
    await this.occupancyConfirmed.assertTextEquals(`${confirmed} %`)
    await this.occupancyPlanned.assertTextEquals(`${planned} %`)
    await this.occupancyDraft.assertTextEquals(`${draft} %`)
  }
}

export class DraftPlacementRow extends Element {
  childName: Element
  placementDate: Element
  cancelPlacementDraftButton: AsyncButton
  constructor(private root: Element) {
    super(root)
    this.childName = this.root.findByDataQa('child-name')
    this.placementDate = this.root.findByDataQa('placement-date')
    this.cancelPlacementDraftButton = new AsyncButton(
      this.root.findByDataQa('cancel-placement-draft-button')
    )
  }

  async assertPlacementDate(date: LocalDate) {
    await this.placementDate.assertTextEquals(`${date.format()} –`)
  }
}
