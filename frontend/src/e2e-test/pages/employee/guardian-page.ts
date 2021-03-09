// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export default class GuardianPage {
  public readonly lastName = Selector('[data-qa="person-last-name"]')

  public readonly firstNames = Selector('[data-qa="person-first-names"]')

  public readonly ssn = Selector('[data-qa="person-ssn"]')

  public readonly dependantsCollapsible = Selector(
    '[data-qa="person-dependants-collapsible"]'
  )

  public readonly applicationsCollapsible = Selector(
    '[data-qa="person-applications-collapsible"]'
  )

  public readonly decisionsCollapsible = Selector(
    '[data-qa="person-decisions-collapsible"]'
  )

  public readonly invoicesCollapsible = Selector(
    '[data-qa="person-invoices-collapsible"]'
  )

  public readonly applicationSummaries = Selector(
    '[data-qa="table-of-applications"]'
  )

  public readonly dependantChildren = Selector(
    '[data-qa="table-of-dependants"]'
  )

  public readonly decisions = Selector('[data-qa="table-of-decisions"]')
  public readonly invoices = Selector('[data-qa="table-of-invoices"]')

  public async containsApplicationSummary(
    childNameAndSsn: string,
    unitName: string
  ) {
    await t.click(this.applicationsCollapsible)
    await t
      .expect(
        this.applicationSummaries.withText(childNameAndSsn).withText(unitName)
          .exists
      )
      .ok()
  }

  public async containsDependantChild(childName: string) {
    await t.click(this.dependantsCollapsible)
    await t.expect(this.dependantChildren.withText(childName).exists).ok()
  }

  public async containsDecisionForChild(
    childName: string,
    unitName: string,
    status: string
  ) {
    await t.click(this.decisionsCollapsible)
    await t.expect(this.decisions.visible).ok()
    await t
      .expect(
        this.decisions.withText(childName).withText(unitName).withText(status)
          .exists
      )
      .ok()
  }

  public async containsInvoice(
    startDate: string,
    endDate: string,
    status: string
  ) {
    await t.click(this.invoicesCollapsible)
    await t.expect(this.invoices.visible).ok()
    await t
      .expect(
        this.invoices.withText(startDate).withText(endDate).withText(status)
          .exists
      )
      .ok()
  }

  public async assertPersonInfo(
    lastName: string,
    firstNames: string,
    ssn: string
  ) {
    await this.assertText(this.lastName, lastName)
    await this.assertText(this.firstNames, firstNames)
    await this.assertText(this.ssn, ssn)
  }

  async assertText(selector: Selector, expectedText: string) {
    await t.expect(selector.withText(expectedText).exists).ok()
  }
}
