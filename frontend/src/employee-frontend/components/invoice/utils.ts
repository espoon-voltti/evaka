// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import YearMonth from 'lib-common/year-month'

import { Translations } from '../../state/i18n'

export function formatInvoicePeriod(
  invoice: InvoiceDetailed,
  i18n: Translations
): string {
  return (
    i18n.invoice.form.details.invoice +
    ' ' +
    YearMonth.ofDate(invoice.periodStart).format() +
    (invoice.revisionNumber > 0
      ? ` (${i18n.invoice.form.details.revision(invoice.revisionNumber)})`
      : '')
  )
}
