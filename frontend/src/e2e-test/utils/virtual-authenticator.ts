// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page } from './page'

/**
 * Attaches a CDP WebAuthn virtual authenticator to the page: a platform
 * authenticator with discoverable credentials and user verification, so
 * passkey ceremonies complete without any real UI.
 */
export async function addVirtualAuthenticator(page: Page): Promise<void> {
  const cdp = await page.page.context().newCDPSession(page.page)
  await cdp.send('WebAuthn.enable')
  await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      transport: 'internal',
      hasResidentKey: true,
      hasUserVerification: true,
      isUserVerified: true,
      automaticPresenceSimulation: true
    }
  })
}
