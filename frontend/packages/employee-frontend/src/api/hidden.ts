// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './client'

export async function triggerFamilyBatch() {
  await client.post('/family/batch-schedule')
}

export async function triggerVtjBatch() {
  await client.post('/person/batch-refresh')
}
