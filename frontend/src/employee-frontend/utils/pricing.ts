// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { InvoiceRowDetailed } from '../types/invoicing'

export function totalPrice(rows: InvoiceRowDetailed[]) {
  return rows.reduce((sum, row) => sum + row.amount * row.unitPrice, 0)
}
