// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, ElementCollection } from '../../utils/page'
import { DatePicker, Element, TextInput } from '../../utils/page'

export class DecisionReasoningsPage {
  daycareTab: Element
  preschoolTab: Element

  // Generic section
  addGenericButton: Element
  genericCards: ElementCollection
  genericValidFrom: DatePicker
  genericTextFi: TextInput
  genericTextSv: TextInput
  genericCancelButton: Element
  genericSaveAsNotReadyButton: Element
  genericSaveAndActivateButton: Element
  toggleOutdatedGeneric: Element

  // Individual section
  addIndividualButton: Element
  individualCards: ElementCollection
  individualTitleFi: TextInput
  individualTitleSv: TextInput
  individualTextFi: TextInput
  individualTextSv: TextInput
  individualCancelButton: Element
  individualSaveAndActivateButton: Element
  toggleRemovedIndividual: Element

  modalOkButton: Element

  constructor(private readonly page: Page) {
    this.daycareTab = page.findByDataQa('DAYCARE-tab')
    this.preschoolTab = page.findByDataQa('PRESCHOOL-tab')
    this.modalOkButton = page.findByDataQa('modal-okBtn')

    // Generic section
    this.addGenericButton = page.findByDataQa('add-generic-reasoning-button')
    this.genericCards = page.findAllByDataQa('generic-reasoning-card')
    this.toggleOutdatedGeneric = page.findByDataQa('toggle-outdated-reasonings')
    this.genericValidFrom = new DatePicker(
      page.findByDataQa('generic-reasoning-valid-from')
    )
    this.genericTextFi = new TextInput(
      page.findByDataQa('generic-reasoning-text-fi')
    )
    this.genericTextSv = new TextInput(
      page.findByDataQa('generic-reasoning-text-sv')
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
    this.individualCards = page.findAllByDataQa('individual-reasoning-card')
    this.toggleRemovedIndividual = page.findByDataQa(
      'toggle-removed-reasonings'
    )
    this.individualTitleFi = new TextInput(
      page.findByDataQa('individual-reasoning-title-fi')
    )
    this.individualTitleSv = new TextInput(
      page.findByDataQa('individual-reasoning-title-sv')
    )
    this.individualTextFi = new TextInput(
      page.findByDataQa('individual-reasoning-text-fi')
    )
    this.individualTextSv = new TextInput(
      page.findByDataQa('individual-reasoning-text-sv')
    )
    this.individualCancelButton = page.findByDataQa(
      'cancel-individual-reasoning-button'
    )
    this.individualSaveAndActivateButton = page.findByDataQa(
      'save-and-activate-button'
    )
  }

  genericCard(index: number): GenericReasoningCard {
    return new GenericReasoningCard(this.genericCards.nth(index))
  }

  individualCard(index: number): IndividualReasoningCard {
    return new IndividualReasoningCard(this.individualCards.nth(index))
  }

  async confirmModal() {
    await this.modalOkButton.click()
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
