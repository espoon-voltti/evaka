// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const keycloakBaseUrl = 'http://localhost:8080'

export class KeycloakRealmClient {
  readonly #baseUrl: string
  readonly #headers: object

  constructor(
    private realm: string,
    private accessToken: string
  ) {
    this.#baseUrl = `${keycloakBaseUrl}/auth/admin/realms/${encodeURIComponent(this.realm)}`
    this.#headers = {
      Authorization: `Bearer ${this.accessToken}`
    }
  }

  static async create({
    realm
  }: {
    realm: string
  }): Promise<KeycloakRealmClient> {
    const accessToken = await acquireAccessToken()
    return new KeycloakRealmClient(realm, accessToken)
  }

  static async createCitizenClient(): Promise<KeycloakRealmClient> {
    return KeycloakRealmClient.create({ realm: 'evaka-customer' })
  }

  async getUsers(): Promise<User[]> {
    const res = await fetch(`${this.#baseUrl}/users`, {
      method: 'GET',
      headers: {
        ...this.#headers,
        Accept: 'application/json'
      }
    })
    if (!res.ok) {
      throw new Error(
        `Failed to get users: ${res.status} ${res.statusText}: ${await res.text()}`
      )
    }
    // eslint-disable-next-line @typescript-eslint/no-unsafe-return
    return res.json()
  }

  async deleteUser(userId: string): Promise<void> {
    const res = await fetch(
      `${this.#baseUrl}/users/${encodeURIComponent(userId)}`,
      {
        method: 'DELETE',
        headers: {
          ...this.#headers,
          Accept: 'application/json'
        }
      }
    )
    if (!res.ok) {
      throw new Error(
        `Failed to delete user: ${res.status} ${res.statusText}: ${await res.text()}`
      )
    }
  }
  async createUser(user: {
    username: string
    enabled: boolean
    firstName: string
    lastName: string
    email: string
    socialSecurityNumber: string
    password: string
  }): Promise<void> {
    const res = await fetch(`${this.#baseUrl}/users`, {
      method: 'POST',
      headers: {
        ...this.#headers,
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: user.username,
        enabled: user.enabled,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        attributes: {
          suomi_sn: user.lastName,
          suomi_givenName: user.firstName,
          emailConfirm: user.email,
          suomi_nationalIdentificationNumber: user.socialSecurityNumber
        }
      })
    })
    if (!res.ok) {
      throw new Error(
        `Failed to create user: ${res.status} ${res.statusText}: ${await res.text()}`
      )
    }

    const users = await this.getUsers()
    const userId = users.find(({ username }) => username === user.username)?.id
    if (!userId) {
      throw new Error('User not found')
    }
    await this.resetUserPassword({
      userId,
      password: user.password
    })
  }

  async resetUserPassword({
    userId,
    password
  }: {
    userId: string
    password: string
  }): Promise<void> {
    const res = await fetch(
      `${this.#baseUrl}/users/${encodeURIComponent(userId)}/reset-password`,
      {
        method: 'PUT',
        headers: {
          ...this.#headers,
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          type: 'password',
          value: password
        })
      }
    )
    if (!res.ok) {
      throw new Error(
        `Failed to reset user password: ${res.status} ${res.statusText}: ${await res.text()}`
      )
    }
  }

  async deleteAllUsers() {
    const users = await this.getUsers()
    for (const user of users) {
      await this.deleteUser(user.id)
    }
  }
}

export async function acquireAccessToken(): Promise<string> {
  const res = await fetch(
    `${keycloakBaseUrl}/auth/realms/master/protocol/openid-connect/token`,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        username: 'admin',
        password: 'admin',
        grant_type: 'password',
        client_id: 'admin-cli'
      })
    }
  )
  if (!res.ok) {
    throw new Error(
      `Failed to acquire access token: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const body = await res.json()
  // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-return
  return body.access_token
}

export interface User {
  id: string
  username: string
  // ...other fields
}
