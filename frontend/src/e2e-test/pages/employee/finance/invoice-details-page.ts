// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { InvoiceReplacementReason } from 'lib-common/generated/api-types/invoicing'

import { Page, Element, TextInput, Select } from '../../../utils/page'

export class InvoiceDetailsPage {
  headOfFamilySection: InvoiceHeadOfFamilySection
  detailsSection: InvoiceDetailsSection

  totalPrice: Element
  previousTotalPrice: Element

  replacementDraftForm: InvoiceReplacementDraftSection

  // view
  replacementInfo: InvoiceReplacementInfoSection

  constructor(private page: Page) {
    this.headOfFamilySection = new InvoiceHeadOfFamilySection(
      page.findByDataQa('head-of-family')
    )
    this.detailsSection = new InvoiceDetailsSection(
      page.findByDataQa('invoice-details')
    )
    this.totalPrice = this.page.findByDataQa('total-sum').findByDataQa('price')
    this.previousTotalPrice = this.page
      .findByDataQa('total-sum')
      .findByDataQa('previous-price')

    this.replacementDraftForm = new InvoiceReplacementDraftSection(
      page.findByDataQa('replacement-draft-form')
    )
    this.replacementInfo = new InvoiceReplacementInfoSection(
      page.findByDataQa('replacement-info')
    )
  }

  nthChild(index: number): InvoiceChildSection {
    return new InvoiceChildSection(
      this.page
        .findByDataQa('invoice-rows')
        .findAll('[data-qa^="child-"]')
        .nth(index)
    )
  }

  child(childId: string): InvoiceChildSection {
    return new InvoiceChildSection(
      this.page.findByDataQa('invoice-rows').findByDataQa(`child-${childId}`)
    )
  }
}

export class InvoiceHeadOfFamilySection extends Element {
  headOfFamilyName = this.findByDataQa('head-of-family-name')
  headOfFamilySsn = this.findByDataQa('head-of-family-ssn')
  codebtorName = this.findByDataQa('codebtor-name')
  codebtorSsn = this.findByDataQa('codebtor-ssn')
}

export class InvoiceDetailsSection extends Element {
  status = this.findByDataQa('status')
  period = this.findByDataQa('period')
  number = this.findByDataQa('number')
  dueDate = this.findByDataQa('due-date')
  account = this.findByDataQa('account')
  agreementType = this.findByDataQa('agreement-type')
  relatedFeeDecisions = this.findByDataQa('related-fee-decisions')
  replacedInvoice = this.findByDataQa('replaced-invoice')
}

export class InvoiceChildSection extends Element {
  childName = this.findByDataQa('child-name')
  childSsn = this.findByDataQa('child-ssn')
  rows = this.findAllByDataQa('invoice-row')
  totalPrice = this.findByDataQa('child-sum').findByDataQa('price')
  previousTotalPrice =
    this.findByDataQa('child-sum').findByDataQa('previous-price')

  row(index: number) {
    return new InvoiceRow(this.rows.nth(index))
  }
}

export class InvoiceRow extends Element {
  product = this.findByDataQa('product')
  description = this.findByDataQa('description')
  unit = this.findByDataQa('unit')
  period = this.findByDataQa('period')
  amount = this.findByDataQa('amount')
  unitPrice = this.findByDataQa('unit-price')
  totalPrice = this.findByDataQa('total-price')
}

export class InvoiceReplacementDraftSection extends Element {
  reason = new Select(this.findByDataQa('replacement-reason'))
  notes = new TextInput(this.findByDataQa('replacement-notes'))
  markSentButton = this.findByDataQa('mark-sent')

  async selectReason(value: InvoiceReplacementReason) {
    await this.reason.selectOption(value)
  }
}

export class InvoiceReplacementInfoSection extends Element {
  reason = this.findByDataQa('replacement-reason')
  notes = this.findByDataQa('replacement-notes')
  sentAt = this.findByDataQa('sent-at')
  sentBy = this.findByDataQa('sent-by')
}
