// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const dismissalDays = 90

export type Suggestion = 'install' | 'push'

const cookieName = (suggestion: Suggestion, userId: string) =>
  `evaka-pwa-${suggestion}-suggestion-dismissal-${userId}`

export function isSuggestionDismissed(
  suggestion: Suggestion,
  userId: string
): boolean {
  return document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith(`${cookieName(suggestion, userId)}=`))
}

export function dismissSuggestion(
  suggestion: Suggestion,
  userId: string
): void {
  const maxAge = dismissalDays * 24 * 60 * 60
  document.cookie = `${cookieName(suggestion, userId)}=true; max-age=${maxAge}; path=/; SameSite=Strict`
}
