// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from '../../api/client'

export interface BulletinReviewResult {
  ok: boolean
  feedback: string
}

export async function reviewBulletin(
  title: string,
  content: string
): Promise<BulletinReviewResult> {
  const response = await client.post<BulletinReviewResult>(
    '/employee/bulletin-ai-review',
    { title, content }
  )
  return response.data
}
