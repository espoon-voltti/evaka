// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  AccountType,
  CitizenMessageThread
} from 'lib-common/generated/api-types/messaging'

export const isPrimaryRecipient = ({ type }: { type: AccountType }) =>
  type !== 'CITIZEN'

export const isRedactedThread = (
  thread: CitizenMessageThread
): thread is CitizenMessageThread.Redacted => thread.type === 'Redacted'

export const isRegularThread = (
  thread: CitizenMessageThread
): thread is CitizenMessageThread.Regular => thread.type === 'Regular'
