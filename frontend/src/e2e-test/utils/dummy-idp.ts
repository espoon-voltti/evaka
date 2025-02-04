// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const baseUrl = 'http://localhost:9090'

export async function deleteAllDummyIdpUsers(): Promise<void> {
  const res = await fetch(`${baseUrl}/idp/users/clear`, {
    method: 'POST'
  })
  if (!res.ok) {
    throw new Error(
      `Failed to delete dummy-idp users: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
}

export async function upsertDummyIdpUser(user: {
  ssn: string
  commonName: string
  givenName: string
  surname: string
  comment?: string
}): Promise<void> {
  const res = await fetch(`${baseUrl}/idp/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify([user])
  })
  if (!res.ok) {
    throw new Error(
      `Failed to create dummy-idp user: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
}
