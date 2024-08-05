// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { type otherIncomes } from 'lib-common/generated/api-types/incomestatement'

export type AttachmentType =
  | (typeof otherIncomes)[number]
  | 'ALIMONY_PAYOUT'
  | 'PAYSLIP'
  | 'STARTUP_GRANT'
  | 'SALARY'
  | 'ACCOUNTANT_REPORT'
  | 'ACCOUNTANT_REPORT_LLC'
  | 'PROFIT_AND_LOSS_STATEMENT'
  | 'PROOF_OF_STUDIES'
  | 'CHILD_INCOME'
