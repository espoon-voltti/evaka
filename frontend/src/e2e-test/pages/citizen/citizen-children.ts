// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { Page, TextInput } from '../../utils/page'

export class CitizenChildrenPage {
  constructor(private readonly page: Page) {}

  #childRows = this.page.findAllByDataQa('child')
  #childName = this.page.findByDataQa('child-name')

  async assertChildCount(count: number) {
    await this.#childRows.assertCount(count)
  }

  async navigateToChild(nth: number) {
    await this.#childRows.nth(nth).click()
    await this.#childName.waitUntilVisible()
  }

  async openChildPage(childName: string) {
    await this.#childRows.find(`h2:has-text("${childName}")`).click()
    await this.page.find(`h1:has-text("${childName}")`).waitUntilVisible()
  }
}

export class CitizenChildPage {
  constructor(private readonly page: Page) {}
  #placements = this.page.findAllByDataQa('placement')
  #terminatedPlacements = this.page.findAllByDataQa('terminated-placement')

  async assertChildNameIsShown(name: string) {
    await this.page.find(`h1:has-text("${name}")`).waitUntilVisible()
  }

  async goBack() {
    await this.page.findText('Palaa').click()
  }

  async openTerminationCollapsible() {
    await this.page.findText('Paikan irtisanominen').click()
  }

  async openAssistanceNeedCollapsible() {
    await this.page.findText('Tuen tarve').click()
  }

  async assertTerminatablePlacementCount(count: number) {
    await this.#placements.assertCount(count)
  }

  async assertNonTerminatablePlacementCount(count: number) {
    await this.page
      .findAllByDataQa('non-terminatable-placement')
      .assertCount(count)
  }

  async assertTerminatedPlacementCount(count: number) {
    await this.#terminatedPlacements.assertCount(count)
  }

  getTerminatedPlacements(): Promise<string[]> {
    return this.#terminatedPlacements.allInnerTexts()
  }

  async togglePlacement(label: string) {
    await this.page.findText(label).click()
  }

  async fillTerminationDate(date: LocalDate, nth = 0) {
    await new TextInput(
      this.page.findAllByDataQa('termination-date').nth(nth)
    ).fill(date.format())
  }

  async submitTermination(nth = 0) {
    await this.page.findAll('text=Irtisano paikka').nth(nth).click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  getTerminatablePlacements(): Promise<string[]> {
    return this.#placements.allInnerTexts()
  }

  getNonTerminatablePlacements(): Promise<string[]> {
    return this.page
      .findAllByDataQa('non-terminatable-placement')
      .allInnerTexts()
  }

  getToggledPlacements(): Promise<string[]> {
    return this.#placements.evaluateAll((elems) =>
      elems
        .filter((e) => !!e.querySelector('input:checked'))
        .map((e) => e.textContent ?? '')
    )
  }

  async getAssistanceNeedDecisionRow(nth: number) {
    const row = this.page
      .findAllByDataQa('assistance-need-decision-row')
      .nth(nth)

    return {
      assistanceLevel: await row.findByDataQa('assistance-level').innerText,
      validityPeriod: await row.findByDataQa('validity-period').innerText,
      selectedUnit: await row.findByDataQa('selected-unit').innerText,
      decisionMade: await row.findByDataQa('decision-made').innerText,
      status: await row.findByDataQa('status').getAttribute('data-qa-status')
    }
  }

  async getAssistanceNeedDecisionRowCount() {
    return this.page.findAllByDataQa('assistance-need-decision-row').count()
  }

  async getAssistanceNeedDecisionRowClick(nth: number) {
    return this.page
      .findAllByDataQa('assistance-need-decision-row')
      .nth(nth)
      .click()
  }
}
