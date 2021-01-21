// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export default class CitizenDecisionsPage {
  readonly unresolvedDecisionsInfoBox = Selector(
    '[data-qa="alert-box-unconfirmed-decisions-count"]'
  )

  readonly applicationDecisions = (applicationId: string) =>
    Selector(`[data-qa="application-${applicationId}"]`)

  readonly decision = (decisionId: string) =>
    Selector(`[data-qa="decision-${decisionId}"]`)

  readonly decisionsChildName = (applicationId: string) =>
    Selector(`[data-qa="title-decision-child-name-${applicationId}"]`)

  readonly decisionType = (decisionId: string) =>
    Selector(`[data-qa="title-decision-type-${decisionId}"]`)

  readonly decisionSentDate = (decisionId: string) =>
    Selector(`[data-qa="decision-sent-date-${decisionId}"]`)

  readonly decisionResolvedDate = (decisionId: string) =>
    Selector(`[data-qa="decision-resolved-date-${decisionId}"]`)

  readonly decisionStatus = (decisionId: string) =>
    Selector(`[data-qa="decision-status-${decisionId}"]`)

  readonly goRespondToDecisionBtn = (applicationId: string) =>
    Selector(`[data-qa="button-confirm-decisions-${applicationId}"]`)

  async assertApplicationDecision(
    applicationId: string,
    decisionId: string,
    expectedChildName: string,
    expectedType: string,
    expectedSentDate: string,
    expectedStatus: string
  ) {
    await t
      .expect(this.decisionsChildName(applicationId).textContent)
      .eql(expectedChildName)
    await t.expect(this.decisionType(decisionId).textContent).eql(expectedType)
    await t
      .expect(this.decisionSentDate(decisionId).textContent)
      .eql(expectedSentDate)
    await t
      .expect(this.decisionStatus(decisionId).textContent)
      .eql(expectedStatus)
  }
}
