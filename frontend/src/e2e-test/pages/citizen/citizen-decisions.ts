// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'

export default class CitizenDecisionsPage {
  constructor(private readonly page: Page) {}

  #decisionResponseButton = (applicationId: string) =>
    this.page
      .findAll(`[data-qa="button-confirm-decisions-${applicationId}"]`)
      .first()

  async assertUnresolvedDecisionsCount(count: number) {
    return assertUnresolvedDecisionsCount(this.page, count)
  }

  async assertApplicationDecision(
    childId: string,
    decisionId: string,
    expectedTitle: string,
    expectedSentDate: string,
    expectedStatus: string
  ) {
    const decision = this.page
      .findByDataQa(`child-decisions-${childId}`)
      .findByDataQa(`application-decision-${decisionId}`)
    await decision
      .findByDataQa('title-decision-type')
      .assertTextEquals(expectedTitle)
    await decision
      .findByDataQa('decision-sent-date')
      .assertTextEquals(expectedSentDate)
    await decision
      .findByDataQa('decision-status')
      .assertText((text) => text.toLowerCase() === expectedStatus.toLowerCase())
  }

  async assertFinanceDecisionShown(
    decisionId: string,
    expectedTitle: string,
    expectedSentAt: string,
    expectedCoDebtorNames: string[],
    expectedChildNames?: string[]
  ) {
    const financeDecision = this.page.findByDataQa(
      `finance-decision-${decisionId}`
    )
    await financeDecision
      .findByDataQa(`finance-decision-title`)
      .assertTextEquals(expectedTitle)
    await financeDecision
      .findByDataQa(`finance-decision-sent-at`)
      .assertTextEquals(expectedSentAt)
    for (const coDebtor of expectedCoDebtorNames) {
      await financeDecision
        .findByDataQa(`finance-decision-co-debtors`)
        .assertText((text) => text.includes(coDebtor))
    }

    if (expectedChildNames) {
      for (const name of expectedChildNames) {
        await financeDecision
          .findByDataQa(`finance-decision-children`)
          .assertText((text) => text.includes(name))
      }
    }
  }

  async assertFinanceDecisionNotShown(decisionId: string) {
    await this.page
      .findByDataQa(`finance-decision-${decisionId}`)
      .waitUntilHidden()
  }

  async navigateToDecisionResponse(applicationId: string) {
    await this.#decisionResponseButton(applicationId).click()
    const responsePage = new CitizenDecisionResponsePage(this.page)
    await responsePage.assertPageTitle()
    return responsePage
  }

  async openAssistanceDecisionCollapsible(childId: string, decisionId: string) {
    const decision = this.page
      .findByDataQa(`child-decisions-${childId}`)
      .findByDataQa(`assistance-decision-${decisionId}`)

    if ((await decision.getAttribute('data-status')) === 'closed') {
      await decision.click()
    }
  }

  async openAssistanceDecision(childId: string, decisionId: string) {
    await this.openAssistanceDecisionCollapsible(childId, decisionId)
    await this.page
      .findByDataQa(`child-decisions-${childId}`)
      .findByDataQa(`assistance-decision-${decisionId}`)
      .findByDataQa('open-decision')
      .click()
  }

  async assertAssistanceDecision(
    childId: string,
    decisionId: string,
    contents: {
      assistanceLevel: string
      selectedUnit: string
      validityPeriod: string
      decisionMade: string
      status: string
      annulmentReason?: string
    }
  ) {
    await this.openAssistanceDecisionCollapsible(childId, decisionId)
    const decision = this.page
      .findByDataQa(`child-decisions-${childId}`)
      .findByDataQa(`assistance-decision-${decisionId}`)
    await decision
      .findByDataQa('assistance-level')
      .assertTextEquals(contents.assistanceLevel)
    await decision
      .findByDataQa('selected-unit')
      .assertTextEquals(contents.selectedUnit)
    await decision
      .findByDataQa('validity-period')
      .assertTextEquals(contents.validityPeriod)
    await decision
      .findByDataQa('decision-made')
      .assertTextEquals(contents.decisionMade)
    await decision
      .findByDataQa('decision-status')
      .assertTextEquals(contents.status)
    if (contents.annulmentReason !== undefined) {
      await decision
        .findByDataQa('annulment-reason')
        .assertTextEquals(contents.annulmentReason)
    }
  }

  async assertAssistancePreschoolDecision(
    childId: string,
    decisionId: string,
    contents: {
      type: string
      selectedUnit: string
      validityPeriod: string
      decisionMade: string
      status: string
      annulmentReason?: string
    }
  ) {
    await this.openAssistanceDecisionCollapsible(childId, decisionId)
    const decision = this.page
      .findByDataQa(`child-decisions-${childId}`)
      .findByDataQa(`assistance-decision-${decisionId}`)
    await decision.findByDataQa('type').assertTextEquals(contents.type)
    await decision
      .findByDataQa('selected-unit')
      .assertTextEquals(contents.selectedUnit)
    await decision
      .findByDataQa('validity-period')
      .assertTextEquals(contents.validityPeriod)
    await decision
      .findByDataQa('decision-made')
      .assertTextEquals(contents.decisionMade)
    await decision
      .findByDataQa('decision-status')
      .assertTextEquals(contents.status)
    if (contents.annulmentReason !== undefined) {
      await decision
        .findByDataQa('annulment-reason')
        .assertTextEquals(contents.annulmentReason)
    }
  }

  async assertChildDecisionCount(n: number, childId: string) {
    await this.page
      .findByDataQa(`child-decisions-${childId}`)
      .findAll('section')
      .assertCount(n)
  }

  async assertUnreadAssistanceNeedDecisions(childId: string, count: number) {
    await waitUntilEqual(
      () =>
        this.page
          .findByDataQa(`child-decisions-${childId}`)
          .findAllByDataQa('count-indicator')
          .count(),
      count
    )
  }

  async assertNoChildDecisions(childId: string) {
    await this.page.findByDataQa('decisions-page').waitUntilVisible()
    await this.page.findByDataQa(`child-decisions-${childId}`).waitUntilHidden()
  }
}

class CitizenDecisionResponsePage {
  constructor(private readonly page: Page) {}

  #title = this.page.find('h1')
  #decisionBlock = (decisionId: string) =>
    this.page.find(`[data-qa="decision-${decisionId}"]`)
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
    await this.#title.assertTextEquals('Päätökset')
  }

  async assertUnresolvedDecisionsCount(count: number) {
    return assertUnresolvedDecisionsCount(this.page, count)
  }

  async assertDecisionCannotBeAccepted(decisionId: string) {
    await this.#submitResponseButton(decisionId).assertAttributeEquals(
      'disabled',
      ''
    )
  }

  async assertDecisionData(
    decisionId: string,
    decisionTypeText: string,
    decisionUnitText: string,
    decisionStatusText: string
  ) {
    await this.#decisionTitle(decisionId).assertTextEquals(decisionTypeText)
    await this.#decisionUnit(decisionId).assertTextEquals(decisionUnitText)
    await this.assertDecisionStatus(decisionId, decisionStatusText)
  }

  async assertDecisionStatus(decisionId: string, statusText: string) {
    await waitUntilEqual(
      async () => (await this.#decisionStatus(decisionId).text).toLowerCase(),
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
    await this.page
      .find('[data-qa="cascade-warning-modal"] [data-qa="modal-okBtn"]')
      .click()
  }
}

async function assertUnresolvedDecisionsCount(page: Page, count: number) {
  const element = page.find('[data-qa="alert-box-unconfirmed-decisions-count"]')

  if (count === 0) {
    return element.waitUntilHidden()
  }

  if (count === 1) {
    return await element.assertTextEquals('1 päätös odottaa vahvistustasi')
  }

  return await element.assertTextEquals(
    `${count} päätöstä odottaa vahvistustasi`
  )
}
