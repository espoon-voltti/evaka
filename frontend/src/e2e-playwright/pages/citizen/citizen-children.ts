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
    await this.page.findAllByDataQa('placement').assertCount(count)
  }

  async assertTerminatedPlacementCount(count: number) {
    await this.page.findAllByDataQa('terminated-placement').assertCount(count)
  }

  async assertTerminatedPlacement(label: string) {
    await this.page.find(`text=${label}`).waitUntilVisible()
  }

  async togglePlacement(label: string) {
    await this.page.find(`text=${label}`).click()
  }

  async fillTerminationDate(date: LocalDate) {
    await new TextInput(this.page.findByDataQa('termination-date')).fill(
      date.format()
    )
  }

  async submitTermination() {
    await this.page.find('text=Irtisano paikka').click()
  }
}
