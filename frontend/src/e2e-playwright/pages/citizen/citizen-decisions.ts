// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilFalse } from 'e2e-playwright/utils'
import { RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class CitizenDecisionsPage {
  constructor(private readonly page: Page) {}

  #decisionChildName = (applicationId: string) =>
    new RawElement(
      this.page,
      `[data-qa="title-decision-child-name-${applicationId}"]`
    )
  #decisionType = (decisionId: string) =>
    new RawElement(this.page, `[data-qa="title-decision-type-${decisionId}"]`)
  #decisionSentDate = (decisionId: string) =>
    new RawElement(this.page, `[data-qa="decision-sent-date-${decisionId}"]`)
  #decisionStatus = (decisionId: string) =>
    new RawElement(this.page, `[data-qa="decision-status-${decisionId}"]`)
  #decisionResponseButton = (applicationId: string) =>
    new RawElement(
      this.page,
      `[data-qa="button-confirm-decisions-${applicationId}"]`
    )

  async assertUnresolvedDecisionsCount(count: number) {
    return assertUnresolvedDecisionsCount(this.page, count)
  }

  async assertApplicationDecision(
    applicationId: string,
    decisionId: string,
    expectedChildName: string,
    expectedType: string,
    expectedSentDate: string,
    expectedStatus: string
  ) {
    await waitUntilEqual(
      () => this.#decisionChildName(applicationId).innerText,
      expectedChildName
    )
    await waitUntilEqual(
      () => this.#decisionType(decisionId).innerText,
      expectedType
    )
    await waitUntilEqual(
      () => this.#decisionSentDate(decisionId).innerText,
      expectedSentDate
    )
    await waitUntilEqual(
      async () =>
        (await this.#decisionStatus(decisionId).innerText).toLowerCase(),
      expectedStatus.toLowerCase()
    )
  }

  async navigateToDecisionResponse(applicationId: string) {
    await this.#decisionResponseButton(applicationId).click()
    const responsePage = new CitizenDecisionResponsePage(this.page)
    await responsePage.assertPageTitle()
    return responsePage
  }
}

class CitizenDecisionResponsePage {
  constructor(private readonly page: Page) {}

  #title = new RawElement(this.page, 'h1')
  #decisionBlock = (decisionId: string) =>
    new RawElement(this.page, `[data-qa="decision-${decisionId}"]`)
  #acceptRadioButton = (decisionId: string) =>
    this.#decisionBlock(decisionId).find('[data-qa="radio-accept"]')
  #rejectRadioButton = (decisionId: string) =>
    this.#decisionBlock(decisionId).find('[data-qa="radio-reject"]')
  #submitResponseButton = (decisionId: string) =>
    this.#decisionBlock(decisionId).find('[data-qa="submit-response"]')
  #decisionTitle = (decisionId: string) =>
    this.#decisionBlock(decisionId).find('[data-qa="title-decision-type"]')
  #decisionUnit = (decisionId: string) =>
    this.#decisionBlock(decisionId).find('[data-qa="decision-unit"]')
  #decisionStatus = (decisionId: string) =>
    this.#decisionBlock(decisionId).find('[data-qa="decision-status"]')

  async assertPageTitle() {
    await waitUntilEqual(() => this.#title.innerText, 'Päätökset')
  }

  async assertUnresolvedDecisionsCount(count: number) {
    return assertUnresolvedDecisionsCount(this.page, count)
  }

  async assertDecisionCannotBeAccepted(decisionId: string) {
    await waitUntilEqual(
      () => this.#submitResponseButton(decisionId).getAttribute('disabled'),
      ''
    )
  }

  async assertDecisionData(
    decisionId: string,
    decisionTypeText: string,
    decisionUnitText: string,
    decisionStatusText: string
  ) {
    await waitUntilEqual(
      () => this.#decisionTitle(decisionId).innerText,
      decisionTypeText
    )
    await waitUntilEqual(
      () => this.#decisionUnit(decisionId).innerText,
      decisionUnitText
    )
    await this.assertDecisionStatus(decisionId, decisionStatusText)
  }

  async assertDecisionStatus(decisionId: string, statusText: string) {
    await waitUntilEqual(
      async () =>
        (await this.#decisionStatus(decisionId).innerText).toLowerCase(),
      statusText.toLowerCase()
    )
  }

  async acceptDecision(decisionId: string) {
    await this.#acceptRadioButton(decisionId).click()
    await this.#submitResponseButton(decisionId).click()
  }

  async rejectDecision(decisionId: string) {
    await this.#rejectRadioButton(decisionId).click()
    await this.#submitResponseButton(decisionId).click()
  }

  async confirmRejectCascade() {
    await new RawElement(
      this.page,
      '[data-qa="cascade-warning-modal"] [data-qa="modal-okBtn"]'
    ).click()
  }
}

async function assertUnresolvedDecisionsCount(page: Page, count: number) {
  const element = new RawElement(
    page,
    '[data-qa="alert-box-unconfirmed-decisions-count"]'
  )

  if (count === 0) {
    return await waitUntilFalse(() => element.visible)
  }

  if (count === 1) {
    return await waitUntilEqual(
      () => element.innerText,
      '1 päätös odottaa vahvistustasi'
    )
  }

  return await waitUntilEqual(
    () => element.innerText,
    `${count} päätöstä odottaa vahvistustasi`
  )
}
