// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'

export function formatDateTimeOnly(date: Date | null | undefined): string {
  return date ? format(date, 'HH:mm') : ''
}
