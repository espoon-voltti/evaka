// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getSentEmails } from '../generated/api-clients'

export async function getVerificationCodeFromEmail(): Promise<string | null> {
  const emails = await getSentEmails()
  if (emails.length === 0) return null

  const verificationCodeRegex = /[0-9]{6}/
  const matches = emails
    .map((email) => verificationCodeRegex.exec(email.content.text))
    .filter((match) => match !== null)
  return matches.length > 0 ? matches[0][0] : null
}
