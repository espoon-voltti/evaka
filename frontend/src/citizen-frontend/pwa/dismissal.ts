// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const dismissalDays = 90

const cookieName = (userId: string) =>
  `evaka-pwa-install-suggestion-dismissal-${userId}`

export function isInstallSuggestionDismissed(userId: string): boolean {
  return document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith(`${cookieName(userId)}=`))
}

export function dismissInstallSuggestion(userId: string): void {
  const maxAge = dismissalDays * 24 * 60 * 60
  document.cookie = `${cookieName(userId)}=true; max-age=${maxAge}; path=/; SameSite=Strict`
}
