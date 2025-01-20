// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getSentEmails } from '../generated/api-clients'

export async function getVerificationCodeFromEmail(): Promise<string | null> {
  // assumption: only one email has been sent during the test, and it's the verification e-mail
  const emails = await getSentEmails()
  if (emails.length === 0) return null
  expect(emails.length).toBe(1)
  const email = emails[0]

  const verificationCodeRegex = /[0-9]{6}/
  const matches = verificationCodeRegex.exec(email.content.text)
  return matches ? matches[0] : null
}
