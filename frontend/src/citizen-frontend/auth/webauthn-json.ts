// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// The WebAuthn JSON serialization helpers only exist in Safari 18.4, Chrome 129
// and Firefox 119 onwards, while passkeys themselves work in far older browsers.
// These wrappers use the native helpers when present and convert by hand otherwise.

function base64UrlToBuffer(value: string): ArrayBuffer {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const binary = atob(padded)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

function bufferToBase64Url(value: ArrayBuffer): string {
  const bytes = new Uint8Array(value)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

// The JSON and non-JSON DOM types differ in enum width, so conversions need a cast.
// Extension inputs are passed through unconverted, as eVaka requests none.
const parseDescriptor = (
  descriptor: PublicKeyCredentialDescriptorJSON
): PublicKeyCredentialDescriptor =>
  ({
    ...descriptor,
    id: base64UrlToBuffer(descriptor.id)
  }) as PublicKeyCredentialDescriptor

export function parseCreationOptions(
  json: PublicKeyCredentialCreationOptionsJSON
): PublicKeyCredentialCreationOptions {
  if (typeof PublicKeyCredential.parseCreationOptionsFromJSON === 'function') {
    return PublicKeyCredential.parseCreationOptionsFromJSON(json)
  }
  return {
    ...json,
    challenge: base64UrlToBuffer(json.challenge),
    user: { ...json.user, id: base64UrlToBuffer(json.user.id) },
    excludeCredentials: json.excludeCredentials?.map(parseDescriptor)
  } as PublicKeyCredentialCreationOptions
}

export function parseRequestOptions(
  json: PublicKeyCredentialRequestOptionsJSON
): PublicKeyCredentialRequestOptions {
  if (typeof PublicKeyCredential.parseRequestOptionsFromJSON === 'function') {
    return PublicKeyCredential.parseRequestOptionsFromJSON(json)
  }
  return {
    ...json,
    challenge: base64UrlToBuffer(json.challenge),
    allowCredentials: json.allowCredentials?.map(parseDescriptor)
  } as PublicKeyCredentialRequestOptions
}

type CredentialJSON = RegistrationResponseJSON | AuthenticationResponseJSON

function attestationResponseToJSON(
  response: AuthenticatorAttestationResponse
): AuthenticatorAttestationResponseJSON {
  const publicKey =
    typeof response.getPublicKey === 'function' ? response.getPublicKey() : null
  return {
    clientDataJSON: bufferToBase64Url(response.clientDataJSON),
    attestationObject: bufferToBase64Url(response.attestationObject),
    transports:
      typeof response.getTransports === 'function'
        ? response.getTransports()
        : [],
    ...(publicKey ? { publicKey: bufferToBase64Url(publicKey) } : {}),
    ...(typeof response.getPublicKeyAlgorithm === 'function'
      ? { publicKeyAlgorithm: response.getPublicKeyAlgorithm() }
      : {}),
    ...(typeof response.getAuthenticatorData === 'function'
      ? {
          authenticatorData: bufferToBase64Url(response.getAuthenticatorData())
        }
      : {})
  } as AuthenticatorAttestationResponseJSON
}

function assertionResponseToJSON(
  response: AuthenticatorAssertionResponse
): AuthenticatorAssertionResponseJSON {
  return {
    clientDataJSON: bufferToBase64Url(response.clientDataJSON),
    authenticatorData: bufferToBase64Url(response.authenticatorData),
    signature: bufferToBase64Url(response.signature),
    userHandle: response.userHandle
      ? bufferToBase64Url(response.userHandle)
      : undefined
  }
}

export function credentialToJSON(
  credential: PublicKeyCredential
): CredentialJSON {
  const json =
    typeof credential.toJSON === 'function'
      ? credential.toJSON()
      : ({
          id: credential.id,
          rawId: bufferToBase64Url(credential.rawId),
          type: credential.type,
          ...(credential.authenticatorAttachment
            ? { authenticatorAttachment: credential.authenticatorAttachment }
            : {}),
          response:
            credential.response instanceof AuthenticatorAttestationResponse
              ? attestationResponseToJSON(credential.response)
              : assertionResponseToJSON(
                  credential.response as AuthenticatorAssertionResponse
                ),
          clientExtensionResults: credential.getClientExtensionResults()
        } as CredentialJSON)
  // Some password managers (e.g. 1Password) don't include the required `clientExtensionResults`
  // field in the credential, which causes the backend to reject the ceremony.
  if (!json.clientExtensionResults) {
    json.clientExtensionResults = {}
  }
  return json
}
