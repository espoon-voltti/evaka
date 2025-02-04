// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  defaultPersons,
  sfiSamlAttrs,
  sfiSamlAttrUrns,
  toSfiSamlAttrs,
  vtjPersonSchema
} from './model'
import express from 'express'
import { z } from 'zod'
import samlp, { IdPOptions } from 'samlp'
import * as crypto from 'node:crypto'
import * as fs from 'node:fs'
import { config } from './config'
import { SessionParticipants, SimpleProfileMapper } from './saml'
import { html, Html } from './html'

let vtjPersons = defaultPersons

export const clearUsers: express.RequestHandler = (_, res) => {
  vtjPersons = []
  res.sendStatus(200)
}

export const upsertUser: express.RequestHandler = (req, res) => {
  const persons = z.array(vtjPersonSchema).parse(req.body)
  vtjPersons = [
    ...vtjPersons.filter((p) => persons.every(({ ssn }) => p.ssn !== ssn)),
    ...persons
  ]
  res.sendStatus(200)
}

const idpPublicCert = fs.readFileSync(config.IDP_PUBLIC_CERT_PATH)
const idpPrivateKey = fs.readFileSync(config.IDP_PRIVATE_KEY_PATH)
const spPublicCert = fs.readFileSync(config.SP_PUBLIC_CERT_PATH)
// samlp library needs the public key separately for some reason, but it can be derived from the certificate
const spPublicKey = crypto.createPublicKey(spPublicCert).export({
  format: 'pem',
  type: 'pkcs1'
})

// responds with an HTML form page that captures SAML state from query parameters using hidden inputs
const renderSamlFormPage = (
  req: express.Request,
  res: express.Response,
  params: { uri: string; bodyHtml: Html }
) => {
  // Preserve SAML state in hidden input fields
  const samlStateInputs = ['SAMLRequest', 'RelayState', 'SigAlg']
    .map((key) => [key, req.query[key] ?? ''] as const)
    .map(
      ([key, value]) =>
        html`<input type="hidden" name="${key}" value="${value.toString()}" />`
    )

  res.contentType('text/html').send(`
<!DOCTYPE html>
<html lang="fi">
<head>
  <meta charset="UTF-8">
  <link rel="icon" href="data:image/png;base64,iVBORw0KGgo="> <!-- this disables favicon.ico requests -->
  <style>
  html {
    font-size: 20px;
  }
  button {
    font-size: 150%;
  }
  pre {
    font-size: 70%;
    margin: 0;
  }
  small {
    color: #777;
    font-style: italic;
  }
  table {
    border-collapse: collapse;
  }
  td {
    padding: 0.5em;
    border: 1px solid #ccc;
    vertical-align: top;
  }
  </style>
  <title>dummy-idp</title>
</head>
<body>
  <h1>Devausympäristön Suomi.fi-kirjautuminen</h1>
  <form action="${encodeURI(params.uri)}" method="get">
${samlStateInputs.join('\n')}
${params.bodyHtml}
  </form>
</body>
</html>
`)
}

// Responds with a confirmation page that lists SAML attributes for the current user.
// This is a bit similar to how real Suomi.fi works after you finish the actual authentication
const confirmHandler: express.RequestHandler = (req, res) => {
  const user = req.session.user
  if (!user) throw new Error('No user found')

  const attrValues = toSfiSamlAttrs(user.person)
  const attrs = sfiSamlAttrUrns.map((urn) => {
    const value = attrValues[urn]
    const friendlyName = sfiSamlAttrs[urn]
    return html`<tr>
      <td>
        <strong>${friendlyName}</strong><br />
        <pre>${urn}</pre>
      </td>
      <td>${value}</td>
    </tr>`
  })

  return renderSamlFormPage(req, res, {
    uri: `${req.baseUrl}/idp/sso-login-finish`,
    bodyHtml: html`
      <button type="submit">Jatka</button>
      <h2>Välitettävät tiedot:</h2>
      <table>
        ${attrs}
      </table>
    `
  })
}

export const samlSingleSignOnRoute: express.RequestHandler = (
  req,
  res,
  next
) => {
  console.log('SSO endpoint called')
  if (req.session.user) {
    confirmHandler(req, res, next)
  } else {
    const defaultSsn = '070644-937X'
    const persons = [...vtjPersons] // copy data, because we're doing a mutable sort
    persons.sort((a, b) => a.ssn.localeCompare(b.ssn))
    const inputs = persons
      .map(({ ssn, givenName, surname, comment }) => {
        if (!ssn) return ''
        const checked = ssn === defaultSsn ? 'checked' : ''
        const commentHtml = comment ? html`<small>(${comment})</small>` : ''
        return html`<div>
          <input
            type="radio"
            id="${ssn}"
            name="ssn"
            value="${ssn}"
            required
            ${checked}
          />
          <label for="${ssn}">
            <span style="font-family: monospace; user-select: all">${ssn}</span
            >: ${givenName} ${surname} ${commentHtml}
          </label>
        </div>`
      })
      .filter((line) => !!line)

    return renderSamlFormPage(req, res, {
      uri: `${req.baseUrl}/idp/sso-login-confirm`,
      bodyHtml: html`
        <div style="margin-bottom: 20px">
          <button type="submit">Kirjaudu</button>
        </div>
        ${inputs}
      `
    })
  }
}

export const samlSingleSignOnConfirmRoute: express.RequestHandler = (
  req,
  res,
  next
) => {
  const ssn = req.query.ssn
  const person = vtjPersons.find((p) => p.ssn === ssn)
  if (!person) throw new Error(`No person with ssn ${ssn}`)
  req.session.user = {
    nameId: crypto.randomUUID(),
    person
  }
  confirmHandler(req, res, next)
}

// @types/samlp is not fully correct, so fix things here
type LoginIdpOptions = IdPOptions & {
  sessionIndex?: string
}
export const samlSingleSignOnFinishRoute: express.RequestHandler = (
  req,
  res,
  next
) => {
  console.log('SSO finish endpoint called')
  samlp.auth({
    cert: idpPublicCert,
    key: idpPrivateKey,
    signatureAlgorithm: 'rsa-sha256',
    digestAlgorithm: 'sha256',
    issuer: 'dummy-idp',
    signResponse: true,
    encryptionCert: spPublicCert,
    encryptionAlgorithm: 'http://www.w3.org/2009/xmlenc11#aes128-gcm',
    encryptionPublicKey: spPublicKey,
    keyEncryptionAlgorighm: 'http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p',
    sessionIndex: '1',
    getUserFromRequest: (req) => req.session.user,
    profileMapper: (pu: any) => new SimpleProfileMapper(pu),
    getPostURL: (audience, authnRequestDom, req, callback) => {
      if (audience === config.SP_ENTITY_ID) {
        return callback(null, config.SP_SSO_CALLBACK_URL)
      } else {
        return callback(new Error(`Unexpected SAML audience ${audience}`), '')
      }
    }
  } satisfies LoginIdpOptions as any)(req, res, next)
}

// @types/samlp is not fully correct, so fix things here
type LogoutIdpOptions = Pick<IdPOptions, 'cert' | 'key' | 'issuer'> & {
  deflate?: boolean
  sessionParticipants: SessionParticipants
  clearIdPSession: (cb: (err: any) => void) => void
}
export const samlSingleLogoutRoute: express.RequestHandler = (
  req,
  res,
  next
) => {
  console.log('SLO endpoint called')
  samlp.logout({
    cert: idpPublicCert,
    key: idpPrivateKey,
    issuer: 'dummy-idp',
    deflate: true,
    sessionParticipants: new SessionParticipants(
      req.session.user
        ? [
            {
              serviceProviderId: config.SP_ENTITY_ID,
              sessionIndex: '1',
              nameId: req.session.user.nameId,
              nameIdFormat:
                'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
              serviceProviderLogoutURL: config.SP_SLO_CALLBACK_URL,
              cert: spPublicCert,
              binding: 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
            }
          ]
        : []
    ),
    clearIdPSession: (cb) => req.session.destroy(cb)
  } satisfies LogoutIdpOptions as any)(req, res, next)
}
