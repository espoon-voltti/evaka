// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const dummySuomifiBaseUrl = 'http://localhost:9000'

export async function deleteAllSuomiFiUsers(): Promise<void> {
  const res = await fetch(`${dummySuomifiBaseUrl}/idp/users/clear`, {
    method: 'POST'
  })
  if (!res.ok) {
    throw new Error(
      `Failed to delete suomi.fi users: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
}

export async function createSuomiFiUser(user: {
  ssn: string
  commonName: string
  givenName: string
  surname: string
}): Promise<void> {
  const res = await fetch(`${dummySuomifiBaseUrl}/idp/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(user)
  })
  if (!res.ok) {
    throw new Error(
      `Failed to create suomi.fi user: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
}
