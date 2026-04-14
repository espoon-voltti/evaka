// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'

import { client, createServiceRequestHeaders } from './service-client.ts'
import { systemUserHeader } from './auth/index.ts'

export interface StoredPasskeyCredential {
  credentialId: string
  publicKey: string
  signCounter: number
  transports: string[]
  label: string
  deviceHint: string | null
  createdAt: string
  lastUsedAt: string | null
}

export interface UpsertPasskeyCredentialPayload {
  personId: string
  credentialId: string
  publicKey: string
  signCounter: number
  transports: string[]
  label: string
  deviceHint: string | null
}

export async function listCitizenPasskeyCredentials(
  req: express.Request,
  personId: string
): Promise<StoredPasskeyCredential[]> {
  const { data } = await client.get<StoredPasskeyCredential[]>(
    `/system/citizen-passkey/credentials/${encodeURIComponent(personId)}`,
    { headers: createServiceRequestHeaders(req, systemUserHeader) }
  )
  return data
}

export async function upsertCitizenPasskeyCredential(
  req: express.Request,
  payload: UpsertPasskeyCredentialPayload
): Promise<void> {
  await client.post(
    `/system/citizen-passkey/credentials`,
    payload,
    { headers: createServiceRequestHeaders(req, systemUserHeader) }
  )
}

export async function touchCitizenPasskeyCredential(
  req: express.Request,
  personId: string,
  credentialId: string,
  signCounter: number
): Promise<void> {
  await client.post(
    `/system/citizen-passkey/credentials/${encodeURIComponent(personId)}/${encodeURIComponent(credentialId)}/touch`,
    { signCounter },
    { headers: createServiceRequestHeaders(req, systemUserHeader) }
  )
}
