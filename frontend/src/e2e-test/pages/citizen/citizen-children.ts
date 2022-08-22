// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { waitUntilEqual } from '../../utils'
import { Page, Radio, TextInput } from '../../utils/page'

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

  async assertChildUnreadCount(childId: string, count: number) {
    await waitUntilEqual(
      () =>
        this.page
          .find(`[data-qa="child"][data-qa-value="${childId}"]`)
          .findByDataQa('unread-count').innerText,
      count.toString()
    )
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

  async openCollapsible(
    collapsible:
      | 'termination'
      | 'assistance-need-decisions'
      | 'consents'
      | 'pedagogical-documents'
      | 'vasu'
  ) {
    await this.page.findByDataQa(`collapsible-${collapsible}`).click()
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
      status: await row
        .findAllByDataQa('status')
        .first()
        .getAttribute('data-qa-status')
    }
  }

  async getAssistanceNeedDecisionRowCount() {
    return this.page.findAllByDataQa('assistance-need-decision-row').count()
  }

  async assistanceNeedDecisionRowClick(nth: number) {
    return this.page
      .findAllByDataQa('assistance-need-decision-row')
      .nth(nth)
      .click()
  }

  async assertUnreadAssistanceNeedDecisions(count: number) {
    await waitUntilEqual(
      () =>
        this.page
          .findByDataQa('collapsible-assistance-need-decisions')
          .findByDataQa('count-indicator').innerText,
      count.toString()
    )
  }

  async assertAssistanceNeedDecisionRowUnread(nth: number) {
    await this.page
      .findAllByDataQa('assistance-need-decision-row')
      .nth(nth)
      .findByDataQa('unopened-indicator')
      .waitUntilVisible()
  }

  async assertNotAssistanceNeedDecisionRowUnread(nth: number) {
    await this.page
      .findAllByDataQa('assistance-need-decision-row')
      .nth(nth)
      .findByDataQa('unopened-indicator')
      .waitUntilHidden()
  }

  readonly evakaProfilePicYes = new Radio(
    this.page.findByDataQa('consent-profilepic-yes')
  )
  readonly evakaProfilePicNo = new Radio(
    this.page.findByDataQa('consent-profilepic-no')
  )

  #consentConfirmButton = this.page.findByDataQa('consent-confirm')

  async saveConsent() {
    await this.#consentConfirmButton.click()
  }

  async assertUnconsentedCount(count: number) {
    await waitUntilEqual(
      () =>
        this.page
          .findByDataQa('collapsible-consents')
          .findByDataQa('count-indicator').innerText,
      count.toString()
    )
  }

  readonly #vasuRowStateChip = (vasuId: string) =>
    this.page.find(`[data-qa="state-chip-${vasuId}"] >> visible=true`)
  readonly #vasuRowPublishedAt = (vasuId: string) =>
    this.page.find(`[data-qa="published-at-${vasuId}"] >> visible=true`)
  readonly #vasuChildContainer = this.page.findAll(
    `[data-qa="vasu-child-container"] >> visible=true`
  )

  async assertVasuRow(
    vasuId: string,
    expectedStatus: string,
    expectedPublishedAt: string
  ) {
    await waitUntilEqual(
      () => this.#vasuRowStateChip(vasuId).textContent,
      expectedStatus
    )
    await waitUntilEqual(
      () => this.#vasuRowPublishedAt(vasuId).textContent,
      expectedPublishedAt
    )
  }

  async assertVasuChildCount(expectedCount: number) {
    await waitUntilEqual(() => this.#vasuChildContainer.count(), expectedCount)
  }
}
