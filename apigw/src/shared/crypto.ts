// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'

export const createSha256Hash = (str: string): string =>
  createHash('sha256').update(str).digest('hex')
