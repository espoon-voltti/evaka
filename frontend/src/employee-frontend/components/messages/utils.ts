// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const sessionKeepalive = async () => {
  const response = await fetch('/api/internal/auth/status', {
    method: 'GET',
    credentials: 'include'
  })
  if (!response.ok) {
    // could be e.g. temporarily offline
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  try {
    const data = (await response.json()) as {
      loggedIn?: boolean
    }
    return data.loggedIn === true
  } catch (error) {
    throw new Error('Failed to parse JSON response')
  }
}
