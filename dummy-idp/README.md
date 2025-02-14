<!--
SPDX-FileCopyrightText: 2017-2025 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# dummy-idp

dummy-idp is a minimalistic SAML identity provider (IDP) for local eVaka development and automated tests.
It uses a library called samlp for low-level SAML protocol functionality.

## Features

- minimalistic IDP functionality that roughly looks a bit like real Suomi.fi SAML authentication
- in-memory user (= "VTJ person") storage, and a simple REST API for clearing/upserting them
- SAML login flow with a user selection page + confirmation page
- SAML logout flow (service provider initiated only)
- in-memory IDP sessions

## SAML login flow

1. Service provider (= eVaka apigw) redirects the user's browser to `/idp/sso`.
2. If there's already an IDP session, goto step 4. If there's no IDP session, a user selection HTML form is rendered. 
3. The user selection form is submitted with a button press to `/idp/sso-login-confirm`
4. A confirmation page is rendered that lists the raw SAML attributes that will be sent in the SAML response to the service provider in the next step
5. The confirmation page form is submitted with a button press to `/idp/sso-login-finish`
6. A SAML response is created and the browser is redirected back to the service provider (= eVaka apigw)

Notes:

- we intentionally use SAML Redirect binding (= GET requests with query parameters) in everything
- the original SAML state (request, signature, RelayState) is passed using hidden form inputs on every page so they persist until the final `/idp/sso-login-finish` request where salmp needs them

## SAML logout flow

1. Service provider (= eVaka apigw) redirects the user's browser to `/idp/slo`.
2. The IDP session is destroyed
3. A SAML response is created and the browser is redirected back to the service provider (= eVaka apigw)

Notes:

- samlp seems to technically support IDP-initiated logout, but this is not implemented in dummy-idp

## Local development

Run `npm run dev` in the dummy-idp project directory.