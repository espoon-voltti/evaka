// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare global {
  interface Window {
    iosSmokeStalePage?: boolean
  }
}

/**
 * Safari may return from `browser.url()` before the previous page has been
 * replaced, so commands would run against the old document. The marker set
 * here disappears only when the new document has loaded.
 */
export async function navigate(url: string) {
  await browser.execute(() => {
    window.iosSmokeStalePage = true
  })
  await browser.url(url)
  await browser.waitUntil(
    () =>
      browser.execute(
        (target: string) =>
          window.iosSmokeStalePage !== true &&
          document.readyState === 'complete' &&
          location.href.startsWith(target),
        url
      ),
    { timeoutMsg: `Page did not load: ${url}` }
  )
}
