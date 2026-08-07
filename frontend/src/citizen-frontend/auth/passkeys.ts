// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { featureFlags } from 'lib-customizations/citizen'

import { client } from '../api-client'
import { startPasskeyRegistration } from '../generated/api-clients/pis'

import { passkeyProviderName } from './passkey-providers'

export function passkeysSupported(): boolean {
  if (!featureFlags.passkeys) return false
  return (
    typeof window.PublicKeyCredential !== 'undefined' &&
    typeof window.PublicKeyCredential.parseCreationOptionsFromJSON ===
      'function'
  )
}

export type PasskeyLoginResult = 'success' | 'cancelled' | 'failure'

interface PasskeyLoginOptionsResponse {
  challengeKey: string
  options: { publicKey: PublicKeyCredentialRequestOptionsJSON }
}

export async function authPasskeyLogin(
  mediation?: 'conditional',
  signal?: AbortSignal
): Promise<PasskeyLoginResult> {
  let data: PasskeyLoginOptionsResponse
  try {
    data = (
      await client.post<PasskeyLoginOptionsResponse>(
        '/citizen/auth/passkey-login/options'
      )
    ).data
  } catch {
    return 'failure'
  }
  let credential: Credential | null
  try {
    credential = await navigator.credentials.get({
      publicKey: PublicKeyCredential.parseRequestOptionsFromJSON(
        data.options.publicKey
      ),
      mediation,
      signal
    })
  } catch {
    // the citizen cancelled the dialog, no matching passkey was found, or
    // the operation was aborted
    return 'cancelled'
  }
  if (!(credential instanceof PublicKeyCredential)) return 'failure'
  try {
    await client.post('/citizen/auth/passkey-login/finish', {
      challengeKey: data.challengeKey,
      credential: JSON.stringify(credential.toJSON())
    })
  } catch {
    return 'failure'
  }
  return 'success'
}

export type PasskeyCreationResult =
  | {
      status: 'success'
      /** JSON serialization of the browser's PublicKeyCredential */
      credential: string
      providerName: string | undefined
    }
  | { status: 'cancelled' }
  | { status: 'failure'; errorCode: string | undefined }

/**
 * Runs the browser part of the registration ceremony. The credential is not
 * saved yet: the caller finishes the registration with the citizen's chosen
 * name.
 */
export async function createPasskeyCredential(): Promise<PasskeyCreationResult> {
  let creationOptions: PublicKeyCredentialCreationOptions
  try {
    const options = await startPasskeyRegistration()
    const optionsJson = JSON.parse(options.credentialsCreate) as {
      publicKey: PublicKeyCredentialCreationOptionsJSON
    }
    creationOptions = PublicKeyCredential.parseCreationOptionsFromJSON(
      optionsJson.publicKey
    )
  } catch (e) {
    return { status: 'failure', errorCode: errorCodeOf(e) }
  }
  let credential: Credential | null
  try {
    credential = await navigator.credentials.create({
      publicKey: creationOptions
    })
  } catch {
    return { status: 'cancelled' }
  }
  if (
    !(credential instanceof PublicKeyCredential) ||
    !(credential.response instanceof AuthenticatorAttestationResponse)
  ) {
    return { status: 'failure', errorCode: undefined }
  }
  return {
    status: 'success',
    credential: JSON.stringify(credential.toJSON()),
    providerName: passkeyProviderName(credential.response)
  }
}

function errorCodeOf(e: unknown): string | undefined {
  if (typeof e === 'object' && e !== null && 'response' in e) {
    const response = (e as { response?: { data?: { errorCode?: string } } })
      .response
    return response?.data?.errorCode
  }
  return undefined
}
