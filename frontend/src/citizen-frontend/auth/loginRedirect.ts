// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// `fromNotification=1` is a transient marker added to every URL the citizen
// service worker opens from a push notification click. When the SPA routes
// through an auth wall, RequireAuth uses this helper to (a) strip the marker
// from the return URL so a successful post-login landing doesn't re-mark
// itself, and (b) signal the reason to the login page so it can show a
// context-specific banner.
export function buildLoginRedirectPath(
  returnPath: string,
  returnSearch: string
): string {
  const params = new URLSearchParams(returnSearch)
  const fromNotification = params.get('fromNotification') === '1'
  if (fromNotification) {
    params.delete('fromNotification')
  }
  const remainingSearch = params.toString()
  const returnUrl = remainingSearch
    ? `${returnPath}?${remainingSearch}`
    : returnPath
  const base = `/login?next=${encodeURIComponent(returnUrl)}`
  return fromNotification ? `${base}&reason=session-expired-open-thread` : base
}
