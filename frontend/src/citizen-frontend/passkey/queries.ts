// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { client } from '../api-client'

export interface CitizenPasskeyCredentialSummary {
  credentialId: string
  label: string
  deviceHint: string | null
  createdAt: string
  lastUsedAt: string | null
}

const q = new Queries()

async function getPasskeyCredentials(): Promise<
  CitizenPasskeyCredentialSummary[]
> {
  const { data } = await client.get<CitizenPasskeyCredentialSummary[]>(
    '/citizen/passkey/credentials'
  )
  return data
}

async function renamePasskeyCredential(arg: {
  credentialId: string
  label: string
}): Promise<void> {
  await client.patch(`/citizen/passkey/credentials/${arg.credentialId}`, {
    label: arg.label
  })
}

async function revokePasskeyCredential(arg: {
  credentialId: string
}): Promise<void> {
  await client.delete(`/citizen/passkey/credentials/${arg.credentialId}`)
}

export const passkeysQuery = q.query(getPasskeyCredentials)

export const renamePasskeyMutation = q.mutation(renamePasskeyCredential, [
  passkeysQuery
])

export const revokePasskeyMutation = q.mutation(revokePasskeyCredential, [
  passkeysQuery
])
