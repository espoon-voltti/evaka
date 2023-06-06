// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'

function isInlineScriptException(event: Sentry.Event): boolean {
  if (
    !event.exception ||
    !event.exception.values ||
    event.exception.values.length === 0
  ) {
    return false
  }

  const exception = event.exception.values[0]
  if (
    !exception.stacktrace ||
    !exception.stacktrace.frames ||
    exception.stacktrace.frames.length === 0
  ) {
    return false
  }

  const frame = exception.stacktrace.frames[0]
  return frame.filename === '<anonymous>'
}

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
  if (isInlineScriptException(event)) return null
  if (isGoogleTranslateError(event)) return null
  return event
}
