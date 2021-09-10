// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { otherIncome } from 'lib-common/api-types/incomeStatement'

export const attachmentType = [
  ...otherIncome,
  'ALIMONY_PAYOUT',
  'PAYSLIP',
  'STARTUP_GRANT',
  'SALARY',
  'ACCOUNTANT_REPORT',
  'PROFIT_AND_LOSS_STATEMENT',
  'PROOF_OF_STUDIES'
] as const

export type AttachmentType = typeof attachmentType[number]
