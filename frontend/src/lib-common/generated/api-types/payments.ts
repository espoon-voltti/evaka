// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.payments.PaymentController.SendPaymentsRequest
*/
export interface SendPaymentsRequest {
  dueDate: LocalDate
  paymentDate: LocalDate
  paymentIds: UUID[]
}
