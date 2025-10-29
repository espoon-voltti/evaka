// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, ElementCollection } from '../../../utils/page'
import { AsyncButton } from '../../../utils/page'
import { Element } from '../../../utils/page'

export default class PlacementDesktopView {
  applicationCards: ElementCollection
  daycareCards: ElementCollection

  constructor(private page: Page) {
    this.applicationCards = this.page.findAllByDataQa('application-card')
    this.daycareCards = this.page.findAllByDataQa('daycare-card')
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
  unitPreferences: ElementCollection
  toPlacementPlanButton: Element
  constructor(private root: Element) {
    super(root)
    this.childName = this.root.findByDataQa('child-name')
    this.unitPreferences = this.root.findAllByDataQa('unit-preference')
    this.toPlacementPlanButton = this.root.findByDataQa(
      'to-placement-plan-button'
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
  }
}

export class DaycareCard extends Element {
  name: Element
  occupancyConfirmed: Element
  occupancyPlanned: Element
  occupancyDraft: Element
  draftPlacementRows: ElementCollection
  constructor(private root: Element) {
    super(root)
    this.name = this.root.findByDataQa('unit-name')
    this.occupancyConfirmed = this.root.findByDataQa('occupancy-confirmed')
    this.occupancyPlanned = this.root.findByDataQa('occupancy-planned')
    this.occupancyDraft = this.root.findByDataQa('occupancy-draft')
    this.draftPlacementRows = this.root.findAllByDataQa('draft-placement-row')
  }

  async assertOccupancies(confirmed: number, planned: number, draft: number) {
    await this.occupancyConfirmed.assertTextEquals(`${confirmed} %`)
    await this.occupancyPlanned.assertTextEquals(`${planned} %`)
    await this.occupancyDraft.assertTextEquals(`${draft} %`)
  }
}
