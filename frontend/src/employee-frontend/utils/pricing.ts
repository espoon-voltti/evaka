// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { InvoiceRowDetailed } from 'lib-common/generated/api-types/invoicing'

export function totalPrice(rows: InvoiceRowDetailed[]) {
  return rows.reduce((sum, row) => sum + row.amount * row.unitPrice, 0)
}
