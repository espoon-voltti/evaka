// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export default class CitizenDecisionResponsePage {
  readonly pageTitle = Selector('h1')
  readonly unresolvedDecisionsInfoBox = Selector(
    '[data-qa="alert-box-unconfirmed-decisions-count"]'
  )
  readonly decisionBlock = (decisionId: string) =>
    Selector(`[data-qa="decision-${decisionId}"]`)
  readonly acceptRadioBtn = (decisionId: string) =>
    this.decisionBlock(decisionId).find('[data-qa="radio-accept"]')
  readonly rejectRadioBtn = (decisionId: string) =>
    this.decisionBlock(decisionId).find('[data-qa="radio-reject"]')
  readonly submitResponseBtn = (decisionId: string) =>
    this.decisionBlock(decisionId).find('[data-qa="submit-response"]')
  readonly decisionTitle = (decisionId: string) =>
    this.decisionBlock(decisionId).find('[data-qa="title-decision-type"]')
  readonly decisionUnit = (decisionId: string) =>
    this.decisionBlock(decisionId).find('[data-qa="decision-unit"]')
  readonly decisionStatus = (decisionId: string) =>
    this.decisionBlock(decisionId).find('[data-qa="decision-status"]')

  async confirmRejectCascade() {
    await t.click(
      Selector('[data-qa="cascade-warning-modal"] [data-qa="modal-okBtn"]')
    )
  }

  async acceptDecision(decisionId: string) {
    await t.click(this.acceptRadioBtn(decisionId))
    await t.click(this.submitResponseBtn(decisionId))
  }

  async rejectDecision(decisionId: string) {
    await t.click(this.rejectRadioBtn(decisionId))
    await t.click(this.submitResponseBtn(decisionId))
  }

  async assertDecisionData(
    decisionId: string,
    decisionTypeText: string,
    decisionUnitText: string,
    decisionStatusText: string
  ) {
    await t
      .expect(this.decisionTitle(decisionId).textContent)
      .eql(decisionTypeText)

    await t
      .expect(this.decisionUnit(decisionId).textContent)
      .eql(decisionUnitText)

    await t
      .expect(this.decisionStatus(decisionId).textContent)
      .eql(decisionStatusText)
  }

  async assertUnresolvedDecisionsNotification(count: number) {
    if (count === 0) {
      await t.expect(this.unresolvedDecisionsInfoBox.exists).notOk()
    } else if (count === 1) {
      await t
        .expect(this.unresolvedDecisionsInfoBox.textContent)
        .contains('1 päätös odottaa vahvistusta')
    } else {
      await t
        .expect(this.unresolvedDecisionsInfoBox.textContent)
        .contains(`${count} päätöstä odottaa vahvistusta`)
    }
  }
}
