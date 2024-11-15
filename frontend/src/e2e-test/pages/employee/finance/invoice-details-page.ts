// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../../utils/page'

export class InvoiceDetailsPage {
  headOfFamilySection: InvoiceHeadOfFamilySection
  detailsSection: InvoiceDetailsSection

  totalPrice: Element
  previousTotalPrice: Element

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
