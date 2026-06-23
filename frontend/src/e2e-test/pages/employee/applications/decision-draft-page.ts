// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DecisionType } from 'lib-common/generated/api-types/decision'
import type { UUID } from 'lib-common/types'

import { expect } from '../../../playwright'
import type { Element, ElementCollection, Page } from '../../../utils/page'
import { Checkbox, Combobox } from '../../../utils/page'

export class DecisionDraftPage {
  constructor(private page: Page) {}

  async waitUntilLoaded() {
    await expect(this.page.findByDataQa('save-decisions-button')).toBeVisible()
  }

  async selectSharedUnit(unitId: UUID) {
    const selector = new Combobox(
      this.page.findByDataQa('header-unit-selector')
    )
    await selector.fillAndSelectItem('', unitId)
  }

  plannedCheckbox(decisionType: DecisionType): Checkbox {
    return new Checkbox(this.page.findByDataQa(`planned-${decisionType}`))
  }

  decisionCard(type: DecisionType): DecisionCardSection {
    return new DecisionCardSection(
      this.page.findByDataQa(`decision-card-${type}`)
    )
  }

  async openPicker(type: DecisionType): Promise<IndividualReasoningPicker> {
    await this.page.findByDataQa(`open-picker-${type}`).click()
    return new IndividualReasoningPicker(this.page)
  }

  async save() {
    await this.page.findByDataQa('save-decisions-button').click()
  }

  async cancel() {
    await this.page.findByDataQa('cancel-decisions-button').click()
  }
}

export class DecisionCardSection {
  constructor(private root: Element) {}

  get unitLanguageUnsupportedWarning(): Element {
    return this.root.findByDataQa('unit-language-unsupported-warning')
  }

  pickerButton(decisionType: DecisionType): Element {
    return this.root.findByDataQa(`open-picker-${decisionType}`)
  }

  genericReasoning(collectionType: 'DAYCARE' | 'PRESCHOOL'): Element {
    return this.root.findByDataQa(`generic-card-${collectionType}`)
  }

  individualReasoning(reasoningId: UUID): Element {
    return this.root.findByDataQa(`individual-card-${reasoningId}`)
  }

  individualReasonings(): ElementCollection {
    return this.root.findAll('[data-qa^="individual-card-"]')
  }
}

export class IndividualReasoningPicker {
  constructor(private page: Page) {}

  reasoningRow(reasoningId: UUID): Element {
    return this.page.findByDataQa(`reasoning-row-${reasoningId}`)
  }

  reasoningRows(): ElementCollection {
    return this.page.findAll('[data-qa^="reasoning-row-"]')
  }

  async selectReasoning(reasoningId: UUID) {
    await this.reasoningRow(reasoningId).click()
  }

  async deselectReasoning(reasoningId: UUID) {
    await this.reasoningRow(reasoningId).click()
  }

  async close() {
    await this.page.findByDataQa('modal-closeBtn').click()
  }
}
