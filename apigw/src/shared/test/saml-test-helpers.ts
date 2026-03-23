// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { SignedXml } from 'xml-crypto'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const IDP_PVK = fs
  .readFileSync(
    path.resolve(__dirname, '../../../config/test-cert/slo-test-idp-key.pem'),
    'utf8'
  )
  .toString()

export const SP_CALLBACK_URL =
  'https://saml-sp.qwerty.local/api/citizen/auth/sfi/logout/callback'
export const SP_DOMAIN = new URL(SP_CALLBACK_URL).origin
export const IDP_ENTRY_POINT_URL = 'https://identity-provider.asdf.local/idp'

export const SP_LOGIN_CALLBACK_ENDPOINT = '/api/citizen/auth/sfi/login/callback'
export const SP_LOGIN_CALLBACK_URL = `${SP_DOMAIN}${SP_LOGIN_CALLBACK_ENDPOINT}`
export const SP_LOGOUT_CALLBACK_ENDPOINT =
  '/api/citizen/auth/sfi/logout/callback'
export const SP_LOGOUT_CALLBACK_URL = `${SP_DOMAIN}${SP_LOGOUT_CALLBACK_ENDPOINT}`

export const SP_ISSUER = 'evaka-local'
export const IDP_ISSUER = 'evaka-slo-test'

export function buildLoginResponse(
  nameId: string,
  sessionIndex: string,
  inResponseTo: string
) {
  const notBefore = '1980-01-01T01:00:00Z'
  const issueInstant = '1980-01-01T01:01:00Z'
  const notOnOrAfter = '4980-01-01T01:01:00Z'

  const assertion = `<saml:Assertion
      xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      ID="_bbbbbbbbbbbbbbbbbbbbbbbb"
      Version="2.0" IssueInstant="${issueInstant}">
      <saml:Issuer>${IDP_ISSUER}</saml:Issuer>
      <saml:Subject>
          <saml:NameID
              SPNameQualifier="${SP_ISSUER}"
              Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">${nameId}</saml:NameID>
          <saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
              <saml:SubjectConfirmationData
                  NotOnOrAfter="${notOnOrAfter}"
                  Recipient="${SP_LOGIN_CALLBACK_URL}"
                  InResponseTo="${inResponseTo}"/>
          </saml:SubjectConfirmation>
      </saml:Subject>
      <saml:Conditions
          NotBefore="${notBefore}"
          NotOnOrAfter="${notOnOrAfter}">
          <saml:AudienceRestriction>
              <saml:Audience>${SP_ISSUER}</saml:Audience>
          </saml:AudienceRestriction>
      </saml:Conditions>
      <saml:AttributeStatement>
        <saml:Attribute Name="urn:oid:1.2.246.21">
          <saml:AttributeValue>010101-999X</saml:AttributeValue>
        </saml:Attribute>
        <saml:Attribute Name="urn:oid:2.5.4.42">
          <saml:AttributeValue>Etunimi</saml:AttributeValue>
        </saml:Attribute>
        <saml:Attribute Name="urn:oid:2.5.4.4">
          <saml:AttributeValue>Sukunimi</saml:AttributeValue>
        </saml:Attribute>
      </saml:AttributeStatement>
      <saml:AuthnStatement
          AuthnInstant="${issueInstant}"
          SessionNotOnOrAfter="${notOnOrAfter}"
          SessionIndex="${sessionIndex}">
          <saml:AuthnContext>
              <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef>
          </saml:AuthnContext>
      </saml:AuthnStatement>
  </saml:Assertion>`

  const loginResponse = `<samlp:Response
  xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
  xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
  ID="_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  Version="2.0"
  IssueInstant="${issueInstant}"
  Destination="${SP_LOGIN_CALLBACK_URL}"
  InResponseTo="${inResponseTo}">
  <saml:Issuer>${IDP_ISSUER}</saml:Issuer>
  <samlp:Status>
      <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
  </samlp:Status>
  ${signXml(assertion)}
</samlp:Response>`
  return Buffer.from(signXml(loginResponse)).toString('base64')
}

export function buildIdPInitiatedLogoutRequest(
  nameId: string,
  sessionIndex: string
) {
  const idpInitiatedLogoutRequest = `<samlp:LogoutRequest
  xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
  xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
  ID="_adcdabcd"
  Version="2.0"
  IssueInstant="2020-01-01T01:01:00Z"
  Destination="${SP_LOGOUT_CALLBACK_URL}">
  <saml:Issuer>${IDP_ISSUER}</saml:Issuer>
  <saml:NameID
      SPNameQualifier="${SP_ISSUER}"
      Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">${nameId}</saml:NameID>
  <samlp:SessionIndex>${sessionIndex}</samlp:SessionIndex>
</samlp:LogoutRequest>`
  return Buffer.from(signXml(idpInitiatedLogoutRequest)).toString('base64')
}

function signXml(xml: string) {
  const sig = new SignedXml()
  sig.addReference({
    xpath: '/*',
    transforms: [
      'http://www.w3.org/2000/09/xmldsig#enveloped-signature',
      'http://www.w3.org/2001/10/xml-exc-c14n#'
    ],
    digestAlgorithm: 'http://www.w3.org/2001/04/xmlenc#sha256'
  })
  sig.signatureAlgorithm = 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256'
  sig.canonicalizationAlgorithm = 'http://www.w3.org/2001/10/xml-exc-c14n#'
  sig.privateKey = IDP_PVK
  sig.computeSignature(xml)
  return sig.getSignedXml()
}
