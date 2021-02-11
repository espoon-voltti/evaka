// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { formatCents } from '@evaka/employee-frontend/src/utils/money'
import { selectFirstOption, waitUntilScrolled } from '../../../utils/helpers'

interface VerifyFamilyPersonOpts {
  personId: string
  /**
   * If set, verify person's age in family overview
   */
  age?: number
  /**
   * If set, verify person's total income in family overview
   */
  incomeCents?: number
}

export default class FridgeHeadInformationPage {
  readonly restrictedDetailsEnabledLabel: Selector = Selector(
    '[data-qa="restriction-details-enabled-label"]'
  )

  readonly personStreetAddress: Selector = Selector(
    '[data-qa="person-details-street-address"]'
  )

  readonly partnersCollapsible = Selector(
    '[data-qa="person-partners-collapsible"]'
  )

  readonly childrenCollapsible = Selector(
    '[data-qa="person-children-collapsible"]'
  )
  readonly childrenTableRow = this.childrenCollapsible.find(
    '[data-qa="table-fridge-child-row"]'
  )

  readonly familyOverviewCollapsible = Selector(
    '[data-qa="family-overview-collapsible"]'
  )

  readonly feeDecisionsCollapsible = Selector(
    '[data-qa="person-fee-decisions-collapsible"]'
  )
  readonly feeDecisionTableRow = this.feeDecisionsCollapsible.find('tr')

  readonly incomesCollapsible = Selector(
    '[data-qa="person-income-collapsible"]'
  )

  async openIncomesCollapsible() {
    await this.openCollapsible(this.incomesCollapsible)
  }

  async openCollapsible(collapsibleSelector: Selector) {
    if ((await collapsibleSelector.getAttribute('data-status')) === 'closed') {
      await t.click(collapsibleSelector.find('[data-qa="collapsible-trigger"]'))
    }
  }

  async verifyRestrictedDetails(enabled: boolean) {
    switch (enabled) {
      case true:
        await t.expect(this.restrictedDetailsEnabledLabel.exists).ok()
        await t
          .expect(this.personStreetAddress.innerText)
          .eql('Osoite ei ole saatavilla turvakiellon vuoksi')
        break
      default:
        await t.expect(this.restrictedDetailsEnabledLabel.exists).notOk()
        await t
          .expect(this.personStreetAddress.innerText)
          .notEql('Osoite ei ole saatavilla turvakiellon vuoksi')
    }
  }

  async addPartner({
    searchWord,
    startDate
  }: {
    searchWord: string
    startDate: string
  }) {
    await this.addPerson({
      collapsible: this.partnersCollapsible,
      searchWord,
      startDate
    })
  }

  async addChild({
    searchWord,
    startDate
  }: {
    searchWord: string
    startDate: string
  }) {
    await this.addPerson({
      collapsible: this.childrenCollapsible,
      searchWord,
      startDate
    })
  }

  async addIncome({ mainIncome }: { mainIncome: number }) {
    if (
      (await this.incomesCollapsible.getAttribute('data-status')) === 'closed'
    ) {
      await t.click(this.incomesCollapsible)
    }

    await t.click(this.incomesCollapsible.find('button'))
    await t.typeText(
      this.incomesCollapsible.find('[data-qa="income-input-MAIN_INCOME"]'),
      String(mainIncome)
    )

    await t.click(this.incomesCollapsible.find('[data-qa="save-income"]'))

    await waitUntilScrolled()
  }

  async verifyFridgeChildAge({ age }: { age: number }) {
    if (
      (await this.childrenCollapsible.getAttribute('data-status')) === 'closed'
    ) {
      await t.click(this.childrenCollapsible)
    }

    const childAge = this.childrenTableRow
      .nth(0)
      .find('[data-qa="child-age"]')
      .nth(0)
    await t.expect(childAge.textContent).eql(`${age}`)
  }

  async verifyFamilyPerson({
    personId,
    age,
    incomeCents
  }: VerifyFamilyPersonOpts) {
    if (
      (await this.familyOverviewCollapsible.getAttribute('data-status')) ===
      'closed'
    ) {
      await t.click(this.familyOverviewCollapsible)
    }

    const person = this.familyOverviewCollapsible.find(
      `[data-qa="table-family-overview-row-${personId}"]`
    )

    await t.expect(person.exists).ok()

    if (age !== undefined) {
      const personAge = person.find('[data-qa="person-age"]')
      await t.expect(personAge.textContent).eql(`${age}`)
    }

    if (incomeCents !== undefined) {
      const personIncome = person.find('[data-qa="person-income-total"]')
      const expectedIncome = formatCents(incomeCents)
      if (!expectedIncome) {
        throw new Error(
          'Income argument must be formattable to a non-empty amount'
        )
      }
      await t
        .expect((await personIncome.textContent).split(' ')[0]) // Strip away any currency indicators
        .eql(expectedIncome)
    }
  }

  async verifyFeeDecision({
    startDate,
    endDate,
    status
  }: {
    startDate: string
    endDate: string
    status: string
  }) {
    if (
      (await this.feeDecisionsCollapsible.getAttribute('data-status')) ===
      'closed'
    ) {
      await t.click(this.feeDecisionsCollapsible)
    }

    const decision = this.feeDecisionTableRow.nth(1)
    await t
      .expect(decision.textContent)
      .contains(`Maksupäätös ${startDate} - ${endDate}`)
    await t.expect(decision.textContent).contains(status)
  }

  async createRetroactiveFeeDecisions(date: string) {
    if (
      (await this.feeDecisionsCollapsible.getAttribute('data-status')) ===
      'closed'
    ) {
      await t.click(this.feeDecisionsCollapsible)
    }

    await t.click(this.feeDecisionsCollapsible.find('button'))
    const modal = Selector('[data-qa="form-modal"]')
    await t.typeText(modal.find('input'), date, { replace: true, paste: true })
    await t.click(modal.find('[data-qa="modal-okBtn"]'))
  }

  private async addPerson({
    collapsible,
    searchWord,
    startDate
  }: {
    collapsible: Selector
    searchWord: string
    startDate: string
  }) {
    if ((await collapsible.getAttribute('data-status')) === 'closed') {
      await t.click(collapsible)
    }
    await t.click(collapsible.find('button'))
    const modal = Selector('[data-qa="form-modal"]')
    await selectFirstOption(modal, searchWord)
    await t.typeText(
      modal.find('.react-datepicker__input-container').nth(0).find('input'),
      startDate,
      { replace: true }
    )
    await t.click(modal.find('.react-datepicker__day--selected'))
    await t.click(modal.find('[data-qa="modal-okBtn"]'))
  }
}
