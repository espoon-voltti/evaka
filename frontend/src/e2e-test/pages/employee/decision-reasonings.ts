// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page } from '../../utils/page'
import { DatePicker, Element, TextInput } from '../../utils/page'

export class DecisionReasoningsPage {
  daycareTab: Element
  preschoolTab: Element

  // Generic section
  addGenericButton: Element
  genericCards: Element
  genericValidFrom: DatePicker
  genericTextFi: TextInput
  genericTextSv: TextInput
  genericCancelButton: Element
  genericSaveAsNotReadyButton: Element
  genericSaveAndActivateButton: Element

  // Individual section
  addIndividualButton: Element
  individualCards: Element
  individualTitleFi: TextInput
  individualTitleSv: TextInput
  individualTextFi: TextInput
  individualTextSv: TextInput
  individualCancelButton: Element
  individualSaveAndActivateButton: Element

  constructor(private readonly page: Page) {
    this.daycareTab = page.findByDataQa('DAYCARE-tab')
    this.preschoolTab = page.findByDataQa('PRESCHOOL-tab')

    // Generic section
    this.addGenericButton = page.findByDataQa('add-generic-reasoning-button')
    this.genericCards = page.findByDataQa('generic-reasoning-card')
    this.genericValidFrom = new DatePicker(
      page.findByDataQa('generic-reasoning-valid-from').locator
    )
    this.genericTextFi = new TextInput(
      page.findByDataQa('generic-reasoning-text-fi').locator
    )
    this.genericTextSv = new TextInput(
      page.findByDataQa('generic-reasoning-text-sv').locator
    )
    this.genericCancelButton = page.findByDataQa(
      'cancel-generic-reasoning-button'
    )
    this.genericSaveAsNotReadyButton = page.findByDataQa(
      'save-as-not-ready-button'
    )
    this.genericSaveAndActivateButton = page.findByDataQa(
      'save-and-activate-button'
    )

    // Individual section
    this.addIndividualButton = page.findByDataQa(
      'add-individual-reasoning-button'
    )
    this.individualCards = page.findByDataQa('individual-reasoning-card')
    this.individualTitleFi = new TextInput(
      page.findByDataQa('individual-reasoning-title-fi').locator
    )
    this.individualTitleSv = new TextInput(
      page.findByDataQa('individual-reasoning-title-sv').locator
    )
    this.individualTextFi = new TextInput(
      page.findByDataQa('individual-reasoning-text-fi').locator
    )
    this.individualTextSv = new TextInput(
      page.findByDataQa('individual-reasoning-text-sv').locator
    )
    this.individualCancelButton = page.findByDataQa(
      'cancel-individual-reasoning-button'
    )
    this.individualSaveAndActivateButton = page.findByDataQa(
      'save-and-activate-button'
    )
  }

  genericCard(index: number): GenericReasoningCard {
    return new GenericReasoningCard(
      this.page.findAllByDataQa('generic-reasoning-card').nth(index).locator
    )
  }

  individualCard(index: number): IndividualReasoningCard {
    return new IndividualReasoningCard(
      this.page.findAllByDataQa('individual-reasoning-card').nth(index).locator
    )
  }

  async confirmModal() {
    await this.page.findByDataQa('modal-okBtn').click()
  }

  toggleOutdatedGeneric(): Element {
    return this.page.findByDataQa('toggle-outdated-reasonings')
  }

  toggleRemovedIndividual(): Element {
    return this.page.findByDataQa('toggle-removed-reasonings')
  }
}

class GenericReasoningCard extends Element {
  get status(): Element {
    return this.findByDataQa('generic-reasoning-status')
  }

  get editButton(): Element {
    return this.findByDataQa('edit-generic-reasoning-button')
  }

  get deleteButton(): Element {
    return this.findByDataQa('delete-generic-reasoning-button')
  }
}

class IndividualReasoningCard extends Element {
  get status(): Element {
    return this.findByDataQa('individual-reasoning-status')
  }

  get removeButton(): Element {
    return this.findByDataQa('remove-individual-reasoning-button')
  }
}
