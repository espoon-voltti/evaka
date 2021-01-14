// SPDX-FileCopyrightText: 2017-2020 City of Espoo
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
}
