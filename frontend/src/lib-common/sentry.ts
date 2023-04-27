// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'

function isGoogleTranslateError(event: Sentry.Event): boolean {
  if (!event.breadcrumbs) return false
  return event.breadcrumbs.some((breadcrumb) => {
    const { category, data } = breadcrumb
    const method: unknown = data?.method
    const url: unknown = data?.url
    return (
      category === 'xhr' &&
      method === 'POST' &&
      typeof url === 'string' &&
      url.includes('translate.googleapis.com')
    )
  })
}

export function sentryEventFilter(event: Sentry.Event): Sentry.Event | null {
  if (isGoogleTranslateError(event)) return null
  return event
}
