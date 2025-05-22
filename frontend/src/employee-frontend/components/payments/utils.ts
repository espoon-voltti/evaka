// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PaymentStatus } from 'lib-common/generated/api-types/invoicing'

export const selectablePaymentStatuses: PaymentStatus[] = ['DRAFT', 'CONFIRMED']
