// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const mailHogBaseUrl = 'http://localhost:8025'

export async function deleteCapturedEmails(): Promise<void> {
  // https://github.com/mailhog/MailHog/blob/master/docs/APIv1.md#delete-apiv1messages
  const res = await fetch(`${mailHogBaseUrl}/api/v1/messages`, {
    method: 'DELETE',
    headers: {
      Accept: 'application/json'
    }
  })
  if (!res.ok) {
    throw new Error(
      `Failed to delete emails: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
}

export async function getCapturedEmails(): Promise<Messages> {
  // This swagger does *NOT* match the actual API:
  // https://github.com/mailhog/MailHog/blob/master/docs/APIv2/swagger-2.0.yaml
  const res = await fetch(`${mailHogBaseUrl}/api/v2/messages`, {
    method: 'GET',
    headers: {
      Accept: 'application/json'
    }
  })
  if (!res.ok) {
    throw new Error(
      `Failed to get emails: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
  // eslint-disable-next-line @typescript-eslint/no-unsafe-return
  return await res.json()
}

export interface Messages {
  total: number
  start: number
  count: number
  items: Message[]
}

export interface Message {
  ID: string
  From: Path
  To: Path[]
  Created: string // date-time
  Raw: {
    From: string
    To: string[]
    // ...other fields
  }
  MIME: {
    Parts: {
      Headers: Record<string, string[]>
      Body: string
      Size: number
      MIME: unknown
    }[]
  }
  Content: {
    Headers: unknown
    Body: unknown
    Size: number
    MIME: unknown
  }
}

export interface Path {
  Relays: string[]
  Mailbox: string
  Domain: string
  Params: string
}
