// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from 'e2e-playwright/utils/page'
import LocalDate from 'lib-common/local-date'

export class CitizenChildrenPage {
  constructor(private readonly page: Page) {}

  #childRows = this.page.findAllByDataQa('child')

  async assertChildCount(count: number) {
    await this.#childRows.assertCount(count)
  }

  async navigateToChild(nth: number) {
    await this.#childRows.nth(nth).click()
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
    await this.page.find(`text=Palaa`).click()
  }

  async openTerminationCollapsible() {
    await this.page.find(`text=Paikan irtisanominen`).click()
  }

  async assertTerminatablePlacementCount(count: number) {
    await this.#placements.assertCount(count)
  }

  async assertTerminatedPlacementCount(count: number) {
    await this.#terminatedPlacements.assertCount(count)
  }

  getTerminatedPlacements(): Promise<string[]> {
    return this.#terminatedPlacements.allInnerTexts()
  }

  async togglePlacement(label: string) {
    await this.page.find(`text=${label}`).click()
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

  getToggledPlacements(): Promise<string[]> {
    return this.#placements.evaluateAll((elems) =>
      elems
        .filter((e) => !!e.querySelector('input:checked'))
        .map((e) => e.textContent ?? '')
    )
  }
}
