// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, ElementCollection } from '../../utils/page'
import { DatePicker, Element, TextInput } from '../../utils/page'

export class DecisionReasoningsPage {
  daycareTab: Element
  preschoolTab: Element
  clubTab: Element

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

  // Individual sections, one per language
  individualFi: IndividualReasoningsSection
  individualSv: IndividualReasoningsSection

  modalOkButton: Element

  constructor(page: Page) {
    this.daycareTab = page.findByDataQa('DAYCARE-tab')
    this.preschoolTab = page.findByDataQa('PRESCHOOL-tab')
    this.clubTab = page.findByDataQa('CLUB-tab')
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

    // Individual sections, one per language
    this.individualFi = new IndividualReasoningsSection(
      page.findByDataQa('individual-reasonings-FI')
    )
    this.individualSv = new IndividualReasoningsSection(
      page.findByDataQa('individual-reasonings-SV')
    )
  }

  genericCard(index: number): GenericReasoningCard {
    return new GenericReasoningCard(this.genericCards.nth(index))
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

  get removeButton(): Element {
    return this.findByDataQa('remove-generic-reasoning-button')
  }
}

export class IndividualReasoningsSection extends Element {
  get addButton(): Element {
    return this.findByDataQa('add-individual-reasoning-button')
  }

  get titleInput(): TextInput {
    return new TextInput(this.findByDataQa('individual-reasoning-title'))
  }

  get textInput(): TextInput {
    return new TextInput(this.findByDataQa('individual-reasoning-text'))
  }

  get cancelButton(): Element {
    return this.findByDataQa('cancel-individual-reasoning-button')
  }

  get saveAndActivateButton(): Element {
    return this.findByDataQa('save-and-activate-button')
  }

  get toggleRemoved(): Element {
    return this.findByDataQa('toggle-removed-reasonings')
  }

  get cards(): ElementCollection {
    return this.findAllByDataQa('individual-reasoning-card')
  }

  card(index: number): IndividualReasoningCard {
    return new IndividualReasoningCard(this.cards.nth(index))
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
